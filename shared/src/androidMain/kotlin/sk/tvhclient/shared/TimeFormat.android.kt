package sk.tvhclient.shared

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun formatTimeHm(epochSec: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochSec * 1000))

actual fun formatDayLabel(epochSec: Long): String =
    SimpleDateFormat("EEEE d.M.", Locale.getDefault()).format(Date(epochSec * 1000))
        .replaceFirstChar { it.uppercase() }
