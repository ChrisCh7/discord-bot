package com.chrisch.discordbot.command

import com.chrisch.discordbot.util.SnipeStore
import com.chrisch.discordbot.util.Utils
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.discordjson.json.ApplicationCommandOptionData
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.rest.util.Permission
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service

@Service
class RemoveSnipe(private val snipeStore: SnipeStore) : CommandHandler<ChatInputInteractionEvent> {

    override val name: String = "removesnipe"

    override val command: ApplicationCommandRequest
        get() = ApplicationCommandRequest.builder()
            .name(name)
            .description("Removes a snipe. Must be admin to use.")
            .addOption(
                ApplicationCommandOptionData.builder()
                    .name("snipe_id")
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
                .contains(Permission.ADMINISTRATOR)
        ) {
            event.reply("You don't have permission to use this command.").withEphemeral(true)
                .awaitSingleOrNull()
            return
        }

        val snipeMsg = event.interaction.channel.awaitSingle()
            .getMessageById(Snowflake.of(Utils.getOptionValue(event, "snipe_id").asString())).awaitSingle()

        event.deferReply().awaitSingleOrNull()

        snipeStore.deletedSnipes.add(snipeMsg.id)

        snipeMsg.delete().awaitSingleOrNull()
        event.deleteReply().awaitSingleOrNull()
    }
}
