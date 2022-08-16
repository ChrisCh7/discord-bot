package com.chrisch.discordbot

import discord4j.core.GatewayDiscordClient
import org.springframework.stereotype.Component
import javax.annotation.PreDestroy

@Component
class ShutdownHook(private val client: GatewayDiscordClient) {
    @PreDestroy
    fun shutdown() {
        client.logout().subscribe()
    }
}