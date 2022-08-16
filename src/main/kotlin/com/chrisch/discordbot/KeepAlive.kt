package com.chrisch.discordbot

import discord4j.core.GatewayDiscordClient
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class KeepAlive(private val client: GatewayDiscordClient) : ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        client.onDisconnect().block()
    }
}