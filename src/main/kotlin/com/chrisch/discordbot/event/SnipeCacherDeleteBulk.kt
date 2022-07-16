package com.chrisch.discordbot.event

import com.chrisch.discordbot.util.SnipeStore
import com.chrisch.discordbot.util.Utils.getEmbedCreateSpecFromEmbed
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageBulkDeleteEvent
import discord4j.core.`object`.entity.Message
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

@Service
class SnipeCacherDeleteBulk(private val snipeStore: SnipeStore) : EventListener<MessageBulkDeleteEvent> {
    override val eventType: Class<MessageBulkDeleteEvent> = MessageBulkDeleteEvent::class.java

    override fun execute(event: MessageBulkDeleteEvent): Mono<Void> {
        return mono {
            val messages = event.messages.sortedBy { it.id }
            for (message in messages) {
                if (message.author.map { it.isBot }
                        .orElse(false) && message.author.orElseThrow().id == event.client.selfId) {
                    if (message.embeds.isEmpty()) continue

                    if (message.embeds.first().title.orElse("title") != "Snipe") continue

                    message.channel
                        .flatMap {
                            it.createMessage("resending snipe because it was deleted by a mod:")
                                .withEmbeds(getEmbedCreateSpecFromEmbed(message.embeds.first()))
                        }.awaitSingle()

                    continue
                } else if (message.author.map { it.isBot }.orElse(false)) continue

                if (message == messages.last()) {
                    addToDeletedMessages(message)
                }
            }
        }.then()
    }

    private fun addToDeletedMessages(message: Message) {
        if (!snipeStore.deletedMessages.containsKey(message.channelId)) {
            snipeStore.deletedMessages[message.channelId] = CopyOnWriteArrayList()
        }

        snipeStore.deletedMessages[message.channelId]!!.add(extractMessageData(message))
        verifyLastMessage(snipeStore.deletedMessages[message.channelId]!!)

        if (snipeStore.deletedMessages[message.channelId]!!.size > 20) {
            snipeStore.deletedMessages[message.channelId]!!.removeFirst()
        }
    }

    private fun extractMessageData(message: Message): SnipeStore.DeletedMessage {
        return SnipeStore.DeletedMessage(
            message = message.content,
            author = message.author.map { it.tag }.orElse(""),
            authorId = message.author.map { it.id }.orElse(Snowflake.of("0")),
            images = message.attachments.map { it.proxyUrl },
            stickers = message.stickers.map { it.data.asset() },
            deletionTimestamp = Instant.now()
        )
    }

    private fun verifyLastMessage(messages: CopyOnWriteArrayList<SnipeStore.DeletedMessage>) {
        val lastDeletedMessage = messages.last()
        val isPurgeCommand = lastDeletedMessage.message.startsWith("?purge")

        if (messages.size == 1) {
            if (isPurgeCommand) {
                messages.removeLast()
                return
            } else {
                return
            }
        } else if (isPurgeCommand) {
            messages.removeLast()
            return
        }

        val previousDeletedMessagesBySameAuthor =
            messages.filter { it.authorId == lastDeletedMessage.authorId }.toMutableList()
        previousDeletedMessagesBySameAuthor.removeLast() // removing the current message

        if (previousDeletedMessagesBySameAuthor.isEmpty()) return

        val previousDeletedMessageBySameAuthor = previousDeletedMessagesBySameAuthor.removeLast()

        if (lastDeletedMessage.message.length <= 5 &&
            lastDeletedMessage.deletionTimestamp.epochSecond - previousDeletedMessageBySameAuthor.deletionTimestamp.epochSecond <= 5
        ) {
            messages.removeLast()
        }
    }
}
