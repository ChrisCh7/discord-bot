package com.chrisch.discordbot.component

import discord4j.core.event.domain.interaction.ComponentInteractionEvent

interface ComponentHandler<T : ComponentInteractionEvent> {

    val customId: String

    suspend fun handle(event: T)
}
