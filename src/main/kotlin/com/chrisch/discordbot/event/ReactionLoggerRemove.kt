package com.chrisch.discordbot.event

import com.chrisch.discordbot.util.Utils.getEmojiFormat
import com.chrisch.discordbot.util.Utils.getMessageUrl
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.ReactionRemoveEvent
import discord4j.core.`object`.entity.channel.TopLevelGuildMessageChannel
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class ReactionLoggerRemove : EventListener<ReactionRemoveEvent> {

    @Value("\${TRACKED_MESSAGE_IDS}")
    private val trackedMessageIds: List<String> = listOf()

    @Value("\${TRACKED_CHANNEL_IDS}")
    private val trackedChannelIds: List<String> = listOf()

    @Value("\${LOGS_REACTIONS_CHANNEL_ID}")
    private val logsReactionsChannelId: String = ""

    override val eventType: Class<ReactionRemoveEvent> = ReactionRemoveEvent::class.java

    override fun execute(event: ReactionRemoveEvent): Mono<Void> {
        if (event.guildId.isEmpty) return Mono.empty()

        if ((trackedMessageIds.isEmpty() && trackedChannelIds.isEmpty()) || logsReactionsChannelId.isBlank()) {
            return Mono.empty()
        }

        return mono {
            val user = event.user.awaitSingle()

            if (user.isBot) {
                return@mono
            }

            if (!trackedMessageIds.contains(event.messageId.asString()) &&
                !trackedChannelIds.contains(event.channelId.asString())
            ) {
                return@mono
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
        }.then()
    }
}
