package com.chrisch.discordbot.event

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.channel.TopLevelGuildMessageChannel
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Service
class AntiMultiChannelSpam : EventListener<MessageCreateEvent> {

    @Value("\${MUTED_ROLE_ID}")
    private val mutedRoleId: String = ""

    @Value("\${REPORTS_CHANNEL_ID}")
    private val reportChannelId: String = ""

    private data class UserMessage(
        val message: String,
        val messageId: Snowflake,
        val channelId: Snowflake,
        val timestamp: Instant,
        var deleted: Boolean
    )

    private val userMessages: ConcurrentHashMap<Snowflake, CopyOnWriteArrayList<UserMessage>> = ConcurrentHashMap()

    override val eventType: Class<MessageCreateEvent> = MessageCreateEvent::class.java

    override suspend fun execute(event: MessageCreateEvent) {
        val message = event.message

        if (message.author.map { it.isBot }.orElse(true) || message.guildId.isEmpty) {
            return
        }

        val authorId = message.author.orElseThrow().id
        userMessages.computeIfAbsent(authorId) { CopyOnWriteArrayList() }

        userMessages[authorId]!!.add(
            UserMessage(
                message.content,
                message.id,
                message.channelId,
                message.timestamp,
                false
            )
        )

        if (userMessages[authorId]!!.size < THRESHOLD) {
            return
        }

        val distinctMessages = userMessages[authorId]!!.stream().map { it.message }.distinct().toList()
        val distinctChannels = userMessages[authorId]!!.stream().map { it.channelId }.distinct().toList()

        if (distinctMessages.size == 1 && distinctChannels.size == THRESHOLD &&
            userMessages[authorId]!!.last().timestamp.epochSecond - userMessages[authorId]!!.first().timestamp.epochSecond <= THRESHOLD * 2
        ) {
            val authorMember = message.authorAsMember.awaitSingle()
            val guild = message.guild.awaitSingle()

            if (!authorMember.roleIds.contains(Snowflake.of(mutedRoleId))) {
                authorMember.addRole(Snowflake.of(mutedRoleId), "multi-channel spam").awaitSingleOrNull()
            }

            val purgeAlreadyStarted =
                userMessages[authorId]!!.stream().filter { it.deleted }.toList().isNotEmpty()

            userMessages[authorId]!!.forEach { userMessage ->
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
                guild.getChannelById(Snowflake.of(reportChannelId))
                    .ofType(TopLevelGuildMessageChannel::class.java)
                    .flatMap {
                        it.createMessage(
                            "User: ${message.author.orElseThrow().tag} (${message.author.orElseThrow().id.asString()})\n" +
                                    "Reason: multi-channel spam\n" +
                                    "Proof:\n" +
                                    "```\n${message.content}\n```" +
                                    "Action took: muted"
                        )
                    }.onErrorResume { Mono.empty() }.awaitSingleOrNull()
            }
        }

        userMessages[authorId]!!.removeFirst()
    }

    companion object {
        private const val THRESHOLD = 5
    }
}