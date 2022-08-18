package com.chrisch.discordbot.event

import discord4j.core.event.domain.lifecycle.ReadyEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class Ready : EventListener<ReadyEvent> {

    private val log = LoggerFactory.getLogger(javaClass)

    override val eventType: Class<ReadyEvent> = ReadyEvent::class.java

    override suspend fun execute(event: ReadyEvent) {
        log.info("Ready! Logged in as ${event.self.tag}")
    }
}