package com.chrisch.discordbot.event

import com.chrisch.discordbot.config.Config
import com.chrisch.discordbot.util.EmojiStore
import com.chrisch.discordbot.util.Utils.getReactionEmoji
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service

@Service
class SuggestionReacter(private val emojiStore: EmojiStore, private val config: Config) :
    EventListener<MessageCreateEvent> {

    override val eventType: Class<MessageCreateEvent> = MessageCreateEvent::class.java

    override suspend fun execute(event: MessageCreateEvent) {
        val message = event.message

        if (message.author.map { it.isBot }.orElse(true)) {
            return
        }

        if (message.channelId == Snowflake.of(config.suggestionsChannelId)) {
            message.addReaction(getReactionEmoji(emojiStore.emojis["upvote"]!!))
                .then(message.addReaction(getReactionEmoji(emojiStore.emojis["downvote"]!!)))
                .awaitSingleOrNull()
        }
    }
}
