package com.chrisch.discordbot

import com.chrisch.discordbot.command.CommandHandler
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent
import discord4j.rest.RestClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class CommandDeployer(
    private val client: RestClient,
    private val commandListeners: List<CommandHandler<out ApplicationCommandInteractionEvent>>
) : ApplicationRunner {

    @Value("\${GUILD_ID}")
    private val guildId: String = ""

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        client.applicationId
            .flatMap { applicationId ->
                client.applicationService
                    .bulkOverwriteGuildApplicationCommand(
                        applicationId!!, guildId.toLong(), commandListeners.stream()
                            .map { it.command }.toList()
                    ).count()
            }.doOnNext { count -> log.info("Successfully registered $count application commands.") }
            .doOnError { e -> log.error("Failed to register application commands.", e) }
            .subscribe()
    }
}
