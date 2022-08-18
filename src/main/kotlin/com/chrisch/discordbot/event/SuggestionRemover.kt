package com.chrisch.discordbot.event

import com.chrisch.discordbot.util.CustomColor
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.ReactionAddEvent
import discord4j.core.`object`.entity.channel.TopLevelGuildMessageChannel
import discord4j.core.spec.EmbedCreateSpec
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class SuggestionRemover : EventListener<ReactionAddEvent> {

    @Value("\${SUGGESTIONS_CHANNEL_ID}")
    private val suggestionsChannelId: String = ""

    @Value("\${LOGS_CHANNEL_ID}")
    private val logsChannelId: String = ""

    override val eventType: Class<ReactionAddEvent> = ReactionAddEvent::class.java

    override suspend fun execute(event: ReactionAddEvent) {
        if (event.member.isEmpty) return

        val member = event.member.orElseThrow()

        if (member.isBot || event.channelId != Snowflake.of(suggestionsChannelId)) {
            return
        }

        val message = event.message.awaitSingle()
        val reactions = message.reactions

        val upvoteReactions = reactions.filter { it.emoji.asEmojiData().name().orElseThrow() == "upvote" }
        val downvoteReactions = reactions.filter { it.emoji.asEmojiData().name().orElseThrow() == "downvote" }

        val nrUpvotes = upvoteReactions.firstOrNull()?.count ?: 0
        val nrDownvotes = downvoteReactions.firstOrNull()?.count ?: 0

        val diff = nrDownvotes - nrUpvotes

        if (diff >= voteDiffLimit) {
            message.delete().awaitSingleOrNull()

            val embed = EmbedCreateSpec.builder()
                .color(CustomColor.RED)
                .title("Suggestion")
                .addField("Author", message.author.orElseThrow().tag, false)
                .addField("Message", message.content, false)
                .build()

            message.guild
                .flatMap { it.getChannelById(Snowflake.of(logsChannelId)) }
                .ofType(TopLevelGuildMessageChannel::class.java)
                .flatMap {
                    it.createMessage(
                        "Deleted suggestion because downvotes ($nrDownvotes)" +
                                " - upvotes ($nrUpvotes) >= $voteDiffLimit"
                    ).withEmbeds(embed)
                }.awaitSingle()
        }
    }

    companion object {
        private const val voteDiffLimit = 5
    }
}
