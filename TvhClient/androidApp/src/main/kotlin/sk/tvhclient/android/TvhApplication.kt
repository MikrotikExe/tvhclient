package sk.tvhclient.android

import android.app.Application
import sk.tvhclient.shared.storage.initSecureStorage

class TvhApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initSecureStorage(this)
    }
}
