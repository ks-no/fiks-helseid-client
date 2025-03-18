package no.ks.fiks.helseid

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.SignedJWT
import io.kotest.assertions.asClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.date.shouldBeBefore
import io.kotest.matchers.date.shouldBeBetween
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.core5.http.ClassicHttpRequest
import org.apache.hc.core5.http.io.HttpClientResponseHandler
import org.apache.hc.core5.http.message.BasicNameValuePair
import org.apache.hc.core5.net.WWWFormCodec
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.*

class HelseIdClientTest : StringSpec({

    "Check that the access token request has the expected values" {
        val clientId = UUID.randomUUID().toString()
        val keyId = UUID.randomUUID().toString()
        val environment = Environment("http://localhost:8080/api/token", UUID.randomUUID().toString())
        val duration = Duration.ofSeconds(25)

        val slot = slot<ClassicHttpRequest>()
        val httpClient = mockk<HttpClient> {
            every { execute(capture(slot), any<HttpClientResponseHandler<TokenResponse>>()) } returns mockk()
        }

        HelseIdClient(
            configuration = Configuration(
                clientId = clientId,
                privateKey = readPrivateKey(),
                keyId = keyId,
                environment = environment,
                jwtRequestExpirationTime = duration,
            ),
            httpClient = httpClient,
        ).getAccessToken()

        with(slot.captured) {
            uri shouldBe URI.create(environment.url)
            entity.contentType shouldBe "application/x-www-form-urlencoded; charset=UTF-8"

            WWWFormCodec.parse(entity.content.readAllBytes().decodeToString(), StandardCharsets.UTF_8).asClue { params ->
                params shouldHaveSize 4
                params shouldContain BasicNameValuePair("client_id", clientId)
                params shouldContain BasicNameValuePair("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                params shouldContain BasicNameValuePair("grant_type", "client_credentials")
                params shouldHaveSingleElement { it.name == "client_assertion" }

                val jwt = SignedJWT.parse(params.single { it.name == "client_assertion" }.value)
                jwt.header.algorithm shouldBe JWSAlgorithm.PS512
                jwt.header.keyID shouldBe keyId
                jwt.header.type shouldBe JOSEObjectType("client-authentication+jwt")

                RSASSAVerifier(readPublicKey()).verify(jwt.header, jwt.signingInput, jwt.signature) shouldBe true

                with(jwt.jwtClaimsSet) {
                    subject shouldBe clientId
                    issuer shouldBe clientId
                    audience shouldBe listOf(environment.audience)
                    issueTime.toInstant() shouldBeBefore Instant.now()
                    jwtid shouldNot beNull()
                    notBeforeTime.toInstant() shouldBeBefore Instant.now()
                    expirationTime.toInstant().shouldBeBetween(Instant.now().plus(duration).minusSeconds(1), Instant.now().plus(duration).plusSeconds(1))
                }
            }
        }
    }

})

private fun readPrivateKey() = readRsaKey().toPrivateKey()

private fun readPublicKey() = readRsaKey().toRSAPublicKey()

private fun readRsaKey() = JWK.parse(readJwk()) as RSAKey

private fun readJwk() = HelseIdClientTest::class.java.classLoader.getResourceAsStream("jwk.json")!!.readAllBytes().decodeToString()
