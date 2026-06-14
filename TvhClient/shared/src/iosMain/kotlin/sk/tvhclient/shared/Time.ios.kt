package sk.tvhclient.shared

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

@OptIn(ExperimentalForeignApi::class)
actual fun currentTimeSeconds(): Long = NSDate().timeIntervalSince1970.toLong()
