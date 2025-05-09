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

        "jwk should be required" {
            shouldThrow<IllegalArgumentException> {
                ConfigurationBuilder()
                    .clientId(UUID.randomUUID().toString())
                    .build()
            } shouldHaveMessage "jwk is required"
        }

        "environment should be required" {
            shouldThrow<IllegalArgumentException> {
                ConfigurationBuilder()
                    .clientId(UUID.randomUUID().toString())
                    .jwk(UUID.randomUUID().toString())
                    .build()
            } shouldHaveMessage "environment is required"
        }

        "Build with minimal config" {
            val clientId = UUID.randomUUID().toString()
            val jwk = UUID.randomUUID().toString()
            val environment = Environment(UUID.randomUUID().toString(), UUID.randomUUID().toString())

            ConfigurationBuilder()
                .clientId(clientId)
                .jwk(jwk)
                .environment(environment)
                .build()
                .asClue {
                    it.clientId shouldBe clientId
                    it.jwk shouldBe jwk
                    it.environment shouldBe environment
                    it.accessTokenLifetime shouldBe Duration.ofSeconds(60)
                    it.accessTokenRenewalThreshold shouldBe Duration.ofSeconds(10)
                }
        }

        "Build with full config" {
            val clientId = UUID.randomUUID().toString()
            val jwk = UUID.randomUUID().toString()
            val environment = Environment(UUID.randomUUID().toString(), UUID.randomUUID().toString())
            val expirationTime = Duration.ofSeconds(nextLong(1, 60))
            val renewalThreshold = Duration.ofSeconds(nextLong(1, 60))

            ConfigurationBuilder()
                .clientId(clientId)
                .jwk(jwk)
                .environment(environment)
                .accessTokenLifetime(expirationTime)
                .accessTokenRenewalThreshold(renewalThreshold)
                .build()
                .asClue {
                    it.clientId shouldBe clientId
                    it.jwk shouldBe jwk
                    it.environment shouldBe environment
                    it.accessTokenLifetime shouldBe expirationTime
                    it.accessTokenRenewalThreshold shouldBe renewalThreshold
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
            val jwk = RSAKeyGenerator(2048).generate().toRSAKey().toString()
            val httpClient = mockk<HttpClient> {
                every { execute(any(), any<HttpClientResponseHandler<TokenResponse>>()) } returns mockk()
            }

            HelseIdClientBuilder()
                .configuration(
                    ConfigurationBuilder()
                        .clientId(UUID.randomUUID().toString())
                        .jwk(jwk)
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