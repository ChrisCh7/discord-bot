package com.chrisch.discordbot.command

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent
import discord4j.discordjson.json.ApplicationCommandRequest

interface CommandHandler<T : ApplicationCommandInteractionEvent> {

    val name: String

    val command: ApplicationCommandRequest

    suspend fun handle(event: T)
}
