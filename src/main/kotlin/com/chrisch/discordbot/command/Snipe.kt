package com.chrisch.discordbot.command

import com.chrisch.discordbot.util.CustomColor
import com.chrisch.discordbot.util.SnipeStore
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData
import discord4j.discordjson.json.ApplicationCommandOptionData
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.rest.util.Permission
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service

@Service
class Snipe(private val snipeStore: SnipeStore) : CommandHandler<ChatInputInteractionEvent> {

    override val name: String = "snipe"

    override val command: ApplicationCommandRequest
        get() = ApplicationCommandRequest.builder()
            .name(name)
            .description("Shows the last deleted message in the current channel.")
            .addOption(
                ApplicationCommandOptionData.builder()
                    .name("nth")
                    .description("Choose message to snipe (1 = last) (must be admin to use)")
                    .type(ApplicationCommandOption.Type.INTEGER.value)
                    .required(false)
                    .addAllChoices(getSnipeOptionChoices())
                    .build()
            ).build()

    override suspend fun handle(event: ChatInputInteractionEvent) {
        if (event.interaction.member.isEmpty) {
            event.reply("Command only usable in a guild").withEphemeral(true).awaitSingleOrNull()
            return
        }

        if (!snipeStore.deletedMessages.containsKey(event.interaction.channelId) ||
            snipeStore.deletedMessages[event.interaction.channelId]!!.isEmpty()
        ) {
            event.reply("No deleted messages found in cache.").awaitSingleOrNull()
            return
        }

        val nth = event.getOption("nth").flatMap { it.value }.map { it.asLong().toInt() }.orElse(1)

        if (nth > 1 && !event.interaction.member.orElseThrow().basePermissions.awaitSingle()
                .contains(Permission.ADMINISTRATOR)
        ) {
            event.reply("You don't have permission to use this command.").withEphemeral(true)
                .awaitSingleOrNull()
            return
        }

        if (snipeStore.deletedMessages[event.interaction.channelId]!!.size < nth) {
            event.reply(
                "You requested message #$nth, but there are only " +
                        "${snipeStore.deletedMessages[event.interaction.channelId]!!.size} messages available."
            ).withEphemeral(true).awaitSingleOrNull()
            return
        }

        val deletedMessagesInChannel = snipeStore.deletedMessages[event.interaction.channelId]!!
        val lastDeletedMessageInChannel = deletedMessagesInChannel[deletedMessagesInChannel.size - nth]

        val embed = EmbedCreateSpec.builder()
            .color(CustomColor.GREEN)
            .title("Snipe")
            .addField("Author", lastDeletedMessageInChannel.author, false)
            .apply {
                if (lastDeletedMessageInChannel.message.isNotBlank()) {
                    addField("Message", lastDeletedMessageInChannel.message, false)
                }
                if (lastDeletedMessageInChannel.images.isNotEmpty()) {
                    image(lastDeletedMessageInChannel.images.first())
                }
                if (lastDeletedMessageInChannel.stickers.isNotEmpty()) {
                    addField("Sticker", lastDeletedMessageInChannel.stickers.first(), false)
                }
            }
            .build()

        event.reply(InteractionApplicationCommandCallbackSpec.builder().addEmbed(embed).build()).awaitSingleOrNull()
    }

    private fun getSnipeOptionChoices(): List<ApplicationCommandOptionChoiceData> {
        return (1..20).map {
            ApplicationCommandOptionChoiceData.builder().name(it.toString()).value(it).build()
        }
    }
}
