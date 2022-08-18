package com.chrisch.discordbot.command

import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.event.domain.interaction.MessageInteractionEvent
import discord4j.core.event.domain.interaction.UserInteractionEvent
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class CommandListener(
    commandHandlersChat: List<CommandHandler<ChatInputInteractionEvent>>,
    commandHandlersMessage: List<CommandHandler<MessageInteractionEvent>>,
    commandHandlersUser: List<CommandHandler<UserInteractionEvent>>, client: GatewayDiscordClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    init {
        client.on(ChatInputInteractionEvent::class.java).flatMap { handle(it, commandHandlersChat) }
            .onErrorResume { logCommandError(it) }.subscribe()
        client.on(MessageInteractionEvent::class.java).flatMap { handle(it, commandHandlersMessage) }
            .onErrorResume { logCommandError(it) }.subscribe()
        client.on(UserInteractionEvent::class.java).flatMap { handle(it, commandHandlersUser) }
            .onErrorResume { logCommandError(it) }.subscribe()
    }

    fun <T : ApplicationCommandInteractionEvent> handle(event: T, handlers: List<CommandHandler<T>>): Mono<Void> {
        return Flux.fromIterable(handlers)
            .filter { it.name == event.commandName }
            .next()
            .flatMap { mono { it.handle(event) }.then() }
    }

    private fun logCommandError(throwable: Throwable): Mono<Void> {
        log.error("Unable to process command", throwable)
        return Mono.empty()
    }
}
