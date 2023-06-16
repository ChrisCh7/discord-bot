package com.chrisch.discordbot.command

import com.chrisch.discordbot.util.Utils.getOptionValue
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.discordjson.json.ApplicationCommandOptionData
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.rest.util.Image
import discord4j.rest.util.Permission
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class Steal : CommandHandler<ChatInputInteractionEvent> {

    private val log = LoggerFactory.getLogger(javaClass)

    override val name: String = "steal"

    override val command: ApplicationCommandRequest
        get() = ApplicationCommandRequest.builder()
            .name(name)
            .description("Add an emoji to the server. Must have permission to do so normally.")
            .addOption(
                ApplicationCommandOptionData.builder()
                    .name("emoji_name")
                    .description("Enter a string")
                    .type(ApplicationCommandOption.Type.STRING.value)
                    .required(true)
                    .build()
            ).addOption(
                ApplicationCommandOptionData.builder()
                    .name("emoji_url")
                    .description("Enter a string")
                    .type(ApplicationCommandOption.Type.STRING.value)
                    .required(true)
                    .build()
            ).build()

    override suspend fun handle(event: ChatInputInteractionEvent) {
        if (event.interaction.member.isEmpty) {
            event.reply("Command only usable in a guild").withEphemeral(true).awaitSingleOrNull()
            return
        }

        if (!event.interaction.member.orElseThrow().basePermissions.awaitSingle()
                .contains(Permission.MANAGE_GUILD_EXPRESSIONS)
        ) {
            event.reply("You don't have permission to add emojis.").withEphemeral(true).awaitSingleOrNull()
        }

        event.deferReply().awaitSingleOrNull()

        event.interaction.guild.awaitSingle()
            .createEmoji(
                getOptionValue(event, "emoji_name").asString(),
                Image.ofUrl(getOptionValue(event, "emoji_url").asString()).awaitSingle()
            ).flatMap { emoji -> event.editReply("${emoji.asFormat()} added as ${emoji.name}") }
            .doOnError { log.error("Failed to add emoji.", it) }
            .onErrorResume { event.editReply("Failed to add emoji.") }
            .awaitSingle()
    }
}
