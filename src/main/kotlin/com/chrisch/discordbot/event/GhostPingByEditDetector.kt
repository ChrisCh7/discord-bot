package com.chrisch.discordbot.event

import discord4j.core.event.domain.message.MessageUpdateEvent
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Color
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class GhostPingByEditDetector : EventListener<MessageUpdateEvent> {
    override val eventType: Class<MessageUpdateEvent> = MessageUpdateEvent::class.java

    override fun execute(event: MessageUpdateEvent): Mono<Void> {
        if (event.old.isEmpty) {
            return Mono.empty()
        }

        val oldMessage = event.old.orElseThrow()

        if (oldMessage.author.map { it.isBot }.orElse(true)) {
            return Mono.empty()
        }

        if (oldMessage.userMentionIds.isNotEmpty() || oldMessage.roleMentionIds.isNotEmpty()) {
            if (oldMessage.userMentions.stream().allMatch { user ->
                    user.id == oldMessage.author.orElseThrow().id || user.isBot
                }) {
                return Mono.empty()
            }

            return event.message
                .filter { message ->
                    !(message.userMentionIds.containsAll(oldMessage.userMentionIds) &&
                            message.roleMentionIds.containsAll(oldMessage.roleMentionIds))
                }.map { message ->
                    EmbedCreateSpec.builder()
                        .color(Color.GREEN)
                        .title("Ghost Ping By Edit Detected!")
                        .addField("Author", oldMessage.author.orElseThrow().tag, false)
                        .addField("Old Message", oldMessage.content, false)
                        .addField("New Message", message.content, false)
                        .build()
                }.zipWith(event.channel)
                .flatMap { objects ->
                    objects.t2.createMessage(objects.t1)
                }.then()
        }

        return Mono.empty()
    }
}
