package sk.tvhclient.shared.storage

import android.annotation.SuppressLint
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

/**
 * Drzi application context pre shared modul.
 * Inicializuje sa v Application.onCreate() volanim initSecureStorage(this).
 */
@SuppressLint("StaticFieldLeak")
object AppContextHolder {
    lateinit var context: Context
}

fun initSecureStorage(context: Context) {
    AppContextHolder.context = context.applicationContext
}

actual fun createSecureSettings(): Settings {
    val ctx = AppContextHolder.context
    val masterKey = MasterKey.Builder(ctx)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    val prefs = EncryptedSharedPreferences.create(
        ctx,
        "tvh_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    return SharedPreferencesSettings(prefs)
}
