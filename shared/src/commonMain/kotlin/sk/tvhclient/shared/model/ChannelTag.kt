package sk.tvhclient.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Tag/kategoria z api/channeltag/grid. uuid sa pouziva na filtrovanie
 * kanalov (Channel.tags obsahuje tieto uuid).
 */
@Serializable
data class ChannelTag(
    val uuid: String,
    val name: String = "",
    @SerialName("index") val index: Int = 999999,
    @SerialName("enabled") val enabled: Boolean = true
)
