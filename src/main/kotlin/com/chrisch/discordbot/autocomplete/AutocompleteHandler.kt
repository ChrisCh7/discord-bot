package com.chrisch.discordbot.autocomplete

import discord4j.core.event.domain.interaction.AutoCompleteInteractionEvent
import reactor.core.publisher.Mono

interface AutocompleteHandler<T : AutoCompleteInteractionEvent> {

    val name: String

    fun handle(event: T): Mono<Void>
}
