package com.chrisch.discordbot.event

import com.chrisch.discordbot.util.EmojiStore
import com.chrisch.discordbot.util.Utils.getReactionEmoji
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class SuggestionReacter(private val emojiStore: EmojiStore) : EventListener<MessageCreateEvent> {

    @Value("\${SUGGESTIONS_CHANNEL_ID}")
    private val suggestionsChannelId: String = ""

    override val eventType: Class<MessageCreateEvent> = MessageCreateEvent::class.java

    override suspend fun execute(event: MessageCreateEvent) {
        val message = event.message

        if (message.author.map { it.isBot }.orElse(true)) {
            return
        }

        if (message.channelId == Snowflake.of(suggestionsChannelId)) {
            message.addReaction(getReactionEmoji(emojiStore.emojis["upvote"]!!))
                .then(message.addReaction(getReactionEmoji(emojiStore.emojis["downvote"]!!)))
                .awaitSingleOrNull()
        }
    }
}
