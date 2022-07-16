package com.chrisch.discordbot.event

import com.chrisch.discordbot.util.CustomColor
import com.chrisch.discordbot.util.Utils.getMessageUrl
import discord4j.core.event.domain.message.MessageDeleteEvent
import discord4j.core.`object`.entity.Message
import discord4j.core.spec.EmbedCreateSpec
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class GhostPingDetector : EventListener<MessageDeleteEvent> {
    override val eventType: Class<MessageDeleteEvent> = MessageDeleteEvent::class.java

    override fun execute(event: MessageDeleteEvent): Mono<Void> {
        if (event.message.isEmpty) return Mono.empty()

        val message = event.message.orElseThrow()

        if (message.author.map { it.isBot }.orElse(true)) {
            return Mono.empty()
        }

        if (message.userMentionIds.isEmpty() && message.roleMentionIds.isEmpty()) {
            return Mono.empty()
        }

        if (message.userMentions.stream().allMatch { user ->
                user.id == message.author.orElseThrow().id || user.isBot
            }
        ) {
            return Mono.empty()
        }

        var repliedToMessage: Message? = null
        var repliedToUserTag: String? = null

        if (message.referencedMessage.isPresent) {
            repliedToMessage = message.referencedMessage.orElseThrow()
            repliedToUserTag = repliedToMessage.author.map { it.tag }.orElse(null)
        }

        val embed = EmbedCreateSpec.builder()
            .color(CustomColor.GREEN)
            .title("Ghost Ping Detected!")
            .apply {
                if (repliedToUserTag != null) {
                    addField("Author", message.author.orElseThrow().tag, true)
                    addField("Reply to", repliedToUserTag, true)
                } else {
                    addField("Author", message.author.orElseThrow().tag, false)
                }
            }
            .addField("Message", message.content, false)
            .apply {
                if (repliedToMessage != null) {
                    addField("Message replied to", getMessageUrl(repliedToMessage), true)
                }
            }
            .build()

        return event.channel
            .flatMap { it.createMessage(embed) }
            .then()
    }
}
