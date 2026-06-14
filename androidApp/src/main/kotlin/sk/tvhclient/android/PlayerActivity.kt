package sk.tvhclient.android

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import sk.tvhclient.shared.Tvh

/**
 * Live prehravac postaveny na libVLC (rovnaky engine ako VLCKit na iOS).
 *
 * Dovod oproti ExoPlayer: DVB kanaly su casto MPEG-2 video s MP2/AC3 zvukom.
 * ExoPlayer nema softverovy MP2/AC3 dekoder (chyba audio, video ide) a
 * Google neposkytuje hotovy FFmpeg dekoder. libVLC dekoduje MPEG-2 aj
 * MP2/AC3/EAC3/DTS softverovo - server nemusi transkodovat (pass profil).
 *
 * Auth: credentials su v stream URL (user:pass@host) - libVLC ich z URL
 * pouzije (rovnako ako VLCKit na iOS). Plain aj digest cez libvlc HTTP stack.
 */
class PlayerActivity : ComponentActivity() {

    private lateinit var libVlc: LibVLC
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var videoLayout: VLCVideoLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val channelUuid = intent.getStringExtra(EXTRA_UUID)
        val channelTitle = intent.getStringExtra(EXTRA_TITLE)

        videoLayout = VLCVideoLayout(this)
        setContentView(videoLayout)

        val server = Tvh.store.active()
        if (server == null || channelUuid == null) {
            finish()
            return
        }

        // network-caching: vyrovnavacia pamat pre live stream (jitter/reconnect)
        val options = arrayListOf(
            "--network-caching=1500",
            "--no-drop-late-frames",
            "--no-skip-frames"
        )
        libVlc = LibVLC(this, options)
        mediaPlayer = MediaPlayer(libVlc)
        mediaPlayer.attachViews(videoLayout, null, false, false)

        mediaPlayer.setEventListener { event ->
            if (event.type == MediaPlayer.Event.EncounteredError) {
                Toast.makeText(
                    this,
                    getString(R.string.playback_error, "VLC"),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // URL s creds (libVLC ich z URL pouzije) + profil zo servera
        val streamUrl = Tvh.liveUrl(
            server, channelUuid, channelTitle,
            server.profile.ifBlank { "pass" }
        )

        val media = Media(libVlc, Uri.parse(streamUrl))
        media.setHWDecoderEnabled(true, false)
        mediaPlayer.media = media
        media.release()
        mediaPlayer.play()
    }

    override fun onStop() {
        super.onStop()
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.stop()
            mediaPlayer.detachViews()
            mediaPlayer.release()
        }
        if (::libVlc.isInitialized) {
            libVlc.release()
        }
    }

    companion object {
        const val EXTRA_UUID = "channel_uuid"
        const val EXTRA_TITLE = "channel_title"
    }
}
