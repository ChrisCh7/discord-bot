package com.chrisch.discordbot.event

import com.chrisch.discordbot.config.Config
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.channel.TopLevelGuildMessageChannel
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class AntiSpam(private val config: Config) : EventListener<MessageCreateEvent> {

    override val eventType: Class<MessageCreateEvent> = MessageCreateEvent::class.java

    override suspend fun execute(event: MessageCreateEvent) {
        val message = event.message

        if (message.author.map { it.isBot }.orElse(true) ||
            message.channelId != Snowflake.of(config.antiSpamChannelId)
        ) {
            return
        }

        message.authorAsMember.awaitSingle()
            .ban()
            .withDeleteMessageSeconds(60 * 60)
            .withReason("Member posted in anti-spam channel")
            .awaitSingleOrNull()

        message.guild.awaitSingle()
            .getChannelById(Snowflake.of(config.reportChannelId))
            .ofType(TopLevelGuildMessageChannel::class.java)
            .flatMap {
                it.createMessage(
                    "User: ${message.author.orElseThrow().tag} (${message.author.orElseThrow().id.asString()})\n" +
                            "Reason: posted in <#${config.antiSpamChannelId}> (anti-spam trap)\n" +
                            "Proof:\n" +
                            "```\n${message.content}\n```" +
                            "Action took: ban + delete message history previous hour"
                )
            }.onErrorResume { Mono.empty() }.awaitSingleOrNull()
    }
}
