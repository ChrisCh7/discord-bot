package com.chrisch.discordbot.command

import com.chrisch.discordbot.util.Utils.getOptionValue
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData
import discord4j.discordjson.json.ApplicationCommandOptionData
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.rest.util.Color
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class Rule : CommandHandler<ChatInputInteractionEvent> {

    override val name: String = "rule"

    override val command: ApplicationCommandRequest
        get() = ApplicationCommandRequest.builder()
            .name(name)
            .description("Show a rule.")
            .addOption(
                ApplicationCommandOptionData.builder()
                    .name("rule_number")
                    .description("Choose a number")
                    .type(ApplicationCommandOption.Type.INTEGER.value)
                    .required(true)
                    .addAllChoices(getRuleChoices())
                    .build()
            ).build()

    override fun handle(event: ChatInputInteractionEvent): Mono<Void> {
        val embed = EmbedCreateSpec.builder()
            .color(Color.GREEN)
            .title("Rule ${getOptionValue(event, "rule_number").asLong()}")
            .description("${rules[getOptionValue(event, "rule_number").asLong().toInt() - 1]}\n\n<#745246003275497543>")
            .build()

        return event.reply(InteractionApplicationCommandCallbackSpec.builder().addEmbed(embed).build())
    }

    fun getRuleChoices(): List<ApplicationCommandOptionChoiceData> {
        return (ruleChoices.indices).map { i ->
            ApplicationCommandOptionChoiceData.builder().name(ruleChoices[i]).value(i + 1).build()
        }
    }

    companion object {
        private val ruleChoices = listOf(
            "1. - No Spam",
            "2. - Nothing against Discord ToS",
            "3. - No spoon feeding",
            "4. - Ask SPECIFIC questions",
            "5. - DM advertising is not allowed",
            "6. - Don't scam people",
            "7. - Discussion of cheating or automation of games is not allowed",
            "8. - If someone asks an easily googleable question",
            "9. - Ear rape memes are not ok",
            "10. - Flashing light GIFs/videos are not allowed",
            "11. -  No NSFW",
            "12. - No harassment, even if it’s in DMs"
        )

        private val rules = listOf(
            "- No Spam",
            "- Nothing against Discord ToS (including racism etc, if you think you shouldn't post it then don't)",
            "- No spoon feeding\nDon't just give answers to people, help them realise their mistakes instead",
            "- Ask SPECIFIC questions, don't expect help if you say \"It doesn't work\", \"Why isn't it working\", \"can you fix it for me\"\n" +
                    "Instead, describe the problem(s), the steps you tried to fix it, the objective etc, don't make people dig information out of you",
            "- DM advertising is not allowed",
            "- Don't scam people",
            "- Discussion of cheating or automation of games is not allowed",
            "- If someone asks an easily googleable question like a \"pip not recognized as an internal command\" or \"Pillow package missing\"" +
                    " don't tell them the answer, use the site https://letmegooglethat.com/ to show them how to google it themselves",
            "- Ear rape memes are not ok and you will be muted*\n* punishment not limited to being muted",
            "- Flashing light GIFs/videos are not allowed",
            "- No NSFW, unless it's in <#873310203930611772>",
            "- No harassment, even if it’s in DMs"
        )
    }
}
