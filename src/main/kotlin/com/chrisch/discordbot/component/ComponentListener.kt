package com.chrisch.discordbot.component

import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import discord4j.core.event.domain.interaction.ComponentInteractionEvent
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class ComponentListener(
    componentHandlersButton: List<ComponentHandler<ButtonInteractionEvent>>,
    componentHandlersModal: List<ComponentHandler<ModalSubmitInteractionEvent>>,
    componentHandlersSelect: List<ComponentHandler<SelectMenuInteractionEvent>>, client: GatewayDiscordClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    init {
        client.on(ButtonInteractionEvent::class.java)
            .flatMap { handle(it, componentHandlersButton).onErrorResume(::logComponentError) }
            .subscribe()
        client.on(ModalSubmitInteractionEvent::class.java)
            .flatMap { handle(it, componentHandlersModal).onErrorResume(::logComponentError) }
            .subscribe()
        client.on(SelectMenuInteractionEvent::class.java)
            .flatMap { handle(it, componentHandlersSelect).onErrorResume(::logComponentError) }
            .subscribe()
    }

    fun <T : ComponentInteractionEvent> handle(event: T, handlers: List<ComponentHandler<T>>): Mono<Void> {
        return Flux.fromIterable(handlers)
            .filter { it.customId == event.customId }
            .next()
            .flatMap { it.handle(event) }
    }

    private fun logComponentError(throwable: Throwable): Mono<Void> {
        log.error("Unable to process component", throwable)
        return Mono.empty()
    }
}
