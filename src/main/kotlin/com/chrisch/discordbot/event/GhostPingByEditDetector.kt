package com.chrisch.discordbot.event

import com.chrisch.discordbot.util.CustomColor
import com.chrisch.discordbot.util.Limit
import discord4j.core.event.domain.message.MessageUpdateEvent
import discord4j.core.spec.EmbedCreateSpec
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

@Service
class GhostPingByEditDetector : EventListener<MessageUpdateEvent> {
    override suspend fun execute(event: MessageUpdateEvent) {
        val oldMessage = event.old.orElse(null) ?: return

        if (oldMessage.author.map { it.isBot }.orElse(true)) {
            return
        }

        if (oldMessage.userMentionIds.isNotEmpty() || oldMessage.roleMentionIds.isNotEmpty()) {
            if (oldMessage.roleMentionIds.isEmpty() && oldMessage.userMentions.stream().allMatch { user ->
                    user.id == oldMessage.author.orElseThrow().id || user.isBot
                }) {
                return
            }

            val message = event.message.awaitSingle()

            if ((message.userMentionIds.containsAll(oldMessage.userMentionIds) &&
                        message.roleMentionIds.containsAll(oldMessage.roleMentionIds)) ||
                oldMessage.content == message.content
            ) {
                return
            }

            val embed = EmbedCreateSpec.builder()
                .color(CustomColor.GREEN)
                .title("Ghost Ping By Edit Detected!")
                .addField("Author", oldMessage.author.orElseThrow().tag, false)
                .addField("Old Message", oldMessage.content.take(Limit.EMBED_FIELD), false)
                .addField("New Message", message.content.take(Limit.EMBED_FIELD), false)
                .build()

            event.channel.flatMap { it.createMessage(embed) }.awaitSingle()
        }
    }
}
