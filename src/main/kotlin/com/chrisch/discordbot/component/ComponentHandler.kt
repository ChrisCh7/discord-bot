package com.chrisch.discordbot.component

import discord4j.core.event.domain.interaction.ComponentInteractionEvent
import reactor.core.publisher.Mono

interface ComponentHandler<T : ComponentInteractionEvent> {

    val customId: String

    fun handle(event: T): Mono<Void>
}
