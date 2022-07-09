package com.chrisch.discordbot.command

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent
import discord4j.discordjson.json.ApplicationCommandRequest
import reactor.core.publisher.Mono

interface CommandHandler<T : ApplicationCommandInteractionEvent> {

    val name: String

    val command: ApplicationCommandRequest

    fun handle(event: T): Mono<Void>
}
