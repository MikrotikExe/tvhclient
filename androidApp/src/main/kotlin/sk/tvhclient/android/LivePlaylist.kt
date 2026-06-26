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
        val nowStop: Long,
        val nextTitle: String = "",
        val nextStart: Long = 0,
        val nextStop: Long = 0,
        val recording: Boolean = false
    )

    @Volatile
    var channels: List<LiveChannel> = emptyList()

    // M271: procesova cache EPG (uuid -> relacie) + cas poslednej uspesnej obnovy.
    // Prezije zatvorenie/otvorenie prehravaca, takze sa nesťahuje znova pri kazdom otvoreni.
    @Volatile
    var epgUpcoming: Map<String, List<sk.tvhclient.shared.model.EpgEvent>> = emptyMap()
    @Volatile
    var epgLastOkMs: Long = 0L

    fun clearEpg() {
        epgUpcoming = emptyMap()
        epgLastOkMs = 0L
    }

    @Volatile
    var index: Int = -1

    fun setIndexForUuid(uuid: String?) {
        index = if (uuid == null) -1 else channels.indexOfFirst { it.uuid == uuid }
    }
}
