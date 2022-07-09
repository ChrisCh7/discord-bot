package com.chrisch.discordbot.util

import discord4j.core.`object`.command.ApplicationCommandInteractionOptionValue
import discord4j.core.`object`.entity.Message
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent

object Utils {
    fun getOptionValue(event: ChatInputInteractionEvent, optionName: String): ApplicationCommandInteractionOptionValue {
        return event.getOption(optionName).flatMap { it.value }.orElseThrow()
    }

    fun getCodeBlock(language: String, content: String): String {
        return "```$language\n$content\n```"
    }

    fun getMessageUrl(message: Message): String {
        return "https://discord.com/channels/${
            message.guildId.orElseThrow().asString()
        }/${message.channelId.asString()}/${message.id.asString()}"
    }
}
