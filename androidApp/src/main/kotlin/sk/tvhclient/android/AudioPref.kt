package sk.tvhclient.android

import android.content.Context

/**
 * Preferencia audio stop: az 3 jazyky v poradi priority. Prehravac vyberie
 * prvy dostupny (napr. SK -> CZ -> EN). Prazdny zoznam = automaticky
 * (necha vyber na serveri/VLC). Ulozene v SharedPreferences ako CSV kodov.
 */
object AudioPref {
    private const val PREFS = "app_prefs"
    private const val KEY = "audio_langs"

    /** Default: Slovenčina, Čeština, English. Vracia kody v poradi (moze
     *  obsahovat prazdne sloty — prehravac ich ignoruje). */
    fun get(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "sk,cs,en") ?: "sk,cs,en"
        return raw.split(",").map { it.trim() }
    }

    fun set(context: Context, langs: List<String>) {
        // zachovaj pozicie slotov (aj prazdne), oddelene ciarkou
        val csv = langs.joinToString(",")
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, csv).apply()
    }

    /** Ponuka jazykov pre vyber (kod -> zobrazeny nazov). "" = nezvolene. */
    val options: List<Pair<String, String>> = listOf(
        "" to "—",
        "sk" to "Slovenčina",
        "cs" to "Čeština",
        "en" to "English",
        "pl" to "Polski",
        "de" to "Deutsch",
        "hu" to "Magyar"
    )

    private fun stripAccents(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s.lowercase()) {
            sb.append(
                when (c) {
                    'á','ä','à','â' -> 'a'; 'č','ç' -> 'c'; 'ď' -> 'd'
                    'é','ě','è','ê' -> 'e'; 'í','ì','î' -> 'i'; 'ľ','ĺ' -> 'l'
                    'ň' -> 'n'; 'ó','ô','ö' -> 'o'; 'ŕ','ř' -> 'r'
                    'š','ś' -> 's'; 'ť' -> 't'; 'ú','ů','ü' -> 'u'
                    'ý' -> 'y'; 'ž','ź' -> 'z'
                    else -> c
                }
            )
        }
        return sb.toString()
    }

    private fun tokens(code: String): List<String> = when (code) {
        "sk" -> listOf("slovak", "sloven", "slk", "slo")
        "cs" -> listOf("czech", "cesk", "cze", "ces")
        "en" -> listOf("english", "anglick", "eng")
        "pl" -> listOf("polish", "polsk", "pol")
        "de" -> listOf("german", "deutsch", "nemeck", "ger", "deu")
        "hu" -> listOf("hungar", "magyar", "madar", "hun")
        else -> emptyList()
    }

    /** Zodpoveda nazov stopy danemu jazyku? */
    fun matches(trackName: String, code: String): Boolean {
        val n = stripAccents(trackName)
        return tokens(code).any { n.contains(it) }
    }
}
