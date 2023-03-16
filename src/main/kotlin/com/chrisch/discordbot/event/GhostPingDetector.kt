package com.chrisch.discordbot.event

import com.chrisch.discordbot.util.CustomColor
import com.chrisch.discordbot.util.Utils.getMessageUrl
import discord4j.core.event.domain.message.MessageDeleteEvent
import discord4j.core.`object`.entity.Message
import discord4j.core.spec.EmbedCreateSpec
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

@Service
class GhostPingDetector : EventListener<MessageDeleteEvent> {
    override val eventType: Class<MessageDeleteEvent> = MessageDeleteEvent::class.java

    override suspend fun execute(event: MessageDeleteEvent) {
        if (event.message.isEmpty) return

        val message = event.message.orElseThrow()

        if (message.author.map { it.isBot }.orElse(true)) {
            return
        }

        if (message.userMentionIds.isEmpty() && message.roleMentionIds.isEmpty()) {
            return
        }

        if (message.userMentions.stream().allMatch { user ->
                user.id == message.author.orElseThrow().id || user.isBot
            }
        ) {
            return
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
                    addField(
                        "Message replied to",
                        getMessageUrl(message.guildId.orElseThrow(), repliedToMessage.channelId, repliedToMessage.id),
                        true
                    )
                }
            }
            .build()

        event.channel
            .flatMap { it.createMessage(embed) }
            .awaitSingle()
    }
}
