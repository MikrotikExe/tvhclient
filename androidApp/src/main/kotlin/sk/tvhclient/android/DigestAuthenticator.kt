package sk.tvhclient.android

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.security.MessageDigest
import kotlin.random.Random

/**
 * OkHttp Authenticator pre HTTP Digest (RFC 2617/7616) — pouzity pri stahovani
 * piconov, aby appka nacitala logo aj z digest-only Tvheadend servera (nie len
 * Basic). Podporuje MD5 aj SHA-256, qop=auth. Tvheadend default je MD5.
 *
 * Coil/OkHttp posle Basic preemptivne; ak server odpovie 401 s Digest vyzvou,
 * tento Authenticator dopocita Authorization: Digest a request zopakuje. Proti
 * sluckam: ak uz bola digest odpoved poslana, vrati null.
 */
class DigestAuthenticator(
    private val username: String,
    private val password: String,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Uz sme raz digest skusili -> nezacykli sa.
        if (response.request.header("Authorization")?.startsWith("Digest") == true) return null
        if (priorResponseCount(response) >= 3) return null

        val header = response.headers("WWW-Authenticate")
            .firstOrNull { it.trim().startsWith("Digest", ignoreCase = true) } ?: return null

        val p = parseChallenge(header)
        val realm = p["realm"] ?: return null
        val nonce = p["nonce"] ?: return null
        val opaque = p["opaque"]
        val algorithm = (p["algorithm"] ?: "MD5").uppercase()
        val qopRaw = p["qop"]
        val qop = when {
            qopRaw == null -> null
            qopRaw.split(",").any { it.trim().equals("auth", true) } -> "auth"
            else -> null
        }

        val req = response.request
        val method = req.method
        val uri = req.url.encodedPath + (req.url.encodedQuery?.let { "?$it" } ?: "")

        val ha1 = h(algorithm, "$username:$realm:$password")
        val ha2 = h(algorithm, "$method:$uri")

        val resp: String
        val cnonce = randomHex(16)
        val nc = "00000001"
        resp = if (qop != null) {
            h(algorithm, "$ha1:$nonce:$nc:$cnonce:$qop:$ha2")
        } else {
            h(algorithm, "$ha1:$nonce:$ha2")
        }

        val sb = StringBuilder("Digest ")
        sb.append("username=\"").append(username).append("\", ")
        sb.append("realm=\"").append(realm).append("\", ")
        sb.append("nonce=\"").append(nonce).append("\", ")
        sb.append("uri=\"").append(uri).append("\", ")
        if (qop != null) {
            sb.append("qop=").append(qop).append(", ")
            sb.append("nc=").append(nc).append(", ")
            sb.append("cnonce=\"").append(cnonce).append("\", ")
        }
        sb.append("response=\"").append(resp).append("\"")
        if (algorithm != "MD5") sb.append(", algorithm=").append(algorithm)
        if (opaque != null) sb.append(", opaque=\"").append(opaque).append("\"")

        return req.newBuilder().header("Authorization", sb.toString()).build()
    }

    private fun priorResponseCount(response: Response): Int {
        var n = 1
        var r = response.priorResponse
        while (r != null) { n++; r = r.priorResponse }
        return n
    }

    private fun h(algorithm: String, data: String): String {
        val md = when (algorithm) {
            "SHA-256", "SHA-256-SESS" -> MessageDigest.getInstance("SHA-256")
            else -> MessageDigest.getInstance("MD5")
        }
        return md.digest(data.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private fun randomHex(bytes: Int): String =
        (0 until bytes).joinToString("") { "%02x".format(Random.nextInt(0, 256)) }

    /** Parsuje "key=value" / key="value" pary z Digest vyzvy. */
    private fun parseChallenge(header: String): Map<String, String> {
        val body = header.trim().removePrefix("Digest").removePrefix("DIGEST").trim()
        val map = HashMap<String, String>()
        var i = 0
        val n = body.length
        while (i < n) {
            while (i < n && (body[i] == ' ' || body[i] == ',')) i++
            val keyStart = i
            while (i < n && body[i] != '=') i++
            if (i >= n) break
            val key = body.substring(keyStart, i).trim().lowercase()
            i++ // skip '='
            val value: String
            if (i < n && body[i] == '"') {
                i++
                val vs = i
                while (i < n && body[i] != '"') i++
                value = body.substring(vs, i)
                if (i < n) i++ // skip closing quote
            } else {
                val vs = i
                while (i < n && body[i] != ',') i++
                value = body.substring(vs, i).trim()
            }
            if (key.isNotEmpty()) map[key] = value
        }
        return map
    }
}
