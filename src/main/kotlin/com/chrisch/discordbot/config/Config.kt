package com.chrisch.discordbot.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties
data class Config(
    val token: String,
    val guildId: String,
    val suggestionsChannelId: String,
    val logsChannelId: String,
    val logsReactionsChannelId: String,
    val trackedChannelIds: List<String>,
    val trackedMessageIds: List<String>,
    val reportChannelId: String,
    val countingChannelId: String,
    val antiSpamChannelId: String,
)
