package com.chrisch.discordbot.event

import com.chrisch.discordbot.util.CustomColor
import discord4j.core.event.domain.message.MessageUpdateEvent
import discord4j.core.spec.EmbedCreateSpec
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

@Service
class GhostPingByEditDetector : EventListener<MessageUpdateEvent> {
    override val eventType: Class<MessageUpdateEvent> = MessageUpdateEvent::class.java

    override suspend fun execute(event: MessageUpdateEvent) {
        if (event.old.isEmpty) {
            return
        }

        val oldMessage = event.old.orElseThrow()

        if (oldMessage.author.map { it.isBot }.orElse(true)) {
            return
        }

        if (oldMessage.userMentionIds.isNotEmpty() || oldMessage.roleMentionIds.isNotEmpty()) {
            if (oldMessage.userMentions.stream().allMatch { user ->
                    user.id == oldMessage.author.orElseThrow().id || user.isBot
                }) {
                return
            }

            event.message
                .filter { message ->
                    !(message.userMentionIds.containsAll(oldMessage.userMentionIds) &&
                            message.roleMentionIds.containsAll(oldMessage.roleMentionIds))
                }.map { message ->
                    EmbedCreateSpec.builder()
                        .color(CustomColor.GREEN)
                        .title("Ghost Ping By Edit Detected!")
                        .addField("Author", oldMessage.author.orElseThrow().tag, false)
                        .addField("Old Message", oldMessage.content, false)
                        .addField("New Message", message.content, false)
                        .build()
                }.zipWith(event.channel)
                .flatMap { objects ->
                    objects.t2.createMessage(objects.t1)
                }.awaitSingle()
        }
    }
}
