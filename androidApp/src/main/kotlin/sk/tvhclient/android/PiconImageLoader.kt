package sk.tvhclient.android

import android.content.Context
import android.util.Base64
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import sk.tvhclient.shared.model.TvhServer
import sk.tvhclient.shared.net.DigestAuthenticator

/**
 * Coil ImageLoader pre picony. Auth: Basic preemptivne (rychla cesta pre
 * basic/auto servery) + DigestAuthenticator na 401 challenge, takze picony
 * idu aj z digest-only servera v defaultnom rezime. Disk cache 50 MB + memory
 * cache — picony sa stahuju lazily ako sa scrolluje, ziadny upfront burst.
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

    /** M272: rucne vycistenie picon cache (memory + disk) — pre „Obnovit zoznam" v nastaveniach. */
    fun clearCache(context: Context, server: TvhServer?) {
        val il = get(context, server)
        runCatching { il.memoryCache?.clear() }
        runCatching { il.diskCache?.clear() }
    }

    private fun build(context: Context, server: TvhServer?): ImageLoader {
        val hasCreds = server != null && server.username.isNotEmpty()
        // Basic posleme preemptivne len ked nie je vynuteny digest (setri roundtrip
        // na basic/auto serveroch); pri digest-only ho vyriesi Authenticator nizsie.
        val preemptiveBasic: String? = if (hasCreds && server!!.authMode != "digest") {
            val raw = "${server.username}:${server.password}"
            "Basic " + Base64.encodeToString(raw.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        } else null

        val builder = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val req = chain.request().newBuilder().apply {
                    if (preemptiveBasic != null) header("Authorization", preemptiveBasic)
                }.build()
                chain.proceed(req)
            })

        // Digest (a Basic fallback) cez 401 challenge — aby picony isli aj z
        // digest-only servera, nie len ked je vynuteny Basic.
        if (hasCreds && server!!.authMode != "none") {
            builder.authenticator(DigestAuthenticator(server.username, server.password))
        }

        // M269: viac subeznych pozadovaniek na host — pri otvoreni zoznamu sa
        // davka piconov stiahne paralelnejsie (default OkHttp je len 5/host).
        val dispatcher = Dispatcher().apply {
            maxRequests = 24
            maxRequestsPerHost = 12
        }

        val ok = builder.dispatcher(dispatcher).build()

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

    /**
     * M269: predbezne stiahnutie piconov na disk (napr. po starte prehravaca alebo
     * pri nacitani zoznamu kanalov), aby pri scrollovani isli z cache, nie zo siete.
     * Pouzivame LEN disk cache — memory cache zamerne vypnuta, aby sme pri davke
     * desiatok piconov nezaplnili RAM bitmapmi; samotne zobrazenie si potom dekoduje
     * z disku na svoj rozmer a ulozi do memory. null/prazdne URL preskakujeme.
     */
    fun prefetch(context: Context, server: TvhServer?, urls: List<String?>) {
        val clean = urls.asSequence().filterNotNull().filter { it.isNotBlank() }.distinct().toList()
        if (clean.isEmpty()) return
        val il = get(context, server)
        val app = context.applicationContext
        for (u in clean) {
            val req = ImageRequest.Builder(app)
                .data(u)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
            runCatching { il.enqueue(req) }
        }
    }
}
