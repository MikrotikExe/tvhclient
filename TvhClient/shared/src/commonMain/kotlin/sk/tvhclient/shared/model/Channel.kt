package sk.tvhclient.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Kanal z api/channel/grid. Mapovanie poli prebrane z Enigma2 pluginu
 * (_data_api.py / _picons.py): uuid, name, number, icon_public_url
 * (imagecache/NNNN), tags (zoznam tag uuid), services.
 */
@Serializable
data class Channel(
    val uuid: String,
    val name: String = "",
    @SerialName("number") val number: Int? = null,
    @SerialName("icon_public_url") val iconPublicUrl: String? = null,
    val tags: List<String> = emptyList(),
    val services: List<String> = emptyList(),
    @SerialName("enabled") val enabled: Boolean = true
) {
    /**
     * Je kanal radio? TVH neoznacuje radio priamo v channel/grid, urcuje sa
     * podla typu sluzby. Heuristika z pluginu: ak nazov/typ nesie radio
     * znaky. Spolahlivejsie sa rozlisuje v M6 cez api/channel/grid filter,
     * zatial ponechane ako placeholder.
     */
    val isRadio: Boolean get() = false
}
