package com.chrisch.discordbot.command

import com.chrisch.discordbot.util.SnipeStore
import discord4j.core.event.domain.interaction.MessageInteractionEvent
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.rest.util.Permission
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service

@Service
class RemoveSnipeContextMenu(private val snipeStore: SnipeStore) : CommandHandler<MessageInteractionEvent> {

    override val name: String = "Remove snipe"

    override val command: ApplicationCommandRequest
        get() = ApplicationCommandRequest.builder()
            .name(name)
            .type(3)
            .build()

    override suspend fun handle(event: MessageInteractionEvent) {
        if (event.interaction.member.isEmpty) {
            event.reply("Command only usable in a guild").withEphemeral(true).awaitSingleOrNull()
            return
        }

        val member = event.interaction.member.orElseThrow()
        val message = event.resolvedMessage

        if (message.embeds.size == 1 && message.embeds[0].title.orElse("title").equals("Snipe")) {
            if (!member.basePermissions.awaitSingle().contains(Permission.ADMINISTRATOR)) {
                event.reply("You don't have permission to use this command.").withEphemeral(true)
                    .awaitSingleOrNull()
                return
            }

            event.deferReply().awaitSingleOrNull()
            snipeStore.deletedSnipes.add(message.id)
            message.delete().awaitSingleOrNull()
            event.deleteReply().awaitSingleOrNull()
            return
        }

        event.reply("That is not a snipe.").withEphemeral(true).awaitSingleOrNull()
    }
}
