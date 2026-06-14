package sk.tvhclient.shared.picon

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Negativna cache pre picony co vratili 404, plus early-abort prah.
 * Prenos logiky z pluginu (_picons.py, FIX 0.48b + FIX 0.71.1):
 *
 *  - 404 URL si pamatame ttlSec (1h) a medzitym ich neskusame znova
 *    (broken upstream icon / lazy-load fail v TVH).
 *  - earlyAbortThreshold: ak pride tolko 404 za sebou a 0 uspechov,
 *    server picony zjavne nema (FIX 0.71.1 — na slabom boxe inak OOM).
 *
 * Pouziva coroutines Mutex (ziadna extra zavislost). Volane z repository
 * v coroutine kontexte.
 */
class Picon404Cache(
    private val ttlSec: Long = 3600,
    val earlyAbortThreshold: Int = 30,
    private val nowSec: () -> Long
) {
    private val mutex = Mutex()
    private val cache = HashMap<String, Long>()

    suspend fun isCached(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        mutex.withLock {
            val ts = cache[url] ?: return false
            if (nowSec() - ts >= ttlSec) {
                cache.remove(url)
                return false
            }
            return true
        }
    }

    suspend fun mark404(url: String?) {
        if (url.isNullOrEmpty()) return
        mutex.withLock { cache[url] = nowSec() }
    }

    suspend fun count(): Int = mutex.withLock { cache.size }

    suspend fun clear() = mutex.withLock { cache.clear() }
}
