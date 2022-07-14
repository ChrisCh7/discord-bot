package com.chrisch.discordbot.autocomplete

import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.interaction.AutoCompleteInteractionEvent
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class AutocompleteListener(
    autocompleteHandlersChat: List<AutocompleteHandler<ChatInputAutoCompleteEvent>>, client: GatewayDiscordClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    init {
        client.on(ChatInputAutoCompleteEvent::class.java).flatMap { handle(it, autocompleteHandlersChat) }
            .onErrorResume { logAutocompleteError(it) }.subscribe()
    }

    fun <T : AutoCompleteInteractionEvent> handle(event: T, handlers: List<AutocompleteHandler<T>>): Mono<Void> {
        if (event !is ChatInputAutoCompleteEvent) return Mono.empty() // change if other types appear in the future

        return Flux.fromIterable(handlers)
            .filter { it.name == event.commandName }
            .next()
            .flatMap { it.handle(event) }
    }

    private fun logAutocompleteError(throwable: Throwable): Mono<Void> {
        log.error("Unable to process autocomplete", throwable)
        return Mono.empty()
    }
}
