package sk.tvhclient.android

import android.content.Context

/**
 * Zvukovy vystupny modul libVLC (telefony). Pri rozchadzajucom sa zvuku, ktory
 * narasta v case (drift), na niektorych telefonoch pomaha prepnut z predvoleneho
 * AudioTrack na OpenSL ES — ten ma inu, casto presnejsiu spravu latencie, takze
 * sa zvuk neoneskoruje od obrazu.
 *  - AUTO: android_audiotrack (default)
 *  - OPENSLES: opensles
 * Mapuje sa na MediaPlayer.setAudioOutput. Ulozene globalne v SharedPreferences.
 */
object AudioModulePref {
    private const val PREFS = "app_prefs"
    private const val KEY = "audio_output_module"

    const val AUTO = "auto"
    const val OPENSLES = "opensles"

    val options = listOf(AUTO, OPENSLES)

    /** aout modul pre setAudioOutput; null = nechaj default (AudioTrack). */
    fun module(context: Context): String? =
        if (get(context) == OPENSLES) "opensles" else null

    fun get(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, AUTO) ?: AUTO

    fun set(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, value).apply()
    }
}
