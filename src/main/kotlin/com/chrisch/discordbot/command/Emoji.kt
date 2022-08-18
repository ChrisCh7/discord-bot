package com.chrisch.discordbot.command

import com.chrisch.discordbot.util.CustomColor
import com.chrisch.discordbot.util.EmojiStore
import com.chrisch.discordbot.util.Utils.getOptionValue
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.core.`object`.entity.channel.TopLevelGuildMessageChannel
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec
import discord4j.core.spec.WebhookExecuteSpec
import discord4j.discordjson.json.ApplicationCommandOptionData
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.discordjson.possible.Possible
import discord4j.rest.util.Image
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.*

@Service
class Emoji(private val emojiStore: EmojiStore) : CommandHandler<ChatInputInteractionEvent> {

    override val name: String = "emoji"

    override val command: ApplicationCommandRequest
        get() = ApplicationCommandRequest.builder()
            .name(name)
            .description("Show an emoji.")
            .addOption(
                ApplicationCommandOptionData.builder()
                    .name("name")
                    .description("Enter a string")
                    .type(ApplicationCommandOption.Type.STRING.value)
                    .required(true)
                    .autocomplete(true)
                    .build()
            ).build()

    override suspend fun handle(event: ChatInputInteractionEvent) {
        val emojiName = getOptionValue(event, "name").asString()

        if (emojiName.equals("all", true)) {
            val embed = EmbedCreateSpec.builder()
                .color(CustomColor.GREEN)
                .title("${emojiStore.emojis.size} emojis")
                .description(emojiStore.emojis.values.joinToString(""))
                .build()

            event.reply(InteractionApplicationCommandCallbackSpec.builder().addEmbed(embed).build()).awaitSingleOrNull()
            return
        }

        val emojiMatches = getEmojiMatches(emojiName)

        if (emojiMatches.isEmpty()) {
            event.reply("Emoji not found!")
                .delayElement(Duration.ofSeconds(2))
                .then(event.deleteReply()).awaitSingleOrNull()
            return
        }

        if (emojiMatches.size > 1) {
            event.reply(
                "Emoji not found.\n" +
                        "Did you mean: ${emojiMatches.keys.joinToString(", ")}"
            ).then(Mono.just(1)).delayElement(Duration.ofSeconds(10))
                .then(event.deleteReply()).awaitSingleOrNull()
            return
        }

        val emoji = emojiMatches.entries.first().value

        val channel = event.interaction.channel.awaitSingle()
        val member = event.interaction.member.orElseThrow()

        event.deferReply().awaitSingleOrNull()
        event.deleteReply().awaitSingleOrNull()

        if (channel is TopLevelGuildMessageChannel) {
            val webhook = channel.createWebhook(member.displayName)
                .withAvatar(
                    Possible.of(
                        Optional.of(
                            Image.ofUrl(member.avatarUrl).awaitSingle()
                        )
                    )
                )
                .withReason("${member.tag} posted an emoji")
                .awaitSingle()

            webhook.executeAndWait(WebhookExecuteSpec.builder().content(emoji).build())
                .onErrorResume { Mono.empty() }.awaitSingleOrNull()
            webhook.delete().awaitSingleOrNull()
        }
    }

    private fun getEmojiMatches(name: String): Map<String, String> {
        val emojiMatches = emojiStore.emojis.filterKeys { emojiName -> emojiName.equals(name, true) }

        if (emojiMatches.isEmpty()) {
            return emojiStore.emojis.filterKeys { emojiName ->
                val toTake = minOf(3, emojiName.length, name.length)
                emojiName.substring(0, toTake).equals(name.substring(0, toTake), true)
            }
        } else if (emojiMatches.size > 1) {
            with(emojiMatches.entries.first()) {
                return mapOf(Pair(key, value))
            }
        } else {
            return emojiMatches
        }
    }
}
