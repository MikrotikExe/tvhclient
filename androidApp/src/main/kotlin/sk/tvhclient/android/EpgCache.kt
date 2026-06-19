package sk.tvhclient.android

import android.content.Context
import sk.tvhclient.shared.model.EpgEvent
import sk.tvhclient.shared.storage.EpgCacheCodec
import java.io.File

/**
 * Diskovy cache EPG na Androide. Per server jeden JSON subor v internom ulozisku.
 * Tvheadend stare relacie z EPG postupne maze; tymto si appka pamata uplynule dni
 * (kolko presne urcuje EpgRangePref.daysBack).
 */
object EpgCache {

    private fun file(ctx: Context, serverId: String): File {
        val dir = File(ctx.filesDir, "epg_cache")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "epg_${serverId}.json")
    }

    fun load(ctx: Context, serverId: String, nowSec: Long, daysBack: Int): Map<String, List<EpgEvent>> {
        return try {
            val f = file(ctx, serverId)
            if (!f.exists()) emptyMap()
            else EpgCacheCodec.prune(EpgCacheCodec.decode(f.readText()), nowSec, daysBack)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun save(ctx: Context, serverId: String, data: Map<String, List<EpgEvent>>, nowSec: Long, daysBack: Int) {
        try {
            val pruned = EpgCacheCodec.prune(data, nowSec, daysBack)
            file(ctx, serverId).writeText(EpgCacheCodec.encode(pruned))
        } catch (e: Exception) {
            // cache je len optimalizacia — zlyhanie zapisu ignorujeme
        }
    }
}
