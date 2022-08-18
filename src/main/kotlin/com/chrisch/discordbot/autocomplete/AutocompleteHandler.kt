package com.chrisch.discordbot.autocomplete

import discord4j.core.event.domain.interaction.AutoCompleteInteractionEvent

interface AutocompleteHandler<T : AutoCompleteInteractionEvent> {

    val name: String

    suspend fun handle(event: T)
}
