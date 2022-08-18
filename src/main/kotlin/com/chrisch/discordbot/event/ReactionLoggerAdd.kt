package com.chrisch.discordbot.event

import com.chrisch.discordbot.util.Utils.getEmojiFormat
import com.chrisch.discordbot.util.Utils.getMessageUrl
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.ReactionAddEvent
import discord4j.core.`object`.entity.channel.TopLevelGuildMessageChannel
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ReactionLoggerAdd : EventListener<ReactionAddEvent> {

    @Value("\${TRACKED_MESSAGE_IDS}")
    private val trackedMessageIds: List<String> = listOf()

    @Value("\${TRACKED_CHANNEL_IDS}")
    private val trackedChannelIds: List<String> = listOf()

    @Value("\${LOGS_REACTIONS_CHANNEL_ID}")
    private val logsReactionsChannelId: String = ""

    override val eventType: Class<ReactionAddEvent> = ReactionAddEvent::class.java

    override suspend fun execute(event: ReactionAddEvent) {
        if (event.member.isEmpty) return

        if ((trackedMessageIds.isEmpty() && trackedChannelIds.isEmpty()) || logsReactionsChannelId.isBlank()) {
            return
        }

        if (event.member.map { it.isBot }.orElse(true)) {
            return
        }

        if (!trackedMessageIds.contains(event.messageId.asString()) &&
            !trackedChannelIds.contains(event.channelId.asString())
        ) {
            return
        }

        event.guild
            .flatMap { it.getChannelById(Snowflake.of(logsReactionsChannelId)) }
            .ofType(TopLevelGuildMessageChannel::class.java)
            .flatMap {
                it.createMessage(
                    "${event.member.orElseThrow().tag} added reaction: " +
                            "${getEmojiFormat(event.emoji)} to message:\n" +
                            getMessageUrl(event.guildId.orElseThrow(), event.channelId, event.messageId)
                )
            }
            .awaitSingle()
    }
}
