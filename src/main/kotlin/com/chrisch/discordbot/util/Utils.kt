package com.chrisch.discordbot.util

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.Embed
import discord4j.core.`object`.command.ApplicationCommandInteractionOptionValue
import discord4j.core.`object`.emoji.Emoji
import discord4j.core.`object`.entity.Message
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.MessageCreateMono
import discord4j.discordjson.json.MessageReferenceData

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

    fun getMessageUrl(guildId: Snowflake, channelId: Snowflake, messageId: Snowflake): String {
        return "https://discord.com/channels/${
            guildId.asString()
        }/${channelId.asString()}/${messageId.asString()}"
    }

    fun getEmoji(format: String): Emoji {
        val (animated, name, id) = emojiRegex.find(format)!!.destructured
        return Emoji.of(id.toLong(), name, animated == "a")
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

    fun getEmojiFormat(emoji: Emoji): String {
        return emoji.asCustomEmoji().map { it.asFormat() }
            .or { emoji.asUnicodeEmoji().map { it.raw } }
            .orElse("unknown emoji")
    }

    fun MessageCreateMono.withMessageReference(message: Message): MessageCreateMono {
        return this.withMessageReference(
            MessageReferenceData.builder()
                .type(message.type.value)
                .messageId(message.id.asString())
                .channelId(message.channelId.asString())
                .guildId(message.guildId.orElseThrow().asString())
                .failIfNotExists(true)
                .build()
        )
    }
}
