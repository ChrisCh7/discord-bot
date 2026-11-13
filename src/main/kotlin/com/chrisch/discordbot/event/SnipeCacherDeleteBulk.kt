package com.chrisch.discordbot.event

import com.chrisch.discordbot.util.SnipeStore
import com.chrisch.discordbot.util.Utils.getEmbedCreateSpecFromEmbed
import discord4j.core.event.domain.message.MessageBulkDeleteEvent
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

@Service
class SnipeCacherDeleteBulk(private val snipeStore: SnipeStore) : EventListener<MessageBulkDeleteEvent> {
    override suspend fun execute(event: MessageBulkDeleteEvent) {
        val messages = event.messages.sortedBy { it.id }
        for (message in messages) {
            if (snipeStore.isSnipe(message)) {
                message.channel
                    .flatMap {
                        it.createMessage("resending snipe because it was deleted by a mod:")
                            .withEmbeds(getEmbedCreateSpecFromEmbed(message.embeds.first()))
                    }.awaitSingle()

                continue
            }

            if (message.author.map { it.isBot }.orElse(false)) continue

            if (message == messages.last()) {
                snipeStore.add(message)
            }
        }
    }
}
