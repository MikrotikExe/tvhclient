package sk.tvhclient.android

import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import sk.tvhclient.shared.htsp.HtspClient
import sk.tvhclient.shared.model.TvhServer
import java.io.FileDescriptor
import java.io.OutputStream

/**
 * M162 — premostí HTSP zivy stream (premuxovany na MPEG-TS v HtspClient.streamSubscribe)
 * do libVLC cez lokalny pipe. `start` vytvori ParcelFileDescriptor pipe, spusti korutinu
 * ktora pise TS bajty do write-endu, a vrati read-end FileDescriptor pre Media(libVlc, fd).
 * Pri zatvoreni read-endu / `stop` sa write zlomi (broken pipe) a slucka skonci.
 *
 * libVLC nehovori HTSP, preto HTSP citame my a podavame mu hotovy TS.
 */
class HtspTsFeeder(private val server: TvhServer) {

    private var job: Job? = null
    private var readPfd: ParcelFileDescriptor? = null
    private var writePfd: ParcelFileDescriptor? = null
    private var out: OutputStream? = null

    /** Spusti feed pre kanal a vrati read FileDescriptor pre libVLC. */
    fun start(channelId: Long, scope: CoroutineScope): FileDescriptor {
        val pipe = ParcelFileDescriptor.createPipe()
        val read = pipe[0]
        val write = pipe[1]
        readPfd = read
        writePfd = write
        val os = ParcelFileDescriptor.AutoCloseOutputStream(write)
        out = os
        job = scope.launch(Dispatchers.IO) {
            val client = HtspClient(server.host, server.htspPort, server.username, server.password)
            try {
                client.connect()
                client.streamSubscribe(
                    channelId = channelId,
                    onTs = { bytes -> os.write(bytes) }   // blokujuci zapis = prirodzeny backpressure
                )
            } catch (_: Throwable) {
                // zrusenie korutiny / zlomeny pipe / chyba spojenia -> ticho ukonci
            } finally {
                client.close()
                try { os.close() } catch (_: Throwable) {}
            }
        }
        return read.fileDescriptor
    }

    fun stop() {
        job?.cancel()
        job = null
        try { out?.close() } catch (_: Throwable) {}
        try { readPfd?.close() } catch (_: Throwable) {}
        try { writePfd?.close() } catch (_: Throwable) {}
        out = null
        readPfd = null
        writePfd = null
    }
}
