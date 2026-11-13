package com.chrisch.discordbot.command

import com.chrisch.discordbot.util.Utils.getOptionValue
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.discordjson.json.ApplicationCommandOptionData
import discord4j.discordjson.json.ApplicationCommandRequest
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientException
import reactor.kotlin.core.publisher.toMono
import reactor.netty.http.client.HttpClient
import java.net.*
import java.time.Duration


@Service
class UnshortenLink : CommandHandler<ChatInputInteractionEvent> {

    override val name: String = "unshortenlink"

    override val command: ApplicationCommandRequest
        get() = ApplicationCommandRequest.builder()
            .name(name)
            .description("Unshorten a link.")
            .addOption(
                ApplicationCommandOptionData.builder()
                    .name("url")
                    .description("Enter a string")
                    .type(ApplicationCommandOption.Type.STRING.value)
                    .required(true)
                    .build()
            ).build()

    private val httpClient = HttpClient.create()
        .followRedirect(true)
        .responseTimeout(Duration.ofSeconds(10))
        .doOnConnected { connection ->
            val address = (connection.channel().remoteAddress() as InetSocketAddress).address
            if (isNonPublic(address)) {
                throw IllegalStateException("Blocked non-public address $address")
            }
        }

    internal fun isNonPublic(address: InetAddress): Boolean {
        val bytes = address.address.map { it.toInt() and 0xFF }
        val isCarrierGradeNat = address is Inet4Address && bytes[0] == 100 && (bytes[1] and 0xC0) == 64 // 100.64.0.0/10
        val isUniqueLocal = address is Inet6Address && (bytes[0] and 0xFE) == 0xFC // fc00::/7
        return address.isLoopbackAddress || address.isSiteLocalAddress || address.isLinkLocalAddress ||
                address.isAnyLocalAddress || isCarrierGradeNat || isUniqueLocal
    }

    override suspend fun handle(event: ChatInputInteractionEvent) {
        event.deferReply().awaitSingleOrNull()

        val url = getOptionValue(event, "url").asString()
        val uri = runCatching { URI(if ("://" in url) url else "https://$url") }.getOrNull()

        if (uri == null || uri.scheme?.lowercase() !in setOf("http", "https") || uri.host == null) {
            event.editReply("Only http(s) links are allowed").awaitSingle()
            return
        }

        val history = followRedirects(uri)

        if (history == null) {
            event.editReply("Error accessing link").awaitSingle()
            return
        }

        if (history.isEmpty() || withoutScheme(history.last()) == withoutScheme(uri.toString())) {
            event.editReply("Link doesn't redirect").awaitSingle()
            return
        }

        event.editReply("Short link: <${url}>\nUnshortened link: <${history.last()}>").awaitSingle()
    }

    /** Returns the redirect targets in order, or null if the final response is not 200 OK. */
    internal suspend fun followRedirects(uri: URI): List<String>? {
        val history = mutableListOf<String>()
        val client = WebClient.builder().clientConnector(
            ReactorClientHttpConnector(
                httpClient.doOnRedirect { res, _ ->
                    val location = res.responseHeaders().get("Location")
                    history.add(
                        runCatching { URI(res.resourceUrl()).resolve(location).toString() }.getOrDefault(
                            location
                        )
                    )
                })
        ).build()

        val status = try {
            client.head().uri(uri).exchangeToMono { it.statusCode().toMono() }.awaitSingle()
        } catch (_: WebClientException) {
            return null
        }

        return if (status == HttpStatus.OK) history else null
    }

    private fun withoutScheme(url: String) = url.substringAfter("://").trimEnd('/')
}
