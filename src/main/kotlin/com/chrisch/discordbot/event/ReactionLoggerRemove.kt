package com.chrisch.discordbot.event

import com.chrisch.discordbot.config.Config
import com.chrisch.discordbot.util.Utils.getEmojiFormat
import com.chrisch.discordbot.util.Utils.getMessageUrl
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.ReactionRemoveEvent
import discord4j.core.`object`.entity.channel.TopLevelGuildMessageChannel
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

@Service
class ReactionLoggerRemove(private val config: Config) : EventListener<ReactionRemoveEvent> {

    override val eventType: Class<ReactionRemoveEvent> = ReactionRemoveEvent::class.java

    override suspend fun execute(event: ReactionRemoveEvent) {
        if (event.guildId.isEmpty) return

        if ((config.trackedMessageIds.isEmpty() && config.trackedChannelIds.isEmpty()) || config.logsReactionsChannelId.isBlank()) {
            return
        }

        val user = event.user.awaitSingle()

        if (user.isBot) {
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
                    "${user.tag}'s reaction was removed: " +
                            "${getEmojiFormat(event.emoji)} from message:\n" +
                            getMessageUrl(event.guildId.orElseThrow(), event.channelId, event.messageId)
                )
            }.awaitSingle()
    }
}
