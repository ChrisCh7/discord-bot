package com.chrisch.discordbot.util

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.Embed
import discord4j.core.`object`.command.ApplicationCommandInteractionOptionValue
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.spec.EmbedCreateSpec

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

    fun getEmbedCreateSpecFromEmbed(embed: Embed): EmbedCreateSpec {
        return EmbedCreateSpec.builder()
            .apply {
                embed.color.ifPresent { color(it) }
                embed.title.ifPresent { title(it) }
                embed.description.ifPresent { description(it) }
                embed.fields.forEach { field ->
                    addField(field.name, field.value, field.isInline)
                }
                embed.image.ifPresent { image(it.proxyUrl) }
                embed.footer.ifPresent { footer ->
                    footer(footer.text, footer.iconUrl.orElse(null))
                }
                embed.thumbnail.ifPresent { thumbnail(it.proxyUrl) }
                embed.timestamp.ifPresent { timestamp(it) }
            }
            .build()
    }
}
