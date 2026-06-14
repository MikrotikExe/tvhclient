package sk.tvhclient.shared.api

import sk.tvhclient.shared.model.Channel
import sk.tvhclient.shared.model.ChannelTag
import sk.tvhclient.shared.model.EpgEvent

/**
 * Kanal s now/next EPG a piconom — to co potrebuje UI zoznam.
 */
data class ChannelRow(
    val channel: Channel,
    val piconUrl: String?,
    val nowTitle: String?,
    val nowStart: Long,
    val nowStop: Long
)

/**
 * Skupina kanalov podla tagu (kategoria) pre TV riadky aj mobilny zoznam.
 */
data class ChannelCategory(
    val tag: ChannelTag?,        // null = "Vsetky" / bez tagu
    val rows: List<ChannelRow>
)

/**
 * Nacita kanaly + tagy + now EPG a poskladá kategorie. Cache 60s ako v
 * pluginnom get_channels (ExpiringLRUCache 60s).
 */
class ChannelRepository(
    private val api: TvhApi,
    private val piconUrlFor: (Channel) -> String?,
    private val nowSec: () -> Long,
    private val cacheTtlSec: Long = 60
) {
    private var cachedChannels: List<Channel>? = null
    private var cachedTags: List<ChannelTag>? = null
    private var cacheTs: Long = 0

    suspend fun load(force: Boolean = false): List<ChannelCategory> {
        val now = nowSec()
        if (force || cachedChannels == null || (now - cacheTs) >= cacheTtlSec) {
            cachedChannels = api.channels().filter { it.enabled }
            cachedTags = api.tags().filter { it.enabled }.sortedBy { it.index }
            cacheTs = now
        }
        val channels = cachedChannels ?: emptyList()
        val tags = cachedTags ?: emptyList()
        val epgNow = runCatching { api.epgNow() }.getOrDefault(emptyMap())

        fun rowOf(ch: Channel): ChannelRow {
            val ev: EpgEvent? = epgNow[ch.uuid]
            return ChannelRow(
                channel = ch,
                piconUrl = piconUrlFor(ch),
                nowTitle = ev?.title?.ifBlank { null },
                nowStart = ev?.start ?: 0,
                nowStop = ev?.stop ?: 0
            )
        }

        val categories = mutableListOf<ChannelCategory>()
        // Kategorie podla tagov (poradie podla tag.index ako na serveri)
        for (tag in tags) {
            val rows = channels
                .filter { tag.uuid in it.tags }
                .sortedWith(compareBy({ it.number ?: Int.MAX_VALUE }, { it.name.lowercase() }))
                .map(::rowOf)
            if (rows.isNotEmpty()) categories.add(ChannelCategory(tag, rows))
        }
        // Kanaly bez tagu — do "Ostatne", aby sa nestratili
        val tagged = channels.filter { it.tags.isNotEmpty() }.map { it.uuid }.toSet()
        val untagged = channels.filterNot { it.uuid in tagged }
        if (untagged.isNotEmpty()) {
            categories.add(ChannelCategory(
                tag = null,
                rows = untagged
                    .sortedWith(compareBy({ it.number ?: Int.MAX_VALUE }, { it.name.lowercase() }))
                    .map(::rowOf)
            ))
        }
        return categories
    }

    /** Plochy zoznam vsetkych kanalov (pre vyhladavanie). */
    suspend fun allRows(force: Boolean = false): List<ChannelRow> {
        load(force)
        val channels = cachedChannels ?: emptyList()
        val epgNow = runCatching { api.epgNow() }.getOrDefault(emptyMap())
        return channels
            .sortedWith(compareBy({ it.number ?: Int.MAX_VALUE }, { it.name.lowercase() }))
            .map { ch ->
                val ev = epgNow[ch.uuid]
                ChannelRow(ch, piconUrlFor(ch), ev?.title?.ifBlank { null },
                    ev?.start ?: 0, ev?.stop ?: 0)
            }
    }
}
