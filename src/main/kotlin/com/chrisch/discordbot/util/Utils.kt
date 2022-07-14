package com.chrisch.discordbot.util

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandInteractionOptionValue
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.reaction.ReactionEmoji

object Utils {

    private val emojiRegex = Regex("<([^:]?):(.+):(.+)>")

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

    fun getReactionEmoji(format: String): ReactionEmoji {
        val (animated, name, id) = emojiRegex.find(format)!!.destructured
        return ReactionEmoji.of(id.toLong(), name, animated == "a")
    }
}
