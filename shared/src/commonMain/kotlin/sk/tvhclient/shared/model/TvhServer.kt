package sk.tvhclient.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class TvhServer(
    val id: String,
    val name: String,
    val host: String,
    val port: Int = 9981,
    val useHttps: Boolean = false,
    val username: String = "",
    val password: String = ""
) {
    val baseUrl: String
        get() {
            val scheme = if (useHttps) "https" else "http"
            return "$scheme://$host:$port"
        }
}
