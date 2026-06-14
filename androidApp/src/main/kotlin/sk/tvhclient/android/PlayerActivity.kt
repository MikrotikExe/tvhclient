package sk.tvhclient.android

import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import sk.tvhclient.shared.Tvh

/**
 * Live prehravac postaveny na libVLC (rovnaky engine ako VLCKit na iOS).
 * Dekoduje MPEG-2 video aj MP2/AC3/EAC3/DTS zvuk softverovo - server
 * nemusi transkodovat (pass profil). Auth: credentials su v stream URL.
 *
 * VLCVideoLayout je holy povrch na video; ovladanie je Compose overlay.
 * Pri live streame seek nedava zmysel, preto len play/pause + zavriet.
 * Overlay sa zobrazi tapom a po 4s sa sam skryje.
 */
class PlayerActivity : ComponentActivity() {

    private lateinit var libVlc: LibVLC
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val channelUuid = intent.getStringExtra(EXTRA_UUID)
        val channelTitle = intent.getStringExtra(EXTRA_TITLE) ?: ""

        val server = Tvh.store.active()
        if (server == null || channelUuid == null) {
            finish()
            return
        }

        val options = arrayListOf(
            "--network-caching=1500",
            "--no-drop-late-frames",
            "--no-skip-frames"
        )
        libVlc = LibVLC(this, options)
        mediaPlayer = MediaPlayer(libVlc)

        mediaPlayer.setEventListener { event ->
            if (event.type == MediaPlayer.Event.EncounteredError) {
                Toast.makeText(
                    this,
                    getString(R.string.playback_error, "VLC"),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        val streamUrl = Tvh.liveUrl(
            server, channelUuid, channelTitle,
            server.profile.ifBlank { "pass" }
        )

        setContent {
            PlayerUi(
                title = channelTitle,
                player = mediaPlayer,
                onAttach = { layout -> mediaPlayer.attachViews(layout, null, false, false) },
                onStart = {
                    val media = Media(libVlc, Uri.parse(streamUrl))
                    media.setHWDecoderEnabled(true, false)
                    mediaPlayer.media = media
                    media.release()
                    mediaPlayer.play()
                },
                onClose = { finish() }
            )
        }
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

@Composable
private fun PlayerUi(
    title: String,
    player: MediaPlayer,
    onAttach: (VLCVideoLayout) -> Unit,
    onStart: () -> Unit,
    onClose: () -> Unit
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }

    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            kotlinx.coroutines.delay(4000)
            controlsVisible = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { controlsVisible = !controlsVisible }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val layout = VLCVideoLayout(ctx)
                layout.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                onAttach(layout)
                onStart()
                layout
            }
        )

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize().background(Color(0x66000000))) {
                // Horny pruh: zavriet + nazov
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleButton("\u2715", onClick = onClose)  // ✕
                    Text(
                        title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
                // Stred: play/pause
                CircleButton(
                    label = if (isPlaying) "\u23F8" else "\u25B6",  // ⏸ / ▶
                    big = true,
                    onClick = {
                        if (player.isPlaying) {
                            player.pause(); isPlaying = false
                        } else {
                            player.play(); isPlaying = true
                        }
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun CircleButton(
    label: String,
    onClick: () -> Unit,
    big: Boolean = false,
    modifier: Modifier = Modifier
) {
    val s = if (big) 76.dp else 44.dp
    Box(
        modifier
            .size(s)
            .clip(CircleShape)
            .background(Color(0x88000000))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = Color.White,
            textAlign = TextAlign.Center,
            fontSize = if (big) 34.sp else 20.sp
        )
    }
}
