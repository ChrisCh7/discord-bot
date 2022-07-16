package com.chrisch.discordbot.util

import discord4j.common.util.Snowflake
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

    val deletedMessages: ConcurrentHashMap<Snowflake, CopyOnWriteArrayList<DeletedMessage>> = ConcurrentHashMap()

    val deletedSnipes: CopyOnWriteArrayList<Snowflake> = CopyOnWriteArrayList()
}