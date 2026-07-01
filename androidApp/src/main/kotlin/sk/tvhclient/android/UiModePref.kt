package sk.tvhclient.android

import android.content.Context

/**
 * Rezim rozhrania na TV: klasicky launcher (dlazdice) alebo moderny (hero
 * s aktualnym programom + rady kariet). Default = klasicky, aby existujuci
 * pouzivatelia po update nevideli ziadnu zmenu. Ulozene v SharedPreferences.
 */
object UiModePref {
    private const val PREFS = "app_prefs"
    private const val KEY = "ui_mode"

    const val CLASSIC = "classic"
    const val MODERN = "modern"

    val options = listOf(CLASSIC, MODERN)

    fun get(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, CLASSIC) ?: CLASSIC

    fun set(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, value).apply()
    }
}
