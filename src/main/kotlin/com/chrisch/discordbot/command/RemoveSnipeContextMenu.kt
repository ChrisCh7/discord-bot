package com.chrisch.discordbot.command

import discord4j.core.event.domain.interaction.MessageInteractionEvent
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.rest.util.Permission
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class RemoveSnipeContextMenu : CommandHandler<MessageInteractionEvent> {

    override val name: String = "Remove snipe"

    override val command: ApplicationCommandRequest
        get() = ApplicationCommandRequest.builder()
            .name(name)
            .type(3)
            .build()

    override fun handle(event: MessageInteractionEvent): Mono<Void> {
        if (event.interaction.member.isEmpty) {
            return event.reply("Command only usable in a guild").withEphemeral(true)
        }

        val member = event.interaction.member.orElseThrow()
        val message = event.resolvedMessage

        if (message.embeds.size == 1 && message.embeds[0].title.orElse("title").equals("Snipe")) {
            return mono {
                if (!member.basePermissions.awaitSingle().contains(Permission.ADMINISTRATOR)) {
                    event.reply("You don't have permission to use this command.").withEphemeral(true)
                        .awaitSingleOrNull()
                    return@mono
                }

                event.deferReply().awaitSingleOrNull()
                message.embeds.removeLast()
                message.delete().awaitSingleOrNull()
                event.deleteReply().awaitSingleOrNull()
            }.then()
        }

        return event.reply("That is not a snipe.").withEphemeral(true)
    }
}
