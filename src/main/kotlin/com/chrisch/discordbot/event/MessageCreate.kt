package com.chrisch.discordbot.event

import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Color
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class MessageCreate : EventListener<MessageCreateEvent> {
    override val eventType: Class<MessageCreateEvent>
        get() = MessageCreateEvent::class.java

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        val message = event.message

        if (message.author.map { it.isBot }.orElse(true)) {
            return Mono.empty()
        }

        if (message.content.startsWith("dog~")) {
            val embed = EmbedCreateSpec.builder()
                .color(Color.GREEN)
                .description("Please use slash `/` commands to interact with the bot.")
                .footer("This message will self-destruct in 10 sec", null)
                .build()

            return message.channel
                .flatMap { channel -> channel.createMessage(embed).withMessageReference(message.id) }
                .delayElement(Duration.ofSeconds(10))
                .flatMap { it.delete() }
        }

        return Mono.empty()
    }
}
