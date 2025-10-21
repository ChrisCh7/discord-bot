package com.chrisch.discordbot.event

import com.chrisch.discordbot.util.CustomColor
import com.chrisch.discordbot.util.Utils.withMessageReference
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.EmbedCreateSpec
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class MessageCreate : EventListener<MessageCreateEvent> {
    override val eventType: Class<MessageCreateEvent> = MessageCreateEvent::class.java

    override suspend fun execute(event: MessageCreateEvent) {
        val message = event.message

        if (message.author.map { it.isBot }.orElse(true)) {
            return
        }

        if (message.content.startsWith("dog~")) {
            val embed = EmbedCreateSpec.builder()
                .color(CustomColor.GREEN)
                .description("Please use slash `/` commands to interact with the bot.")
                .footer("This message will self-destruct in 10 sec", null)
                .build()

            message.channel
                .flatMap { channel -> channel.createMessage(embed).withMessageReference(message) }
                .delayElement(Duration.ofSeconds(10))
                .flatMap { it.delete() }.awaitSingleOrNull()
        }
    }
}
