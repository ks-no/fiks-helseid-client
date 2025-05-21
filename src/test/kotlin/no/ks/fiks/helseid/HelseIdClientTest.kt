package no.ks.fiks.helseid

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.SignedJWT
import io.kotest.assertions.asClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.date.shouldBeBefore
import io.kotest.matchers.date.shouldBeBetween
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
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

class HelseIdClientTest : FreeSpec({

    "Bearer token" - {
        "Check that the access token request has the expected values" {
            val clientId = UUID.randomUUID().toString()
            val environment = Environment("http://localhost:8080", UUID.randomUUID().toString())

            val slot = slot<ClassicHttpRequest>()
            val httpClient = mockk<HttpClient> {
                every { execute(capture(slot), any<HttpClientResponseHandler<TokenResponse>>()) } returns mockk()
            }
            val tokenEndpoint = URI("http://${UUID.randomUUID()}:8080/token")
            val openIdConfiguration = mockk<OpenIdConfiguration> {
                every { getTokenEndpoint() } returns tokenEndpoint
            }

            HelseIdClient(
                configuration = Configuration(
                    clientId = clientId,
                    jwk = readJwkJson(),
                    environment = environment,
                ),
                httpClient = httpClient,
                openIdConfiguration = openIdConfiguration,
            ).getAccessToken()

            with(slot.captured) {
                uri shouldBe tokenEndpoint
                getFirstHeader("DPoP") should beNull()

                entity.contentType shouldBe "application/x-www-form-urlencoded; charset=UTF-8"

                WWWFormCodec.parse(entity.content.readAllBytes().decodeToString(), StandardCharsets.UTF_8).asClue { params ->
                    params shouldHaveSize 4
                    params shouldContain BasicNameValuePair("client_id", clientId)
                    params shouldContain BasicNameValuePair("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                    params shouldContain BasicNameValuePair("grant_type", "client_credentials")
                    params shouldHaveSingleElement { it.name == "client_assertion" }

                    val jwt = SignedJWT.parse(params.single { it.name == "client_assertion" }.value)
                    jwt.header.algorithm shouldBe JWSAlgorithm.PS512
                    jwt.header.keyID shouldBe "LJjagdyyRG-oj3bYLEf3kOt7im5ChDHe05DdiHUtqAA"
                    jwt.header.type shouldBe JOSEObjectType("client-authentication+jwt")

                    RSASSAVerifier(readJwk().toRSAKey()).verify(jwt.header, jwt.signingInput, jwt.signature) shouldBe true

                    jwt.jwtClaimsSet.claims shouldHaveSize 7
                    with(jwt.jwtClaimsSet) {
                        subject shouldBe clientId
                        issuer shouldBe clientId
                        audience shouldBe listOf(environment.audience)
                        issueTime.toInstant() shouldBeBefore Instant.now()
                        jwtid shouldNot beNull()
                        notBeforeTime.toInstant() shouldBeBefore Instant.now()
                        expirationTime.toInstant().shouldBeBetween(Instant.now().plusSeconds(55), Instant.now().plusSeconds(65))
                    }
                }
            }
        }

        "The access token should be cached according to config" {
            val clientId = UUID.randomUUID().toString()
            val environment = Environment("http://localhost:8080/api/token", UUID.randomUUID().toString())

            val httpClient = mockk<HttpClient> {
                every { execute(any(), any<HttpClientResponseHandler<TokenResponse>>()) } returns mockk()
            }
            val tokenEndpoint = URI("http://${UUID.randomUUID()}:8080/token")
            val openIdConfiguration = mockk<OpenIdConfiguration> {
                every { getTokenEndpoint() } returns tokenEndpoint
            }

            val client = HelseIdClient(
                configuration = Configuration(
                    clientId = clientId,
                    jwk = readJwkJson(),
                    environment = environment,
                    accessTokenLifetime = Duration.ofSeconds(1),
                    accessTokenRenewalThreshold = Duration.ofMillis(300), // Will be renewed after 700 ms
                ),
                httpClient = httpClient,
                openIdConfiguration = openIdConfiguration,
            )

            val start = Instant.now()
            while (start.plusMillis(600).isAfter(Instant.now())) {
                client.getAccessToken()
            }

            verify(exactly = 1) { httpClient.execute(any(), any<HttpClientResponseHandler<TokenResponse>>()) }

            while (start.plusMillis(1000).isAfter(Instant.now())) {
                client.getAccessToken()
            }

            verify(exactly = 2) { httpClient.execute(any(), any<HttpClientResponseHandler<TokenResponse>>()) }
        }
    }

    "DPoP token" - {
        "Check that the access token request and proof has the expected values" {
            val clientId = UUID.randomUUID().toString()
            val environment = Environment("http://localhost:8080", UUID.randomUUID().toString())

            val nonce = UUID.randomUUID().toString()

            val captured = mutableListOf<ClassicHttpRequest>()
            val httpClient = mockk<HttpClient> {
                every { execute(capture(captured), any<HttpClientResponseHandler<Any>>()) } returns nonce andThen mockk<TokenResponse>()
            }
            val tokenEndpoint = URI("http://${UUID.randomUUID()}:8080/token")
            val openIdConfiguration = mockk<OpenIdConfiguration> {
                every { getTokenEndpoint() } returns tokenEndpoint
            }

            val configuration = Configuration(
                clientId = clientId,
                jwk = readJwkJson(),
                environment = environment,
            )
            HelseIdClient(
                configuration = configuration,
                httpClient = httpClient,
                openIdConfiguration = openIdConfiguration,
            ).getDpopAccessToken()

            captured shouldHaveSize 2
            with(captured.first()) {
                uri shouldBe tokenEndpoint

                val dpopHeaders = headerIterator("DPoP").asSequence().toList()
                dpopHeaders shouldHaveSize 1
                dpopHeaders.first().asClue {
                    SignedJWT.parse(it.value).asClue { jwt ->
                        jwt.header.algorithm shouldBe JWSAlgorithm.PS512
                        jwt.header.type shouldBe JOSEObjectType("dpop+jwt")
                        jwt.header.jwk shouldBe readJwk().toPublicJWK()

                        RSASSAVerifier(readJwk().toRSAKey()).verify(jwt.header, jwt.signingInput, jwt.signature) shouldBe true

                        jwt.jwtClaimsSet.claims shouldHaveSize 4
                        with(jwt.jwtClaimsSet) {
                            issueTime.toInstant() shouldBeBefore Instant.now()
                            jwtid shouldNot beNull()
                            getStringClaim("htm") shouldBe "POST"
                            getStringClaim("htu") shouldBe tokenEndpoint.toString()
                            getStringClaim("nonce") should beNull()
                            getStringClaim("ath") should beNull()
                        }
                    }
                }

                entity.contentType shouldBe "application/x-www-form-urlencoded; charset=UTF-8"

                WWWFormCodec.parse(entity.content.readAllBytes().decodeToString(), StandardCharsets.UTF_8).asClue { params ->
                    params shouldHaveSize 4
                    params shouldContain BasicNameValuePair("client_id", clientId)
                    params shouldContain BasicNameValuePair("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                    params shouldContain BasicNameValuePair("grant_type", "client_credentials")
                    params shouldHaveSingleElement { it.name == "client_assertion" }

                    val jwt = SignedJWT.parse(params.single { it.name == "client_assertion" }.value)
                    jwt.header.algorithm shouldBe JWSAlgorithm.PS512
                    jwt.header.keyID shouldBe "LJjagdyyRG-oj3bYLEf3kOt7im5ChDHe05DdiHUtqAA"
                    jwt.header.type shouldBe JOSEObjectType("client-authentication+jwt")

                    RSASSAVerifier(readJwk().toRSAKey()).verify(jwt.header, jwt.signingInput, jwt.signature) shouldBe true

                    jwt.jwtClaimsSet.claims shouldHaveSize 7
                    with(jwt.jwtClaimsSet) {
                        subject shouldBe clientId
                        issuer shouldBe clientId
                        audience shouldBe listOf(environment.audience)
                        issueTime.toInstant() shouldBeBefore Instant.now()
                        jwtid shouldNot beNull()
                        notBeforeTime.toInstant() shouldBeBefore Instant.now()
                        expirationTime.toInstant().shouldBeBetween(Instant.now().plusSeconds(55), Instant.now().plusSeconds(65))
                    }
                }
            }

            with(captured[1]) {
                uri shouldBe tokenEndpoint

                val dpopHeaders = headerIterator("DPoP").asSequence().toList()
                dpopHeaders shouldHaveSize 1
                dpopHeaders.first().asClue {
                    SignedJWT.parse(it.value).asClue { jwt ->
                        jwt.header.algorithm shouldBe JWSAlgorithm.PS512
                        jwt.header.type shouldBe JOSEObjectType("dpop+jwt")
                        jwt.header.jwk shouldBe readJwk().toPublicJWK()

                        RSASSAVerifier(readJwk().toRSAKey()).verify(jwt.header, jwt.signingInput, jwt.signature) shouldBe true

                        jwt.jwtClaimsSet.claims shouldHaveSize 5
                        with(jwt.jwtClaimsSet) {
                            issueTime.toInstant() shouldBeBefore Instant.now()
                            jwtid shouldNot beNull()
                            getStringClaim("htm") shouldBe "POST"
                            getStringClaim("htu") shouldBe tokenEndpoint.toString()
                            getStringClaim("nonce") shouldBe nonce
                            getStringClaim("ath") should beNull()
                        }
                    }
                }

                entity.contentType shouldBe "application/x-www-form-urlencoded; charset=UTF-8"

                WWWFormCodec.parse(entity.content.readAllBytes().decodeToString(), StandardCharsets.UTF_8).asClue { params ->
                    params shouldHaveSize 4
                    params shouldContain BasicNameValuePair("client_id", clientId)
                    params shouldContain BasicNameValuePair("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                    params shouldContain BasicNameValuePair("grant_type", "client_credentials")
                    params shouldHaveSingleElement { it.name == "client_assertion" }

                    val jwt = SignedJWT.parse(params.single { it.name == "client_assertion" }.value)
                    jwt.header.algorithm shouldBe JWSAlgorithm.PS512
                    jwt.header.keyID shouldBe "LJjagdyyRG-oj3bYLEf3kOt7im5ChDHe05DdiHUtqAA"
                    jwt.header.type shouldBe JOSEObjectType("client-authentication+jwt")

                    RSASSAVerifier(readJwk().toRSAKey()).verify(jwt.header, jwt.signingInput, jwt.signature) shouldBe true

                    jwt.jwtClaimsSet.claims shouldHaveSize 7
                    with(jwt.jwtClaimsSet) {
                        subject shouldBe clientId
                        issuer shouldBe clientId
                        audience shouldBe listOf(environment.audience)
                        issueTime.toInstant() shouldBeBefore Instant.now()
                        jwtid shouldNot beNull()
                        notBeforeTime.toInstant() shouldBeBefore Instant.now()
                        expirationTime.toInstant().shouldBeBetween(Instant.now().plusSeconds(55), Instant.now().plusSeconds(65))
                    }
                }
            }
        }

        "The access token should be cached according to config" {
            val clientId = UUID.randomUUID().toString()
            val environment = Environment("http://localhost:8080/api/token", UUID.randomUUID().toString())

            val httpClient = mockk<HttpClient> {
                every { execute(any(), any<HttpClientResponseHandler<Any>>()) } returns UUID.randomUUID().toString() andThen mockk<TokenResponse>() andThen UUID.randomUUID().toString() andThen mockk<TokenResponse>()
            }
            val tokenEndpoint = URI("http://${UUID.randomUUID()}:8080/token")
            val openIdConfiguration = mockk<OpenIdConfiguration> {
                every { getTokenEndpoint() } returns tokenEndpoint
            }

            val client = HelseIdClient(
                configuration = Configuration(
                    clientId = clientId,
                    jwk = readJwkJson(),
                    environment = environment,
                    accessTokenLifetime = Duration.ofSeconds(1),
                    accessTokenRenewalThreshold = Duration.ofMillis(300), // Will be renewed after 700 ms
                ),
                httpClient = httpClient,
                openIdConfiguration = openIdConfiguration,
            )

            val start = Instant.now()
            while (start.plusMillis(600).isAfter(Instant.now())) {
                client.getDpopAccessToken()
            }

            verify(exactly = 2) { httpClient.execute(any(), any<HttpClientResponseHandler<TokenResponse>>()) }

            while (start.plusMillis(1000).isAfter(Instant.now())) {
                client.getDpopAccessToken()
            }

            verify(exactly = 4) { httpClient.execute(any(), any<HttpClientResponseHandler<TokenResponse>>()) }
        }
    }

})

private fun readJwk() = JWK.parse(readJwkJson())

private fun readJwkJson() = HelseIdClientTest::class.java.classLoader.getResourceAsStream("jwk.json")!!.readAllBytes().decodeToString()
