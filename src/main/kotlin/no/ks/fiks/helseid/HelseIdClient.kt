package no.ks.fiks.helseid

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import mu.KotlinLogging
import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.ClassicHttpResponse
import org.apache.hc.core5.http.message.BasicNameValuePair
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*


private const val CLIENT_ASSERTION_TYPE_VALUE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"
private const val GRANT_TYPE_VALUE = "client_credentials"

private const val JWS_HEADER_TYPE_VALUE = "client-authentication+jwt"
private val jwsHeaderType = JOSEObjectType(JWS_HEADER_TYPE_VALUE)

private object FormFields {
    const val CLIENT_ID = "client_id"
    const val CLIENT_ASSERTION = "client_assertion"
    const val CLIENT_ASSERTION_TYPE = "client_assertion_type"
    const val GRANT_TYPE = "grant_type"
}

private val log = KotlinLogging.logger { }

class HelseIdClient(
    private val configuration: Configuration,
    private val httpClient: HttpClient = HttpClients.createMinimal(),
) {

    private val signer = RSASSASigner(configuration.privateKey)

    private val mapper = ObjectMapper()
        .findAndRegisterModules()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    fun getAccessToken(): TokenResponse =
        httpClient
            .execute(buildPostRequest()) {
                if (it.code >= 300) {
                    throw HttpException(it.code, it.readBodyAsString())
                }
                mapper.readValue<InternalTokenResponse>(it.readBodyAsString())
                    .toTokenResponse()
            }

    private fun buildPostRequest() = HttpPost(configuration.environment.url).apply {
        entity = buildUrlEncodedFormEntity()
    }

    private fun buildUrlEncodedFormEntity() = UrlEncodedFormEntity(
        listOf(
            BasicNameValuePair(FormFields.CLIENT_ID, configuration.clientId),
            BasicNameValuePair(FormFields.CLIENT_ASSERTION, buildSignedJwt().serialize()),
            BasicNameValuePair(FormFields.CLIENT_ASSERTION_TYPE, CLIENT_ASSERTION_TYPE_VALUE),
            BasicNameValuePair(FormFields.GRANT_TYPE, GRANT_TYPE_VALUE),
        ),
        StandardCharsets.UTF_8,
    )

    private fun buildSignedJwt() =
        Instant.now().let { now ->
            SignedJWT(
                JWSHeader.Builder(JWSAlgorithm.PS512)
                    .keyID(configuration.keyId)
                    .type(jwsHeaderType)
                    .build(),
                JWTClaimsSet.Builder()
                    .subject(configuration.clientId)
                    .issuer(configuration.clientId)
                    .audience(configuration.environment.audience)
                    .issueTime(now.toDate())
                    .jwtID(UUID.randomUUID().toString())
                    .notBeforeTime(now.toDate())
                    .expirationTime(now.plus(configuration.jwtRequestExpirationTime).toDate())
                    .build()
            ).apply {
                log.debug { "Generated JWT id: ${this.jwtClaimsSet.jwtid}" }
                sign(signer)
            }
        }

}

private data class InternalTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,

    @JsonProperty("expires_in")
    val expiresIn: Int,

    @JsonProperty("token_type")
    val tokenType: String,

    @JsonProperty("scope")
    val scope: String,
) {
    fun toTokenResponse() = TokenResponse(
        accessToken = accessToken,
        expiresIn = expiresIn,
        tokenType = tokenType,
        scope = scope,
    )
}

private fun ClassicHttpResponse.readBodyAsString() = entity.content.readAllBytes().decodeToString()

private fun Instant.toDate() = Date(toEpochMilli())
