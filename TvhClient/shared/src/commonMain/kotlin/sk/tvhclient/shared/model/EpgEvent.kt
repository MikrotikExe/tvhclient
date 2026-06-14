package sk.tvhclient.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * EPG event z api/epg/events/grid. Pre now/next staci channelUuid + cas +
 * title. Pole prebrane z pluginu (_data_api.get_epg_now).
 */
@Serializable
data class EpgEvent(
    @SerialName("eventId") val eventId: Long? = null,
    @SerialName("channelUuid") val channelUuid: String? = null,
    val start: Long = 0,
    val stop: Long = 0,
    val title: String = "",
    val subtitle: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("nextEventId") val nextEventId: Long? = null
)
