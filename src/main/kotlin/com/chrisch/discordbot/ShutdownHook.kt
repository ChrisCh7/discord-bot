package com.chrisch.discordbot

import discord4j.core.GatewayDiscordClient
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component

@Component
class ShutdownHook(private val client: GatewayDiscordClient) {
    @PreDestroy
    fun shutdown() {
        client.logout().block()
    }
}