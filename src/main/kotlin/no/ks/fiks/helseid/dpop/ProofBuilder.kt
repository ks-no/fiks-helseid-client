package no.ks.fiks.helseid.dpop

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import no.ks.fiks.helseid.Configuration
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.*

private const val CLAIM_HTTP_METHOD = "htm"
private const val CLAIM_HTTP_URI = "htu"
private const val CLAIM_NONCE = "nonce"
private const val CLAIM_ACCESS_TOKEN_HASH = "ath"

private const val DPOP_JWS_HEADER_TYPE_VALUE = "dpop+jwt"
private val dpopJwsHeaderType = JOSEObjectType(DPOP_JWS_HEADER_TYPE_VALUE)

class ProofBuilder(
    configuration: Configuration,
) {

    private val jwk = JWK.parse(configuration.jwk)
    private val signer = RSASSASigner(jwk.toRSAKey())

    private val digest = MessageDigest.getInstance("SHA-256")

    fun buildProof(
        endpoint: Endpoint,
        nonce: String? = null,
        accessToken: String? = null,
    ): String =
        SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.PS512)
                .type(dpopJwsHeaderType)
                .jwk(jwk.toPublicJWK())
                .build(),
            JWTClaimsSet.Builder()
                .issueTime(Date(Instant.now().toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim(CLAIM_HTTP_METHOD, endpoint.method.name)
                .claim(CLAIM_HTTP_URI, endpoint.url)
                .apply {
                    if (nonce != null) claim(CLAIM_NONCE, nonce)
                    if (accessToken != null) claim(CLAIM_ACCESS_TOKEN_HASH, accessToken.hashAndEncode())
                }
                .build()
        )
            .apply { sign(signer) }
            .serialize()

    private fun String.hashAndEncode(): String {
        val hash = digest.digest(toByteArray(StandardCharsets.US_ASCII))
        return Base64URL.encode(hash).toString()
    }

}