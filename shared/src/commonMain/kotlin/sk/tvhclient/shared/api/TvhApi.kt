package sk.tvhclient.shared.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.DigestAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.auth.providers.digest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import sk.tvhclient.shared.model.ServerInfo
import sk.tvhclient.shared.model.TvhServer

/**
 * HTTP klient pre Tvheadend 4.3 JSON API.
 *
 * Auth: server moze byt nastaveny na Plain alebo Digest. Instalujeme oba
 * providery - Ktor si vyberie podla WWW-Authenticate hlavicky v 401 challenge,
 * cize auto-detekcia bez konfiguracie pouzivatelom.
 */
class TvhApi(private val server: TvhServer) {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient {
        expectSuccess = false
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 5_000
            requestTimeoutMillis = 15_000
            socketTimeoutMillis = 15_000
        }
        if (server.username.isNotEmpty()) {
            install(Auth) {
                basic {
                    credentials {
                        BasicAuthCredentials(server.username, server.password)
                    }
                    realm = null
                }
                digest {
                    credentials {
                        DigestAuthCredentials(server.username, server.password)
                    }
                }
            }
        }
    }

    suspend fun serverInfo(): ServerInfo {
        val response = client.get("${server.baseUrl}/api/serverinfo")
        if (response.status != HttpStatusCode.OK) {
            throw TvhHttpException(response.status.value)
        }
        return response.body()
    }

    /**
     * Test pripojenia s citatelnym vysledkom pre UI.
     * Nikdy nehadze vynimku - vsetko mapuje na ConnectionResult.
     */
    suspend fun testConnection(): ConnectionResult {
        return try {
            val response = client.get("${server.baseUrl}/api/serverinfo")
            when (response.status.value) {
                200 -> ConnectionResult.Success(response.body<ServerInfo>())
                401, 403 -> ConnectionResult.AuthFailed(response.status.value)
                else -> ConnectionResult.HttpError(response.status.value)
            }
        } catch (e: Exception) {
            ConnectionResult.NetworkError(e.message ?: e::class.simpleName ?: "unknown")
        }
    }

    fun close() {
        client.close()
    }
}

class TvhHttpException(val httpCode: Int) : Exception("HTTP $httpCode")
