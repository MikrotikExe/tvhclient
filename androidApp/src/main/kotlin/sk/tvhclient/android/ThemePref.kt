package sk.tvhclient.android

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * Rezim temy aplikacie.
 *  - AUTO: podla systemu (svetly/tmavy podla nastavenia zariadenia) — predvolene
 *  - LIGHT: vzdy svetla
 *  - DARK: vzdy tmava
 * Drzime aj zivy stav (MutableState), aby sa tema prepla okamzite po zmene v nastaveniach,
 * bez nutnosti restartu. Ulozene globalne v SharedPreferences.
 */
object ThemePref {
    private const val PREFS = "app_prefs"
    private const val KEY = "app_theme"

    const val AUTO = "auto"
    const val LIGHT = "light"
    const val DARK = "dark"

    val options = listOf(AUTO, LIGHT, DARK)

    private var state: MutableState<String>? = null

    private fun load(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, AUTO) ?: AUTO

    /** Zivy stav rezimu — citanim .value v @Composable sa tema obnovi pri zmene. */
    fun stateOf(context: Context): MutableState<String> =
        state ?: mutableStateOf(load(context)).also { state = it }

    fun get(context: Context): String = stateOf(context).value

    fun set(context: Context, value: String) {
        stateOf(context).value = value
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, value).apply()
    }
}
