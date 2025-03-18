package no.ks.fiks.helseid.builder

import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.ks.fiks.helseid.Environment
import no.ks.fiks.helseid.TokenResponse
import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.core5.http.io.HttpClientResponseHandler
import java.security.PrivateKey
import java.time.Duration
import java.util.*
import kotlin.random.Random.Default.nextLong

class BuilderTest : FreeSpec({

    "Configuration builder" - {
        "clientId should be required" {
            shouldThrow<IllegalArgumentException> {
                ConfigurationBuilder()
                    .build()
            } shouldHaveMessage "clientId is required"
        }

        "privateKey should be required" {
            shouldThrow<IllegalArgumentException> {
                ConfigurationBuilder()
                    .clientId(UUID.randomUUID().toString())
                    .build()
            } shouldHaveMessage "privateKey is required"
        }

        "keyId should be required" {
            shouldThrow<IllegalArgumentException> {
                ConfigurationBuilder()
                    .clientId(UUID.randomUUID().toString())
                    .privateKey(mockk())
                    .build()
            } shouldHaveMessage "keyId is required"
        }

        "environment should be required" {
            shouldThrow<IllegalArgumentException> {
                ConfigurationBuilder()
                    .clientId(UUID.randomUUID().toString())
                    .privateKey(mockk())
                    .keyId(UUID.randomUUID().toString())
                    .build()
            } shouldHaveMessage "environment is required"
        }

        "Build with minimal config" {
            val clientId = UUID.randomUUID().toString()
            val privateKey = mockk<PrivateKey>()
            val keyId = UUID.randomUUID().toString()
            val environment = Environment(UUID.randomUUID().toString(), UUID.randomUUID().toString())

            ConfigurationBuilder()
                .clientId(clientId)
                .privateKey(privateKey)
                .keyId(keyId)
                .environment(environment)
                .build()
                .asClue {
                    it.clientId shouldBe clientId
                    it.privateKey shouldBe privateKey
                    it.keyId shouldBe keyId
                    it.environment shouldBe environment
                    it.jwtRequestExpirationTime shouldBe Duration.ofSeconds(60)
                }
        }

        "Build with full config" {
            val clientId = UUID.randomUUID().toString()
            val privateKey = mockk<PrivateKey>()
            val keyId = UUID.randomUUID().toString()
            val environment = Environment(UUID.randomUUID().toString(), UUID.randomUUID().toString())
            val expirationTime = Duration.ofSeconds(nextLong(1, 60))

            ConfigurationBuilder()
                .clientId(clientId)
                .privateKey(privateKey)
                .keyId(keyId)
                .environment(environment)
                .jwtRequestExpirationTime(expirationTime)
                .build()
                .asClue {
                    it.clientId shouldBe clientId
                    it.privateKey shouldBe privateKey
                    it.keyId shouldBe keyId
                    it.environment shouldBe environment
                    it.jwtRequestExpirationTime shouldBe expirationTime
                }
        }
    }

    "Client builder" - {
        "configuration should be required" {
            shouldThrow<IllegalArgumentException> {
                HelseIdClientBuilder()
                    .build()
            } shouldHaveMessage "configuration is required"
        }

        "Build with configuration" {
            val privateKey = RSAKeyGenerator(2048).generate().toPrivateKey()
            val httpClient = mockk<HttpClient> {
                every { execute(any(), any<HttpClientResponseHandler<TokenResponse>>()) } returns mockk()
            }

            HelseIdClientBuilder()
                .configuration(
                    ConfigurationBuilder()
                        .clientId(UUID.randomUUID().toString())
                        .privateKey(privateKey)
                        .keyId(UUID.randomUUID().toString())
                        .environment(Environment(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                        .build()
                )
                .httpClient(httpClient)
                .build()
                .getAccessToken()

            verify(exactly = 1) {
                httpClient.execute(any(), any<HttpClientResponseHandler<TokenResponse>>())
            }
        }

    }

})