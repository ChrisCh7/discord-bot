package com.chrisch.discordbot.event

import com.chrisch.discordbot.config.Config
import com.chrisch.discordbot.util.Utils.getEmojiFormat
import com.chrisch.discordbot.util.Utils.getMessageUrl
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.ReactionAddEvent
import discord4j.core.`object`.entity.channel.TopLevelGuildMessageChannel
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

@Service
class ReactionLoggerAdd(private val config: Config) : EventListener<ReactionAddEvent> {

    override val eventType: Class<ReactionAddEvent> = ReactionAddEvent::class.java

    override suspend fun execute(event: ReactionAddEvent) {
        if (event.member.isEmpty) return

        if ((config.trackedMessageIds.isEmpty() && config.trackedChannelIds.isEmpty()) || config.logsReactionsChannelId.isBlank()) {
            return
        }

        if (event.member.map { it.isBot }.orElse(true)) {
            return
        }

        if (!config.trackedMessageIds.contains(event.messageId.asString()) &&
            !config.trackedChannelIds.contains(event.channelId.asString())
        ) {
            return
        }

        event.guild
            .flatMap { it.getChannelById(Snowflake.of(config.logsReactionsChannelId)) }
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
