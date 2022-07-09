package com.chrisch.discordbot.command

import com.chrisch.discordbot.util.Utils.getCodeBlock
import com.chrisch.discordbot.util.Utils.getOptionValue
import discord4j.common.util.Snowflake
import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.discordjson.json.ApplicationCommandOptionData
import discord4j.discordjson.json.ApplicationCommandRequest
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class CodeFormat : CommandHandler<ChatInputInteractionEvent> {

    override val name: String
        get() = "codeformat"

    override val command: ApplicationCommandRequest
        get() = ApplicationCommandRequest.builder()
            .name(name)
            .description("Format the message to make the code look nice.")
            .addOption(
                ApplicationCommandOptionData.builder()
                    .name("language")
                    .description("Enter a string")
                    .type(ApplicationCommandOption.Type.STRING.value)
                    .required(true)
                    .build()
            ).addOption(
                ApplicationCommandOptionData.builder()
                    .name("message_id")
                    .description("Enter a string")
                    .type(ApplicationCommandOption.Type.STRING.value)
                    .required(true)
                    .build()
            ).build()

    override fun handle(event: ChatInputInteractionEvent): Mono<Void> {
        return event.interaction.channel
            .flatMap { channel -> channel.getMessageById(Snowflake.of(getOptionValue(event, "message_id").asString())) }
            .flatMap { message ->
                event.reply(
                    getCodeBlock(
                        getOptionValue(event, "language").asString(),
                        message.content
                    )
                )
            }
    }
}
