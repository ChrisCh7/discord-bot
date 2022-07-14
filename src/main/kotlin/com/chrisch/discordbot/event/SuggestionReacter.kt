package com.chrisch.discordbot.event

import com.chrisch.discordbot.util.EmojiStore
import com.chrisch.discordbot.util.Utils.getReactionEmoji
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class SuggestionReacter(private val emojiStore: EmojiStore) : EventListener<MessageCreateEvent> {

    @Value("\${SUGGESTIONS_CHANNEL_ID}")
    private val suggestionsChannelId: String = ""

    override val eventType: Class<MessageCreateEvent> = MessageCreateEvent::class.java

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        val message = event.message

        if (message.author.map { it.isBot }.orElse(true)) {
            return Mono.empty()
        }

        if (message.channelId == Snowflake.of(suggestionsChannelId)) {
            return message.addReaction(getReactionEmoji(emojiStore.emojis["upvote"]!!))
                .then(message.addReaction(getReactionEmoji(emojiStore.emojis["downvote"]!!)))
        }

        return Mono.empty()
    }
}
