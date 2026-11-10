package com.chrisch.discordbot.event

import discord4j.core.event.domain.Event
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.GenericTypeResolver
import reactor.core.publisher.Mono

interface EventListener<T : Event> {

    @Suppress("UNCHECKED_CAST")
    val eventType: Class<T>
        get() = GenericTypeResolver.resolveTypeArgument(javaClass, EventListener::class.java) as Class<T>

    suspend fun execute(event: T)

    fun handleError(error: Throwable): Mono<Void> {
        log.error("Unable to process ${eventType.simpleName}", error)
        return Mono.empty()
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(EventListener::class.java)
    }
}
