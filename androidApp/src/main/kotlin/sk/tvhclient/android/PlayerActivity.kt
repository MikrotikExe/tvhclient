package sk.tvhclient.android

import android.os.Bundle
import android.util.Base64
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import okhttp3.OkHttpClient
import sk.tvhclient.shared.Tvh

/**
 * Live prehravac. Media3/ExoPlayer s MPEG-TS (pass profil). Auth ide cez
 * Authorization: Basic hlavicku v OkHttp datasource (rovnako ako picony) —
 * stream URL bez creds, lebo prehravac otvara spojenie mimo Ktor klienta.
 *
 * MPEG-TS: ExoPlayer ho rozpozna automaticky cez TsExtractor v default
 * extractors factory. HW dekod MPEG-2 je na TV boxoch standard; na mobiloch
 * ak chyba kodek, onPlayerError vypise citatelnu hlasku.
 *
 * Pozn.: digest-only servery treba Authenticator (neskor). Tvoj server sa
 * pripaja so stock auth + picony cez Basic funguju, takze Basic staci.
 */
class PlayerActivity : ComponentActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val channelUuid = intent.getStringExtra(EXTRA_UUID)
        val channelTitle = intent.getStringExtra(EXTRA_TITLE)

        playerView = PlayerView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            useController = true
        }
        setContentView(playerView)

        val server = Tvh.store.active()
        if (server == null || channelUuid == null) {
            finish()
            return
        }

        val streamUrl = Tvh.liveUrlNoCreds(server, channelUuid, channelTitle)

        // OkHttp s Basic auth hlavickou
        val authHeader: String? = if (server.username.isNotEmpty()) {
            val raw = "${server.username}:${server.password}"
            "Basic " + Base64.encodeToString(raw.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        } else null

        val okHttp = OkHttpClient.Builder().build()
        val dataSourceFactory = OkHttpDataSource.Factory(okHttp).apply {
            if (authHeader != null) {
                setDefaultRequestProperties(mapOf("Authorization" to authHeader))
            }
        }

        val exo = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
        playerView.player = exo
        player = exo

        exo.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                android.widget.Toast.makeText(
                    this@PlayerActivity,
                    getString(R.string.playback_error, error.errorCodeName),
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        })

        exo.setMediaItem(MediaItem.fromUri(streamUrl))
        exo.playWhenReady = true
        exo.prepare()
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }

    companion object {
        const val EXTRA_UUID = "channel_uuid"
        const val EXTRA_TITLE = "channel_title"
    }
}
