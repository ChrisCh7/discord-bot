package com.chrisch.discordbot.event

import com.chrisch.discordbot.util.Utils.getEmojiFormat
import com.chrisch.discordbot.util.Utils.getMessageUrl
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.ReactionRemoveEvent
import discord4j.core.`object`.entity.channel.TopLevelGuildMessageChannel
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ReactionLoggerRemove : EventListener<ReactionRemoveEvent> {

    @Value("\${TRACKED_MESSAGE_IDS}")
    private val trackedMessageIds: List<String> = listOf()

    @Value("\${TRACKED_CHANNEL_IDS}")
    private val trackedChannelIds: List<String> = listOf()

    @Value("\${LOGS_REACTIONS_CHANNEL_ID}")
    private val logsReactionsChannelId: String = ""

    override val eventType: Class<ReactionRemoveEvent> = ReactionRemoveEvent::class.java

    override suspend fun execute(event: ReactionRemoveEvent) {
        if (event.guildId.isEmpty) return

        if ((trackedMessageIds.isEmpty() && trackedChannelIds.isEmpty()) || logsReactionsChannelId.isBlank()) {
            return
        }

        val user = event.user.awaitSingle()

        if (user.isBot) {
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
                    "${user.tag}'s reaction was removed: " +
                            "${getEmojiFormat(event.emoji)} from message:\n" +
                            getMessageUrl(event.guildId.orElseThrow(), event.channelId, event.messageId)
                )
            }.awaitSingle()
    }
}
