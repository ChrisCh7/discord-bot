package com.chrisch.discordbot.util

import org.springframework.stereotype.Component

@Component
class EmojiStore {
    val emojis: MutableMap<String, String> = HashMap()
}