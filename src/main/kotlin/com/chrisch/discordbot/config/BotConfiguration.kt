package com.chrisch.discordbot.config

import com.chrisch.discordbot.event.EventListener
import com.chrisch.discordbot.util.EmojiStore
import discord4j.core.DiscordClientBuilder
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.EventDispatcher
import discord4j.core.event.domain.Event
import discord4j.gateway.intent.Intent
import discord4j.gateway.intent.IntentSet
import discord4j.rest.RestClient
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Flux
import java.time.Duration

@Configuration
class BotConfiguration(private val emojiStore: EmojiStore) {

    @Value("\${TOKEN}")
    private val token: String = ""

    private val log = LoggerFactory.getLogger(BotConfiguration::class.java)

    @Bean
    fun <T : Event> gatewayDiscordClient(eventListeners: List<EventListener<T>>): GatewayDiscordClient? {
        return DiscordClientBuilder.create(token)
            .build()
            .gateway()
            .withEventDispatcher { eventDispatcher -> subscribeToEvents(eventDispatcher, eventListeners) }
            .setEnabledIntents(IntentSet.of(Intent.GUILDS, Intent.GUILD_MESSAGES, Intent.GUILD_MESSAGE_REACTIONS))
            .login()
            .delayElement(Duration.ofSeconds(10))
            .flatMap { client ->
                client.guilds
                    .flatMap { it.emojis }
                    .filter { it.isAvailable }
                    .doOnNext { guildEmoji -> emojiStore.emojis[guildEmoji.name] = guildEmoji.asFormat() }
                    .count()
                    .doOnNext { count -> log.info("Cached $count emojis successfully.") }
                    .thenReturn(client)
            }
            .block()
    }

    private fun <T : Event> subscribeToEvents(
        eventDispatcher: EventDispatcher,
        eventListeners: List<EventListener<T>>
    ): Flux<Void> {
        return Flux.merge(eventListeners.stream()
            .map { listener ->
                eventDispatcher.on(listener.eventType)
                    .flatMap { mono { listener.execute(it) }.then() }
                    .onErrorResume(listener::handleError)
            }
            .toList()
        )
    }

    @Bean
    fun discordRestClient(client: GatewayDiscordClient): RestClient {
        return client.restClient
    }
}