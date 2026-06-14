package sk.tvhclient.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerInfo(
    @SerialName("sw_version") val swVersion: String? = null,
    @SerialName("api_version") val apiVersion: Int? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("capabilities") val capabilities: List<String> = emptyList()
)
