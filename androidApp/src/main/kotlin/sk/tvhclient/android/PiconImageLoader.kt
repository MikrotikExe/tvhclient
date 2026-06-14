package sk.tvhclient.android

import android.content.Context
import android.util.Base64
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import sk.tvhclient.shared.model.TvhServer

/**
 * Coil ImageLoader pre picony. Auth ide cez Authorization: Basic hlavicku
 * (OkHttp neposle userinfo z URL). Disk cache 50 MB + memory cache —
 * picony sa stahuju lazily ako sa scrolluje, ziadny upfront burst (na
 * rozdiel od E2 boxu kde plugin riesil OOM cez early-abort).
 *
 * Pozn.: pre digest-only servery treba Authenticator (M3 polish). M1 sa
 * pripojil so stock auth, takze Basic hlavicka pokryva tvoj server.
 */
object PiconImageLoader {

    @Volatile private var loader: ImageLoader? = null
    @Volatile private var forServerId: String? = null

    fun get(context: Context, server: TvhServer?): ImageLoader {
        val existing = loader
        if (existing != null && forServerId == server?.id) return existing
        synchronized(this) {
            val again = loader
            if (again != null && forServerId == server?.id) return again
            val built = build(context.applicationContext, server)
            loader = built
            forServerId = server?.id
            return built
        }
    }

    private fun build(context: Context, server: TvhServer?): ImageLoader {
        val authHeader: String? = if (server != null && server.username.isNotEmpty()) {
            val raw = "${server.username}:${server.password}"
            "Basic " + Base64.encodeToString(raw.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        } else null

        val ok = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val req = chain.request().newBuilder().apply {
                    if (authHeader != null) header("Authorization", authHeader)
                }.build()
                chain.proceed(req)
            })
            .build()

        return ImageLoader.Builder(context)
            .okHttpClient(ok)
            .memoryCache {
                MemoryCache.Builder(context).maxSizePercent(0.15).build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("picons"))
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .build()
    }
}
