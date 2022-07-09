package com.chrisch.discordbot.command

import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.event.domain.interaction.MessageInteractionEvent
import discord4j.core.event.domain.interaction.UserInteractionEvent
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
        client.on(ChatInputInteractionEvent::class.java).flatMap { event -> handle(event, commandHandlersChat) }
            .onErrorResume { throwable -> logCommandError(throwable) }.subscribe()
        client.on(MessageInteractionEvent::class.java).flatMap { event -> handle(event, commandHandlersMessage) }
            .onErrorResume { throwable -> logCommandError(throwable) }.subscribe()
        client.on(UserInteractionEvent::class.java).flatMap { event -> handle(event, commandHandlersUser) }
            .onErrorResume { throwable -> logCommandError(throwable) }.subscribe()
    }

    fun <T : ApplicationCommandInteractionEvent> handle(event: T, commands: List<CommandHandler<T>>): Mono<Void> {
        return Flux.fromIterable(commands)
            .filter { command -> command.name == event.commandName }
            .next()
            .flatMap { command -> command.handle(event) }
    }

    private fun logCommandError(throwable: Throwable): Mono<Void> {
        log.error("Unable to process command", throwable)
        return Mono.empty()
    }
}
