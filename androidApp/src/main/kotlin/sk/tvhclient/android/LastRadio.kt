package sk.tvhclient.android

import android.content.Context

/**
 * Zapamatanie posledne zvolenej rozhlasovej stanice (per server) — oddelene od
 * LastChannel (TV), aby sa navzajom neprepisovali. Launcher na TV vie podla toho
 * pustit rovno poslednu stanicu, rovnako ako pri TV kanaloch.
 */
object LastRadio {
    private const val PREFS = "app_prefs"
    private const val KEY = "last_radio_uuid_"

    fun get(context: Context, serverId: String?): String? {
        if (serverId == null) return null
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY + serverId, null)
    }

    fun set(context: Context, serverId: String?, uuid: String?) {
        if (serverId == null || uuid == null) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY + serverId, uuid).apply()
    }
}
