package no.ks.fiks.helseid

import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import org.wiremock.integrations.testcontainers.WireMockContainer
import java.util.*


class HelseIdClientHttpTest : StringSpec() {

    private val successClient = UUID.randomUUID().toString()
    private val badRequestClient = UUID.randomUUID().toString()
    private val internalServerErrorClient = UUID.randomUUID().toString()

    private val wireMock = WireMockContainer("wiremock/wiremock:3.12.1")
        .withMappingFromJSON(createOkMappingJson(successClient))
        .withMappingFromJSON(createErrorMappingJson(badRequestClient, 400, "invalid_client"))
        .withMappingFromJSON(createErrorMappingJson(internalServerErrorClient, 500, "Something went wrong!"))
        .also {
            it.start()
        }

    init {
        "Exception should not be thrown for success code" {
            HelseIdClient(
                configuration = Configuration(
                    clientId = successClient,
                    privateKey = RSAKeyGenerator(2048).generate().toPrivateKey(),
                    keyId = UUID.randomUUID().toString(),
                    environment = Environment("${wireMock.baseUrl}/token", UUID.randomUUID().toString()),
                ),
            ).getAccessToken().asClue {
                it.accessToken shouldNot beNull()
                it.tokenType shouldBe "Bearer"
                it.scope shouldBe "the_scope"
                it.expiresIn shouldBe 123
            }
        }
        "Exception should be thrown for client error code" {
            shouldThrow<HttpException> {
                HelseIdClient(
                    configuration = Configuration(
                        clientId = badRequestClient,
                        privateKey = RSAKeyGenerator(2048).generate().toPrivateKey(),
                        keyId = UUID.randomUUID().toString(),
                        environment = Environment("${wireMock.baseUrl}/token", UUID.randomUUID().toString()),
                    ),
                ).getAccessToken()
            }.asClue {
                it.status shouldBe 400
                it.body shouldBe """{"error":"invalid_client"}"""
            }
        }

        "Exception should be thrown for server error code" {
            shouldThrow<HttpException> {
                HelseIdClient(
                    configuration = Configuration(
                        clientId = internalServerErrorClient,
                        privateKey = RSAKeyGenerator(2048).generate().toPrivateKey(),
                        keyId = UUID.randomUUID().toString(),
                        environment = Environment("${wireMock.baseUrl}/token", UUID.randomUUID().toString()),
                    ),
                ).getAccessToken()
            }.asClue {
                it.status shouldBe 500
                it.body shouldBe """{"error":"Something went wrong!"}"""
            }
        }
    }
}

private fun createOkMappingJson(clientId: String) = """
                {
                    "request": {
                        "method": "POST",
                        "url": "/token",
                        "bodyPatterns": [
                            {
                                "contains": "$clientId"
                            }
                        ]
                    },
                    "response": {
                        "status": 200,
                        "jsonBody": {
                            "access_token": "${UUID.randomUUID()}",
                            "token_type": "Bearer",
                            "scope": "the_scope",
                            "expires_in": 123
                        }
                    }
                }
            """.trimIndent()

private fun createErrorMappingJson(clientId: String, code: Int, error: String) = """
                {
                    "request": {
                        "method": "POST",
                        "url": "/token",
                        "bodyPatterns": [
                            {
                                "contains": "$clientId"
                            }
                        ]
                    },
                    "response": {
                        "status": $code,
                        "jsonBody": {
                            "error": "$error"
                        }
                    }
                }
            """.trimIndent()
