package com.chrisch.discordbot.command

import com.chrisch.discordbot.util.Utils.getOptionValue
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.core.`object`.entity.channel.TopLevelGuildMessageChannel
import discord4j.core.spec.WebhookExecuteSpec
import discord4j.discordjson.json.ApplicationCommandOptionData
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.rest.util.Image
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class SayAsWebhook : CommandHandler<ChatInputInteractionEvent> {

    override val name: String = "say-as-webhook"

    override val command: ApplicationCommandRequest
        get() = ApplicationCommandRequest.builder()
            .name(name)
            .description("Say something as a webhook. Use any :emoji: you want.")
            .addOption(
                ApplicationCommandOptionData.builder()
                    .name("message")
                    .description("Enter a string")
                    .type(ApplicationCommandOption.Type.STRING.value)
                    .required(true)
                    .build()
            ).build()

    override suspend fun handle(event: ChatInputInteractionEvent) {
        event.deferReply().awaitSingleOrNull()
        event.reply.flatMap { it.delete() }.awaitSingleOrNull()

        val channel = event.interaction.channel.awaitSingle()
        val member = event.interaction.member.orElseThrow()
        val content = getOptionValue(event, "message").asString()

        if (channel is TopLevelGuildMessageChannel) {
            val webhook = channel.createWebhook(member.displayName)
                .withAvatarOrNull(Image.ofUrl(member.avatarUrl).awaitSingle())
                .withReason("${member.tag} said something as a webhook")
                .awaitSingle()

            webhook.executeAndWait(WebhookExecuteSpec.builder().content(content).build())
                .flatMap { webhook.editMessage(it.id).withContentOrNull(content) } // emojis appear only after editing
                .onErrorResume { Mono.empty() }.awaitSingleOrNull()
            webhook.delete().awaitSingleOrNull()
        }
    }
}
