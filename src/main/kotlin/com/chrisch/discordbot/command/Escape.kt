package com.chrisch.discordbot.command

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.discordjson.json.ApplicationCommandRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
class Escape : CommandHandler<ChatInputInteractionEvent> {

    @Value("\${PRISONER_ROLE_ID}")
    private val prisonerRoleId: String = ""

    private val log = LoggerFactory.getLogger(javaClass)

    private val escapeAttempts: ConcurrentHashMap<Snowflake, Instant> = ConcurrentHashMap()

    override val name: String = "escape"

    override val command: ApplicationCommandRequest
        get() = ApplicationCommandRequest.builder()
            .name(name)
            .description("Have a chance to escape from gulag.")
            .build()

    override fun handle(event: ChatInputInteractionEvent): Mono<Void> {
        if (event.interaction.member.isEmpty) return Mono.empty()

        val member = event.interaction.member.orElseThrow()
        val userId = event.interaction.user.id

        if (!member.roleIds.contains(Snowflake.of(prisonerRoleId))) {
            return event.reply("You must be a prisoner to use this command.").withEphemeral(true)
        }

        val now = Instant.now()

        if (escapeAttempts.containsKey(userId)) {
            val elapsedTime = now.epochSecond - escapeAttempts[userId]!!.epochSecond
            if (elapsedTime < escapeCooldownSeconds) {
                return event.reply("Please try again <t:${now.epochSecond + (escapeCooldownSeconds - elapsedTime)}:R>")
            }
        }

        escapeAttempts[userId] = now

        if (0 == (0..(100 / percentageChanceOfEscaping)).random()) {
            return event.reply(
                "Congratulations! You managed to escape.\n" +
                        "Mods: Please use `?gulag ${userId.asString()}` to make Dyno aware of this."
            ).then(Mono.just(1))
                .delayElement(Duration.ofSeconds(2))
                .then(member.removeRole(Snowflake.of(prisonerRoleId), "Escaped from gulag"))
                .doOnError { log.error("Error removing prisoner role", it) }
                .onErrorResume { event.createFollowup("There was an error removing the prisoner role.").then() }
        }

        return event.reply("Oh no! You failed to escape.")
    }

    companion object {
        private val percentageChanceOfEscaping = 10
        private val escapeCooldownSeconds = 3600
    }
}
