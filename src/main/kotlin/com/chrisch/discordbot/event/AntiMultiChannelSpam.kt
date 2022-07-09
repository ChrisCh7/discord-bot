package com.chrisch.discordbot.event

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

@Service
class AntiMultiChannelSpam : EventListener<MessageCreateEvent> {

    @Value("\${MUTED_ROLE_ID}")
    private val mutedRoleId: String? = null

    @Value("\${REPORTS_CHANNEL_ID}")
    private val reportChannelId: String? = null

    private data class UserMessage(
        val message: String,
        val messageId: Snowflake,
        val channelId: Snowflake,
        val timestamp: Instant,
        var deleted: Boolean
    )

    private val userMessages: MutableMap<Snowflake, MutableList<UserMessage>> = HashMap()

    override val eventType: Class<MessageCreateEvent>
        get() = MessageCreateEvent::class.java

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        val message = event.message

        if (message.author.map { it.isBot }.orElse(true) || message.guildId.isEmpty) {
            return Mono.empty()
        }

        val authorId = message.author.orElseThrow().id
        userMessages.computeIfAbsent(authorId) { ArrayList() }

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
            return Mono.empty()
        }

        val distinctMessages = userMessages[authorId]!!.stream().map { it.message }.distinct().toList()
        val distinctChannels = userMessages[authorId]!!.stream().map { it.channelId }.distinct().toList()

        if (distinctMessages.size == 1 && distinctChannels.size == THRESHOLD &&
            userMessages[authorId]!![4].timestamp.epochSecond - userMessages[authorId]!![0].timestamp.epochSecond <= THRESHOLD * 2
        ) {
            val purgeAlreadyStarted = userMessages[authorId]!!.stream().filter { it.deleted }.toList().isNotEmpty()

            return message.authorAsMember
                .flatMapMany { it.roles }
                .filter { role -> role.id == Snowflake.of(mutedRoleId!!) }
                .count()
                .flatMap { count ->
                    if (count == 0L) {
                        message.authorAsMember
                            .flatMap { member -> member.addRole(Snowflake.of(mutedRoleId!!), "multi-channel spam") }
                    } else Mono.empty()
                }.thenMany(
                    Flux.fromIterable(userMessages[authorId]!!)
                ).flatMap { userMessage ->
                    if (!userMessage.deleted) {
                        return@flatMap message.guild
                            .flatMap { guild -> guild.getChannelById(userMessage.channelId) }
                            .flatMap { guildChannel ->
                                guildChannel.restChannel.message(userMessage.messageId).delete(null)
                            }
                            .doOnTerminate { userMessage.deleted = true }
                    } else {
                        return@flatMap Mono.empty()
                    }
                }.then(Mono.just(purgeAlreadyStarted))
                .flatMap { purgeStarted ->
                    if (purgeStarted) {
                        return@flatMap message.guild
                            .flatMap { guild -> guild.getChannelById(Snowflake.of(reportChannelId!!)) }
                            .flatMap { guildChannel ->
                                guildChannel.restChannel.createMessage(
                                    "User: ${message.author.orElseThrow().tag} (${message.author.orElseThrow().id.asString()})\n" +
                                            "Reason: multi-channel spam\n" +
                                            "Proof:\n" +
                                            "```\n${message.content}\n```" +
                                            "Action took: muted"
                                )
                            }.then()
                    } else {
                        return@flatMap Mono.empty()
                    }
                }.then(Mono.fromRunnable { userMessages[authorId]!!.removeAt(0) })
        }

        userMessages[authorId]!!.removeAt(0)

        return Mono.empty()
    }

    companion object {
        private const val THRESHOLD = 5
    }
}