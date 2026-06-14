package sk.tvhclient.shared.htsp

import sk.tvhclient.shared.model.Channel
import sk.tvhclient.shared.model.ChannelTag
import sk.tvhclient.shared.model.DvrEntry
import sk.tvhclient.shared.model.EpgEvent
import sk.tvhclient.shared.model.TvhServer

/**
 * HTSP dátový zdroj: pripojí sa cez 9982, stiahne metadáta (enableAsyncMetadata)
 * a namapuje HTSP polia na modely appky (rovnaké ako z HTTP /api). Mapovanie
 * prebraté z pluginu (_htsp_api.py). Jednoduchá TTL cache podľa servera, aby
 * sa nepripájalo pri každej karte.
 *
 * Streaming a picony ostávajú HTTP (HTSP tu rieši len dáta).
 */
object HtspData {
    private data class Cache(val ts: Long, val meta: HtspClient.Metadata, val withEpg: Boolean)
    private val cache = HashMap<String, Cache>()

    private fun longOf(m: Map<String, Any?>, key: String): Long? = (m[key] as? Long)
    private fun strOf(m: Map<String, Any?>, key: String): String = (m[key] as? String) ?: ""

    suspend fun metadata(server: TvhServer, withEpg: Boolean, nowSec: Long, epgMaxDays: Int = 1): HtspClient.Metadata {
        val key = server.id
        val ttl = if (withEpg) 600 else 120
        val c = cache[key]
        if (c != null && nowSec - c.ts < ttl && (!withEpg || c.withEpg)) {
            return c.meta
        }
        val client = HtspClient(server.host, server.htspPort, server.username, server.password)
        client.connect()
        val meta = try {
            client.fetchMetadata(withEpg = withEpg, epgMaxDays = epgMaxDays, nowSec = nowSec)
        } finally {
            client.close()
        }
        cache[key] = Cache(nowSec, meta, withEpg)
        return meta
    }

    /** now/next mapa: pre kazdy kanal aktualne beziaci program. Cez getEvents
     *  na jednom otvorenom spojeni — async dump je tu nepouzitelny, lebo
     *  posiela najprv tisice DVR zaznamov a eventy sa nestihnu. */
    suspend fun epgNowMap(server: TvhServer, nowSec: Long): Map<String, EpgEvent> {
        val meta = metadata(server, withEpg = false, nowSec = nowSec)
        val channelIds = meta.channels.mapNotNull { longOf(it, "channelId") }
        if (channelIds.isEmpty()) return emptyMap()
        val client = HtspClient(server.host, server.htspPort, server.username, server.password)
        client.connect()
        val out = HashMap<String, EpgEvent>()
        try {
            for (cid in channelIds) {
                val evs = try {
                    client.getEvents(cid, numFollowing = 2, maxTime = 0)
                } catch (e: Exception) { continue }
                val mapped = evs.mapNotNull { mapEvent(it) }
                val current = mapped.firstOrNull { it.start <= nowSec && nowSec < it.stop }
                    ?: mapped.firstOrNull()
                if (current != null) out[cid.toString()] = current
            }
        } finally {
            client.close()
        }
        return out
    }

    fun clear(serverId: String) { cache.remove(serverId) }

    // ---- mapovanie ----

    fun channels(meta: HtspClient.Metadata): List<Channel> =
        meta.channels.mapNotNull { ch ->
            val cid = longOf(ch, "channelId") ?: return@mapNotNull null
            @Suppress("UNCHECKED_CAST")
            val tagIds = (ch["tags"] as? List<Any?>)?.mapNotNull { (it as? Long)?.toString() } ?: emptyList()
            Channel(
                uuid = cid.toString(),
                name = strOf(ch, "channelName").ifBlank { cid.toString() },
                number = longOf(ch, "channelNumber")?.toInt(),
                iconPublicUrl = strOf(ch, "channelIcon").ifBlank { null },
                tags = tagIds,
                enabled = true
            )
        }

    fun tags(meta: HtspClient.Metadata): List<ChannelTag> =
        meta.tags.mapNotNull { t ->
            val tid = longOf(t, "tagId") ?: return@mapNotNull null
            ChannelTag(
                uuid = tid.toString(),
                name = strOf(t, "tagName").ifBlank { tid.toString() },
                index = longOf(t, "tagIndex")?.toInt() ?: 0,
                enabled = true
            )
        }

    /** Dokončené DVR nahrávky (state == "completed"). */
    fun dvrFinished(meta: HtspClient.Metadata): List<DvrEntry> =
        meta.dvr.mapNotNull { d ->
            val state = strOf(d, "state")
            if (state.isNotBlank() && state != "completed") return@mapNotNull null
            val id = longOf(d, "id") ?: return@mapNotNull null
            // /dvrfile potrebuje hex uuid; HTSP ho dáva v "uuid" (ak je), inak id
            val uuid = (d["uuid"] as? String)?.ifBlank { null } ?: id.toString()
            val start = longOf(d, "start") ?: 0
            val stop = longOf(d, "stop") ?: 0
            DvrEntry(
                uuid = uuid,
                dispTitle = strOf(d, "title"),
                dispSubtitle = strOf(d, "subtitle"),
                dispDescription = strOf(d, "description").ifBlank { strOf(d, "summary") },
                channelName = strOf(d, "channelName"),
                start = start,
                stop = stop,
                duration = if (stop > start) stop - start else 0,
                fileSize = longOf(d, "dataSize") ?: 0,
                status = state,
                contentType = longOf(d, "contentType")?.toInt() ?: 0
            )
        }

    /** Všetky EPG eventy (len ak meta načítané withEpg). */
    fun events(meta: HtspClient.Metadata): List<EpgEvent> =
        meta.events.mapNotNull { mapEvent(it) }

    private fun mapEvent(e: Map<String, Any?>): EpgEvent? {
        val cid = longOf(e, "channelId") ?: return null
        val ct = longOf(e, "contentType")?.toInt() ?: 0
        return EpgEvent(
            eventId = longOf(e, "eventId"),
            channelUuid = cid.toString(),
            channelName = "",
            start = longOf(e, "start") ?: 0,
            stop = longOf(e, "stop") ?: 0,
            title = strOf(e, "title"),
            subtitle = strOf(e, "subtitle"),
            summary = strOf(e, "summary"),
            description = strOf(e, "description"),
            genre = if (ct > 0) listOf(ct) else emptyList(),
            ageRating = longOf(e, "ageRating")?.toInt() ?: 0,
            episodeOnscreen = strOf(e, "episodeOnscreen"),
            nextEventId = longOf(e, "nextEventId")
        )
    }

    /** Program pre kanal cez HTSP getEvents (rychle, per-kanal). */
    suspend fun epgForChannel(server: TvhServer, channelId: String, nowSec: Long): List<EpgEvent> {
        val cid = channelId.toLongOrNull() ?: return emptyList()
        val client = HtspClient(server.host, server.htspPort, server.username, server.password)
        client.connect()
        return try {
            client.getEvents(cid, numFollowing = 80, maxTime = nowSec + 3 * 86400)
                .mapNotNull { mapEvent(it) }
                .sortedBy { it.start }
        } finally {
            client.close()
        }
    }
}
