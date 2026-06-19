package sk.tvhclient.android

import android.content.Context

/**
 * Timeshift v prehravaci (pauza/pretacanie zivej TV cez HTSP). Predvolene zapnute.
 * Toto je len uzivatelska volba — realne sa timeshift zapne iba ak ho podporuje aj
 * server (HtspData.timeshiftAvailable: HTSP port dostupny + capability "timeshift").
 * Ulozene globalne v SharedPreferences.
 */
object TimeshiftPref {
    private const val PREFS = "app_prefs"
    private const val KEY = "timeshift_enabled"

    fun get(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY, true)

    fun set(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY, enabled).apply()
    }
}
