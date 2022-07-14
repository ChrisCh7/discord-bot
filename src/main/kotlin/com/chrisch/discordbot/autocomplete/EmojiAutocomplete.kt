package com.chrisch.discordbot.autocomplete

import com.chrisch.discordbot.util.EmojiStore
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class EmojiAutocomplete(private val emojiStore: EmojiStore) : AutocompleteHandler<ChatInputAutoCompleteEvent> {
    override val name: String = "emoji"

    override fun handle(event: ChatInputAutoCompleteEvent): Mono<Void> {
        val currentlyTypedOption = event.focusedOption.value.orElseThrow().asString()
        return event.respondWithSuggestions(getEmojiAutocompleteOptions(currentlyTypedOption))
    }

    private fun getEmojiAutocompleteOptions(name: String): List<ApplicationCommandOptionChoiceData> {
        if (name.isBlank()) return listOf()

        val emojiMatchesStartsWith = emojiStore.emojis.filterKeys { it.lowercase().startsWith(name.lowercase()) }
        val emojiMatchesContains = emojiStore.emojis.filterKeys { it.lowercase().contains(name.lowercase()) }
        val emojiMatches = emojiMatchesStartsWith + emojiMatchesContains

        val options = mutableListOf<ApplicationCommandOptionChoiceData>()

        if (name.equals("all", true)) {
            options.add(ApplicationCommandOptionChoiceData.builder().name("all").value("all").build())
        }

        if (emojiMatches.isEmpty()) {
            return options
        } else {
            for (emojiName in emojiMatches.keys) {
                options.add(ApplicationCommandOptionChoiceData.builder().name(emojiName).value(emojiName).build())
            }
            return options.take(25)
        }
    }
}
