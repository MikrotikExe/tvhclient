package sk.tvhclient.shared

actual fun currentTimeSeconds(): Long = System.currentTimeMillis() / 1000
