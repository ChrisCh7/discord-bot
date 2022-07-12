package com.chrisch.discordbot.event

import com.chrisch.discordbot.util.Utils.getMessageUrl
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.channel.TopLevelGuildMessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class CountChecker : EventListener<MessageCreateEvent> {

    @Value("\${COUNTING_CHANNEL_ID}")
    private val countingChannelId: String = ""

    override val eventType: Class<MessageCreateEvent> = MessageCreateEvent::class.java

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        val message = event.message

        if (message.author.map { it.isBot }.orElse(true) || message.channelId != Snowflake.of(countingChannelId) ||
            message.content.isBlank()
        ) {
            return Mono.empty()
        }

        val nr = message.content.toIntOrNull() ?: return Mono.empty()

        return mono {
            val lastNr = message.channel
                .ofType(TopLevelGuildMessageChannel::class.java)
                .flatMapMany { it.getMessagesBefore(message.id) }
                .take(1).single()
                .map { it.content }
                .map { it.toInt() }
                .onErrorResume { Mono.empty() }.awaitSingleOrNull() ?: return@mono

            if (nr == lastNr + 1) {
                return@mono
            }

            val notif = "The number you entered doesn't look like the right one (${lastNr + 1}).\n" +
                    "Please change it if necessary.\n" + getMessageUrl(message)

            message.authorAsMember.awaitSingle().privateChannel.awaitSingle().createMessage(notif).awaitSingle()
        }.then()
    }
}
