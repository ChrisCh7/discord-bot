package com.chrisch.discordbot.event

import com.chrisch.discordbot.util.EmojiStore
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.channel.TopLevelGuildMessageChannel
import discord4j.core.spec.WebhookExecuteSpec
import discord4j.rest.util.Image
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class EmojiReplacer(private val emojiStore: EmojiStore) : EventListener<MessageCreateEvent> {
    override val eventType: Class<MessageCreateEvent> = MessageCreateEvent::class.java

    override suspend fun execute(event: MessageCreateEvent) {
        val message = event.message

        if (message.author.map { it.isBot }.orElse(true) || message.guildId.isEmpty ||
            message.content.isBlank() || message.content.contains("@") || message.referencedMessage.isPresent
        ) {
            return
        }

        val messageParts = message.content.split(" +".toRegex()).filter { it.isNotBlank() }

        val finalMessageParts: MutableList<String> = mutableListOf()
        var replacedCount = 0

        for (messagePart in messageParts) {
            if (messagePart.startsWith(":") && messagePart.endsWith(":")) {
                val emoji = getEmoji(messagePart.trim(':'))

                if (emoji != null) {
                    replacedCount++
                    finalMessageParts.add(emoji)
                } else {
                    finalMessageParts.add(messagePart)
                }
            } else {
                finalMessageParts.add(messagePart)
            }
        }

        if (replacedCount == 0) {
            return
        }

        val finalMessage = finalMessageParts.joinToString(" ")

        val channel = message.channel.awaitSingle()

        if (channel is TopLevelGuildMessageChannel) {
            val webhook = channel.createWebhook(message.authorAsMember.awaitSingle().displayName)
                .withAvatarOrNull(Image.ofUrl(message.author.orElseThrow().avatarUrl).awaitSingle())
                .withReason("${message.author.orElseThrow().tag} posted a message containing emoji(s)")
                .awaitSingle()

            webhook.executeAndWait(WebhookExecuteSpec.builder().content(finalMessage).build())
                .onErrorResume { Mono.empty() }.awaitSingleOrNull()
            webhook.delete().awaitSingleOrNull()
            message.delete().awaitSingleOrNull()
        }
    }

    fun getEmoji(name: String): String? {
        val emojiMatches = emojiStore.emojis.filterKeys { emojiName -> emojiName.equals(name, true) }
        return emojiMatches.entries.firstOrNull()?.value
    }
}
