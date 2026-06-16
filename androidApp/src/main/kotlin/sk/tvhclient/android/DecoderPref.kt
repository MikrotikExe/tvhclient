package sk.tvhclient.android

import android.content.Context

/**
 * Vynutenie softveroveho dekodovania videa. Lacne Android boxy mavaju chybny
 * HW dekoder (MediaCodec) -> zelena blikajuca obrazovka. Zapnutim sa video
 * dekoduje softverovo (pomalsie, ale ide). Default = vypnute (HW dekoder).
 * Ulozene globalne v SharedPreferences.
 */
object DecoderPref {
    private const val PREFS = "app_prefs"
    private const val KEY = "force_sw_decode"

    fun get(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY, false)

    fun set(context: Context, forceSoftware: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY, forceSoftware).apply()
    }
}
