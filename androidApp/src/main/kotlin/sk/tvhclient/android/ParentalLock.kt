package sk.tvhclient.android

import android.content.Context

/**
 * Rodicovsky zamok (PIN). Aktivuje ho uzivatel v nastaveniach. PIN su 4 cislice
 * (0000-9999). Zamykaju sa konkretne kanaly (per server) — najma 18+.
 * Po spravnom zadani PINu plati 5-minutove okno, pocas ktoreho sa PIN nepyta;
 * po jeho uplynuti sa pri dalsom prepnuti na zamknuty kanal / vstupe do
 * nastaveni vypyta znova. Stav okna je zdielany medzi zoznamom a prehravacom
 * cez SharedPreferences (rovnaky proces).
 */
object ParentalLock {
    private const val PREFS = "app_prefs"
    private const val KEY_ENABLED = "plock_enabled"
    private const val KEY_PIN = "plock_pin"
    private const val KEY_LOCKED = "plock_channels_" // + serverId -> Set<uuid>
    private const val KEY_UNTIL = "plock_unlocked_until"
    const val GRACE_MS = 5 * 60 * 1000L

    private fun p(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(c: Context) = p(c).getBoolean(KEY_ENABLED, false)
    fun setEnabled(c: Context, on: Boolean) = p(c).edit().putBoolean(KEY_ENABLED, on).apply()

    fun hasPin(c: Context) = !p(c).getString(KEY_PIN, "").isNullOrEmpty()
    fun setPin(c: Context, pin: String) = p(c).edit().putString(KEY_PIN, pin).apply()
    fun checkPin(c: Context, pin: String): Boolean = p(c).getString(KEY_PIN, "") == pin

    fun lockedSet(c: Context, serverId: String?): Set<String> {
        if (serverId == null) return emptySet()
        return p(c).getStringSet(KEY_LOCKED + serverId, emptySet()) ?: emptySet()
    }

    fun isChannelLocked(c: Context, serverId: String?, uuid: String?): Boolean {
        if (serverId == null || uuid == null) return false
        return lockedSet(c, serverId).contains(uuid)
    }

    fun setChannelLocked(c: Context, serverId: String?, uuid: String, locked: Boolean) {
        if (serverId == null) return
        val cur = HashSet(lockedSet(c, serverId))
        if (locked) cur.add(uuid) else cur.remove(uuid)
        p(c).edit().putStringSet(KEY_LOCKED + serverId, cur).apply()
    }

    fun isUnlocked(c: Context): Boolean =
        System.currentTimeMillis() < p(c).getLong(KEY_UNTIL, 0L)

    fun markUnlocked(c: Context) =
        p(c).edit().putLong(KEY_UNTIL, System.currentTimeMillis() + GRACE_MS).apply()

    fun relock(c: Context) = p(c).edit().putLong(KEY_UNTIL, 0L).apply()

    /** Treba teraz vypytat PIN? (zamok zapnuty, PIN nastaveny a nie sme v 5-min okne) */
    fun needsPin(c: Context): Boolean = isEnabled(c) && hasPin(c) && !isUnlocked(c)

    /** Treba PIN pre dany kanal? */
    fun channelNeedsPin(c: Context, serverId: String?, uuid: String?): Boolean =
        needsPin(c) && isChannelLocked(c, serverId, uuid)
}
