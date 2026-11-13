package com.chrisch.discordbot.util

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Message
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Component
class SnipeStore {
    data class DeletedMessage(
        val message: String, val author: String, val authorId: Snowflake, val images: List<String>,
        val stickers: List<String>, val deletionTimestamp: Instant
    )

    val deletedMessages: ConcurrentHashMap<Snowflake, List<DeletedMessage>> = ConcurrentHashMap()

    val deletedSnipes: CopyOnWriteArrayList<Snowflake> = CopyOnWriteArrayList()

    fun isSnipe(message: Message): Boolean =
        message.author.map { it.id == message.client.selfId }.orElse(false) &&
                message.embeds.firstOrNull()?.title?.orElse(null) == "Snipe"

    fun add(message: Message) {
        val deletedMessage = DeletedMessage(
            message = message.content,
            author = message.author.map { it.tag }.orElse(""),
            authorId = message.author.map { it.id }.orElse(Snowflake.of("0")),
            images = message.attachments.map { it.proxyUrl },
            stickers = message.stickersItems.map { it.imageUrl },
            deletionTimestamp = Instant.now()
        )
        deletedMessages.compute(message.channelId) { _, messages -> append(messages.orEmpty(), deletedMessage) }
    }

    private fun append(messages: List<DeletedMessage>, new: DeletedMessage): List<DeletedMessage> {
        if (new.message.startsWith("?purge")) return messages

        val previousBySameAuthor = messages.lastOrNull { it.authorId == new.authorId }
        if (previousBySameAuthor != null && new.message.length <= 5 &&
            new.deletionTimestamp.epochSecond - previousBySameAuthor.deletionTimestamp.epochSecond <= 5
        ) {
            return messages
        }

        return (messages + new).takeLast(MAX_PER_CHANNEL)
    }

    private companion object {
        const val MAX_PER_CHANNEL = 20
    }
}
