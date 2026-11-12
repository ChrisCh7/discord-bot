package com.chrisch.discordbot.util

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class EmojiStore {
    val emojis: MutableMap<String, String> = ConcurrentHashMap()
}