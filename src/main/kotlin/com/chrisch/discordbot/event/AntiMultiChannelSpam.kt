package com.chrisch.discordbot.event

import com.chrisch.discordbot.config.Config
import com.chrisch.discordbot.util.Limit
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.channel.TopLevelGuildMessageChannel
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
class AntiMultiChannelSpam(private val config: Config) : EventListener<MessageCreateEvent> {

    private data class UserMessage(
        val message: String,
        val messageId: Snowflake,
        val channelId: Snowflake,
        val timestamp: Instant,
        var deleted: Boolean
    )

    private val userMessages: ConcurrentHashMap<Snowflake, List<UserMessage>> = ConcurrentHashMap()

    override suspend fun execute(event: MessageCreateEvent) {
        val message = event.message

        if (message.author.map { it.isBot }.orElse(true) || message.guildId.isEmpty) {
            return
        }

        val authorId = message.author.orElseThrow().id
        val cutoff = message.timestamp.minus(WINDOW)

        userMessages.values.removeIf { it.last().timestamp < cutoff }
        val recentMessages = userMessages.merge(
            authorId,
            listOf(UserMessage(message.content, message.id, message.channelId, message.timestamp, false))
        ) { old, new -> old.filter { it.timestamp >= cutoff } + new }!!

        if (recentMessages.distinctBy { it.message }.size == 1 &&
            recentMessages.distinctBy { it.channelId }.size >= THRESHOLD
        ) {
            val authorMember = message.authorAsMember.awaitSingle()
            val guild = message.guild.awaitSingle()

            if (authorMember.communicationDisabledUntil.isEmpty) {
                authorMember.edit().withCommunicationDisabledUntilOrNull(Instant.now().plus(Duration.ofDays(27)))
                    .withReason("multi-channel spam").awaitSingle()
            }

            val purgeAlreadyStarted = recentMessages.any { it.deleted }

            recentMessages.forEach { userMessage ->
                if (!userMessage.deleted) {
                    guild.getChannelById(userMessage.channelId)
                        .ofType(TopLevelGuildMessageChannel::class.java)
                        .flatMap { it.getMessageById(userMessage.messageId) }
                        .flatMap { it.delete() }
                        .onErrorResume { Mono.empty() }.awaitSingleOrNull()
                    userMessage.deleted = true
                }
            }

            if (!purgeAlreadyStarted) {
                guild.getChannelById(Snowflake.of(config.reportChannelId))
                    .ofType(TopLevelGuildMessageChannel::class.java)
                    .flatMap {
                        it.createMessage(
                            "User: ${message.author.orElseThrow().tag} (${message.author.orElseThrow().id.asString()})\n" +
                                    "Reason: multi-channel spam\n" +
                                    "Proof:\n" +
                                    "```\n${message.content.take(Limit.MESSAGE_QUOTE)}\n```" +
                                    "Action took: 27 days timeout"
                        )
                    }.onErrorResume { Mono.empty() }.awaitSingleOrNull()
            }
        }
    }

    companion object {
        private const val THRESHOLD = 5
        private val WINDOW = Duration.ofSeconds(10)
    }
}