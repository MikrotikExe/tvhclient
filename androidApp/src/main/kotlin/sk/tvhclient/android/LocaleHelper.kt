package sk.tvhclient.android

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Vyber jazyka appky nezavisle od systemu (Systém/SK/CZ/EN).
 * Ulozi sa do SharedPreferences a aplikuje cez createConfigurationContext
 * v attachBaseContext kazdej aktivity. Hodnota "" = systemovy jazyk.
 */
object LocaleHelper {
    private const val PREFS = "app_prefs"
    private const val KEY_LANG = "app_lang"

    /** "" = system, inak "sk"/"cs"/"en". */
    fun getLang(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANG, "") ?: ""
    }

    fun setLang(context: Context, lang: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANG, lang).apply()
    }

    /** Obali kontext zvolenym jazykom (ak nie je systemovy). */
    fun wrap(context: Context): Context {
        val lang = getLang(context)
        if (lang.isBlank()) return context
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
