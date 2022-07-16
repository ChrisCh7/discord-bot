package com.chrisch.discordbot.event

import com.chrisch.discordbot.util.Utils.getMessageUrl
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.ReactionAddEvent
import discord4j.core.`object`.entity.channel.TopLevelGuildMessageChannel
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class ReactionLoggerAdd : EventListener<ReactionAddEvent> {

    @Value("\${TRACKED_MESSAGE_IDS}")
    private val trackedMessageIds: List<String> = listOf()

    @Value("\${TRACKED_CHANNEL_IDS}")
    private val trackedChannelIds: List<String> = listOf()

    @Value("\${LOGS_REACTIONS_CHANNEL_ID}")
    private val logsReactionsChannelId: String = ""

    override val eventType: Class<ReactionAddEvent> = ReactionAddEvent::class.java

    override fun execute(event: ReactionAddEvent): Mono<Void> {
        if (event.member.isEmpty) return Mono.empty()

        if ((trackedMessageIds.isEmpty() && trackedChannelIds.isEmpty()) || logsReactionsChannelId.isBlank()) {
            return Mono.empty()
        }

        if (event.member.map { it.isBot }.orElse(true)) {
            return Mono.empty()
        }

        if (!trackedMessageIds.contains(event.messageId.asString()) &&
            !trackedChannelIds.contains(event.channelId.asString())
        ) {
            return Mono.empty()
        }

        return event.guild
            .flatMap { it.getChannelById(Snowflake.of(logsReactionsChannelId)) }
            .ofType(TopLevelGuildMessageChannel::class.java)
            .flatMap {
                it.createMessage(
                    "${event.member.orElseThrow().tag} added reaction: " +
                            "${event.emoji.asCustomEmoji().orElseThrow().asFormat()} to message:\n" +
                            getMessageUrl(event.guildId.orElseThrow(), event.channelId, event.messageId)
                )
            }
            .then()
    }
}
