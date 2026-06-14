package sk.tvhclient.shared

import sk.tvhclient.shared.api.ChannelRepository
import sk.tvhclient.shared.api.ConnectionResult
import sk.tvhclient.shared.api.TvhApi
import sk.tvhclient.shared.model.TvhServer
import sk.tvhclient.shared.stream.StreamUrlBuilder
import sk.tvhclient.shared.storage.ServerStore
import kotlin.coroutines.cancellation.CancellationException

/**
 * Vstupny bod pre obe platformy. Drzi store a tovarne na API/repository.
 */
object Tvh {

    val store: ServerStore by lazy { ServerStore() }

    @Throws(CancellationException::class)
    suspend fun testConnection(server: TvhServer): ConnectionResult {
        val api = TvhApi(server)
        try { return api.testConnection() } finally { api.close() }
    }

    fun newServerId(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return buildString { repeat(16) { append(chars.random()) } }
    }

    /** Vytvori API klienta pre dany server (volajuci je zodpovedny za close). */
    fun apiFor(server: TvhServer): TvhApi = TvhApi(server)

    /** Repository kanalov pre dany server. Picon URL bez creds (auth cez hlavicku). */
    fun channelRepository(server: TvhServer, api: TvhApi): ChannelRepository =
        ChannelRepository(
            api = api,
            piconUrlFor = { ch -> StreamUrlBuilder.piconUrlNoCreds(server, ch.iconPublicUrl) },
            nowSec = { currentTimeSeconds() }
        )

    fun liveUrl(server: TvhServer, channelUuid: String, channelTitle: String?, profile: String): String =
        StreamUrlBuilder.liveUrl(server, channelUuid, profile, channelTitle)

    /** Live URL bez creds (auth cez hlavicku) — pouziva profil zo servera. */
    fun liveUrlNoCreds(server: TvhServer, channelUuid: String, channelTitle: String?): String =
        StreamUrlBuilder.liveUrlNoCreds(server, channelUuid, server.profile.ifBlank { "pass" }, channelTitle)

    fun dvrUrl(server: TvhServer, dvrFileId: String): String =
        StreamUrlBuilder.dvrUrl(server, dvrFileId)
}

/** Aktualny cas v sekundach — expect/actual per platforma. */
expect fun currentTimeSeconds(): Long

/** Formatuje unix sekundy na "HH:MM" v lokalnom case. expect/actual. */
expect fun formatTimeHm(epochSec: Long): String

/** Formatuje unix sekundy na nazov dna "Pondelok 14.6." v lokalnom case. */
expect fun formatDayLabel(epochSec: Long): String
