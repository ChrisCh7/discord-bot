package com.chrisch.discordbot.command

import com.chrisch.discordbot.util.Utils.getOptionValue
import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.discordjson.json.ApplicationCommandOptionData
import discord4j.discordjson.json.ApplicationCommandRequest
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientException
import reactor.kotlin.core.publisher.toMono
import reactor.netty.http.client.HttpClient


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

    override suspend fun handle(event: ChatInputInteractionEvent) {
        event.deferReply().awaitSingleOrNull()

        val history = mutableListOf<String>()

        val client = WebClient.builder().clientConnector(
            ReactorClientHttpConnector(
                HttpClient.create().followRedirect(true)
                    .doOnRedirect { res, _ -> history.add(res.responseHeaders().get("Location")) })
        ).build()

        val url = getOptionValue(event, "url").asString()
        var responseResult: HttpStatusCode = HttpStatus.NOT_FOUND

        try {
            responseResult = client.head().uri(url).exchangeToMono { it.statusCode().toMono() }.awaitSingle()
        } catch (_: WebClientException) {
            // ignored
        }

        if (responseResult != HttpStatus.OK) {
            event.editReply("Error accessing link").awaitSingle()
            return
        }

        if (history.isEmpty() || history.size == 1 && history.first().contains("://$url")) {
            event.editReply("Link doesn't redirect").awaitSingle()
            return
        }

        event.editReply("Short link: <${url}>\nUnshortened link: <${history.last()}>").awaitSingle()
    }
}
