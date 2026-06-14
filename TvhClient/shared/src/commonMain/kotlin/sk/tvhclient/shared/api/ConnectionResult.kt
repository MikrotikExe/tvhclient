package sk.tvhclient.shared.api

import sk.tvhclient.shared.model.ServerInfo

sealed class ConnectionResult {
    data class Success(val info: ServerInfo) : ConnectionResult()
    data class AuthFailed(val httpCode: Int) : ConnectionResult()
    data class HttpError(val httpCode: Int) : ConnectionResult()
    data class NetworkError(val message: String) : ConnectionResult()
}
