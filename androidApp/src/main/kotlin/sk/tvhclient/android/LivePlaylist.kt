package sk.tvhclient.android

/**
 * Zdielany zoznam live kanalov pre prepinanie (zapping) a zoznam kanalov
 * priamo v prehravaci. Naplna sa pri zobrazeni zoznamu kanalov / mriezky.
 */
object LivePlaylist {
    data class LiveChannel(
        val uuid: String,
        val name: String,
        val number: Int,
        val piconUrl: String?,
        val nowTitle: String,
        val nowStart: Long,
        val nowStop: Long
    )

    @Volatile
    var channels: List<LiveChannel> = emptyList()

    @Volatile
    var index: Int = -1

    fun setIndexForUuid(uuid: String?) {
        index = if (uuid == null) -1 else channels.indexOfFirst { it.uuid == uuid }
    }
}
