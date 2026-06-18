package sk.tvhclient.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Vypocet pozicie zaciatku/konca relacie v nahravke (znacka na seek bare). */
class DvrEntryMarkerTest {

    // 60 min relacia, okraj 15 min pred a 15 min po -> subor 90 min.
    // start_real = start - 900s, stop_real = stop + 900s.
    private val padded = DvrEntry(
        start = 10_000,
        stop = 10_000 + 3600,            // relacia 60 min
        startReal = 10_000 - 900,        // 15 min pred
        stopReal = 10_000 + 3600 + 900   // 15 min po
    )

    @Test
    fun realLengthIncludesPadding() {
        // 60 + 15 + 15 = 90 min = 5400 s
        assertEquals(5400, padded.realLengthSec)
    }

    @Test
    fun programStartIsPaddingFraction() {
        // 900 / 5400 = 0.1667
        assertEquals(0.1667f, padded.programStartFraction, 0.001f)
    }

    @Test
    fun programStopIsBeforeTrailingPadding() {
        // (900 + 3600) / 5400 = 0.8333
        assertEquals(0.8333f, padded.programStopFraction, 0.001f)
    }

    @Test
    fun fallbackToStartExtraWhenNoRealFields() {
        val e = DvrEntry(
            start = 10_000,
            stop = 10_000 + 3600,
            startExtra = 15,  // minut
            stopExtra = 15
        )
        assertEquals(5400, e.realLengthSec)
        assertEquals(0.1667f, e.programStartFraction, 0.001f)
    }

    @Test
    fun noPaddingMeansNoMarker() {
        val e = DvrEntry(start = 10_000, stop = 10_000 + 3600)
        assertEquals(3600, e.realLengthSec)
        // bez okraja -> ziadna znacka (0 a 1)
        assertEquals(0f, e.programStartFraction)
        assertEquals(1f, e.programStopFraction)
    }

    @Test
    fun markerStaysInRange() {
        assertTrue(padded.programStartFraction in 0f..1f)
        assertTrue(padded.programStopFraction in 0f..1f)
        assertTrue(padded.programStartFraction < padded.programStopFraction)
    }
}
