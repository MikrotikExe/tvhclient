package sk.tvhclient.shared

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.dateWithTimeIntervalSince1970

private fun fmt(pattern: String, epochSec: Long): String {
    val df = NSDateFormatter()
    df.dateFormat = pattern
    return df.stringFromDate(NSDate.dateWithTimeIntervalSince1970(epochSec.toDouble()))
}

actual fun formatTimeHm(epochSec: Long): String = fmt("HH:mm", epochSec)

actual fun formatDayLabel(epochSec: Long): String =
    fmt("EEEE d.M.", epochSec).replaceFirstChar { it.uppercase() }
