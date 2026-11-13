package com.chrisch.discordbot.event

import com.chrisch.discordbot.util.SnipeStore
import com.chrisch.discordbot.util.Utils.getEmbedCreateSpecFromEmbed
import discord4j.core.event.domain.message.MessageDeleteEvent
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

@Service
class SnipeCacherDelete(private val snipeStore: SnipeStore) : EventListener<MessageDeleteEvent> {
    override suspend fun execute(event: MessageDeleteEvent) {
        if (event.message.isEmpty) return

        val message = event.message.orElseThrow()

        if (snipeStore.isSnipe(message)) {
            if (snipeStore.deletedSnipes.remove(message.id)) return // snipe was deleted using removesnipe

            message.channel
                .flatMap {
                    it.createMessage("resending snipe because it was deleted by a mod:")
                        .withEmbeds(getEmbedCreateSpecFromEmbed(message.embeds.first()))
                }
                .awaitSingle()
            return
        }

        if (message.author.map { it.isBot }.orElse(false)) return

        snipeStore.add(message)
    }
}
