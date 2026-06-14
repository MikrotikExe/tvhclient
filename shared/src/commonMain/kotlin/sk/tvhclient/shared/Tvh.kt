package sk.tvhclient.shared

import sk.tvhclient.shared.api.ConnectionResult
import sk.tvhclient.shared.api.TvhApi
import sk.tvhclient.shared.model.TvhServer
import sk.tvhclient.shared.storage.ServerStore
import kotlin.coroutines.cancellation.CancellationException

/**
 * Jednoduchy vstupny bod pre Swift stranu - Kotlin objekty s default
 * argumentmi sa zo Swiftu volaju neprijemne, toto to obide.
 */
object Tvh {

    val store: ServerStore by lazy { ServerStore() }

    @Throws(CancellationException::class)
    suspend fun testConnection(server: TvhServer): ConnectionResult {
        val api = TvhApi(server)
        try {
            return api.testConnection()
        } finally {
            api.close()
        }
    }

    fun newServerId(): String {
        // Jednoduche unikatne ID bez zavislosti na UUID kniznici
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return buildString {
            repeat(16) { append(chars.random()) }
        }
    }
}
