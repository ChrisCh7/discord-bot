package com.chrisch.discordbot.util

import discord4j.core.`object`.command.ApplicationCommandInteractionOptionValue
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent

object Utils {
    fun getOptionValue(event: ChatInputInteractionEvent, optionName: String): ApplicationCommandInteractionOptionValue {
        return event.getOption(optionName).flatMap { it.value }.orElseThrow()
    }

    fun getCodeBlock(language: String, content: String): String {
        return "```$language\n$content\n```"
    }
}
