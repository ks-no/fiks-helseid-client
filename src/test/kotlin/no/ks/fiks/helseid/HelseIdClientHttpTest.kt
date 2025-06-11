package no.ks.fiks.helseid

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.throwable.shouldHaveMessage
import no.ks.fiks.helseid.http.HttpException
import org.wiremock.integrations.testcontainers.WireMockContainer
import java.util.*


class HelseIdClientHttpTest : FreeSpec() {

    private val successClient = UUID.randomUUID().toString()
    private val badRequestClient = UUID.randomUUID().toString()
    private val internalServerErrorClient = UUID.randomUUID().toString()

    private val dpopClient = UUID.randomUUID().toString()
    private val dpopErrorClient = UUID.randomUUID().toString()
    private val dpopWithoutNonceClient = UUID.randomUUID().toString()
    private val nonce = UUID.randomUUID().toString()

    private val wireMock = WireMockContainer("wiremock/wiremock:3.12.1")
        .withMappingFromJSON(createOkMappingJson(successClient))
        .withMappingFromJSON(createErrorMappingJson(badRequestClient, 400, "invalid_client"))
        .withMappingFromJSON(createErrorMappingJson(internalServerErrorClient, 500, "Something went wrong!"))
        .withMappingFromJSON(createDpopOkMappingJson(dpopClient, nonce))
        .withMappingFromJSON(createDpopErrorMappingJson(dpopErrorClient, nonce))
        .withMappingFromJSON(createDpopWithoutNonceMappingJson(dpopWithoutNonceClient))
        .also {
            it.start()
            it.setupOpenIdConfigurationMapping() // Needs the container to be started to get the dynamic port
        }

    private fun WireMockContainer.setupOpenIdConfigurationMapping() {
        WireMock.configureFor(port)
        WireMock.stubFor(
            get("/.well-known/openid-configuration")
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "issuer": "$baseUrl",
                                    "jwks_uri":"$baseUrl/jwks",
                                    "token_endpoint": "$baseUrl/token",
                                    "subject_types_supported": [ "public" ]
                                }
                            """.trimIndent())
                )
        )
    }

    init {
        "Bearer token" - {
            "Exception should not be thrown for success code" {
                HelseIdClient(
                    configuration = Configuration(
                        clientId = successClient,
                        jwk = RSAKeyGenerator(2048).generate().toRSAKey().toString(),
                        environment = Environment(wireMock.baseUrl, UUID.randomUUID().toString()),
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
                            jwk = RSAKeyGenerator(2048).generate().toRSAKey().toString(),
                            environment = Environment(wireMock.baseUrl, UUID.randomUUID().toString()),
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
                            jwk = RSAKeyGenerator(2048).generate().toRSAKey().toString(),
                            environment = Environment(wireMock.baseUrl, UUID.randomUUID().toString()),
                        ),
                    ).getAccessToken()
                }.asClue {
                    it.status shouldBe 500
                    it.body shouldBe """{"error":"Something went wrong!"}"""
                }
            }
        }

        "DPoP token" - {
            "Exception should not be thrown for bad request with use_dpop_nonce error followed by ok" {
                HelseIdClient(
                    configuration = Configuration(
                        clientId = dpopClient,
                        jwk = RSAKeyGenerator(2048).generate().toRSAKey().toString(),
                        environment = Environment(wireMock.baseUrl, UUID.randomUUID().toString()),
                    ),
                ).getAccessToken(AccessTokenRequestBuilder().tokenType(TokenType.DPOP).build()).asClue {
                    it.accessToken shouldNot beNull()
                    it.tokenType shouldBe "Bearer"
                    it.scope shouldBe "the_scope"
                    it.expiresIn shouldBe 123
                }
            }

            "Exception should be thrown if an error other than bad request is returned for the first request" {
                shouldThrow<HttpException> {
                    HelseIdClient(
                        configuration = Configuration(
                            clientId = internalServerErrorClient,
                            jwk = RSAKeyGenerator(2048).generate().toRSAKey().toString(),
                            environment = Environment(wireMock.baseUrl, UUID.randomUUID().toString()),
                        ),
                    ).getAccessToken(AccessTokenRequestBuilder().tokenType(TokenType.DPOP).build())
                }.asClue {
                    it.status shouldBe 500
                    it.body shouldBe """{"error":"Something went wrong!"}"""
                }
            }

            "Exception should be thrown if a bad request error with code other than use_dpop_nonce is returned for the first request" {
                shouldThrow<HttpException> {
                    HelseIdClient(
                        configuration = Configuration(
                            clientId = badRequestClient,
                            jwk = RSAKeyGenerator(2048).generate().toRSAKey().toString(),
                            environment = Environment(wireMock.baseUrl, UUID.randomUUID().toString()),
                        ),
                    ).getAccessToken(AccessTokenRequestBuilder().tokenType(TokenType.DPOP).build())
                }.asClue {
                    it.status shouldBe 400
                    it.body shouldBe """{"error":"invalid_client"}"""
                }
            }

            "Exception should be thrown if an error is returned for the second request" {
                shouldThrow<HttpException> {
                    HelseIdClient(
                        configuration = Configuration(
                            clientId = dpopErrorClient,
                            jwk = RSAKeyGenerator(2048).generate().toRSAKey().toString(),
                            environment = Environment(wireMock.baseUrl, UUID.randomUUID().toString()),
                        ),
                    ).getAccessToken(AccessTokenRequestBuilder().tokenType(TokenType.DPOP).build())
                }.asClue {
                    it.status shouldBe 500
                    it.body shouldBe "Something went wrong"
                }
            }

            "Exception should be thrown if nonce header is missing in first response" {
                shouldThrow<RuntimeException> {
                    HelseIdClient(
                        configuration = Configuration(
                            clientId = dpopWithoutNonceClient,
                            jwk = RSAKeyGenerator(2048).generate().toRSAKey().toString(),
                            environment = Environment(wireMock.baseUrl, UUID.randomUUID().toString()),
                        ),
                    ).getAccessToken(AccessTokenRequestBuilder().tokenType(TokenType.DPOP).build())
                }.asClue {
                    it shouldHaveMessage "Expected DPoP-Nonce header to be set"
                }
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

private fun createDpopOkMappingJson(clientId: String, nonce: String) = """
                {
                    "mappings": [
                        {
                            "scenarioName": "$clientId",
                            "requiredScenarioState": "Started",
                            "newScenarioState": "ok",
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
                                "status": 400,
                                "jsonBody": {
                                    "error": "use_dpop_nonce"
                                },
                                "headers": {
                                    "DPoP-Nonce": "$nonce"
                                }
                            }
                        },
                        {
                            "scenarioName": "$clientId",
                            "requiredScenarioState": "ok",
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
                    ]
                }
            """.trimIndent()

private fun createDpopErrorMappingJson(clientId: String, nonce: String) = """
                {
                    "mappings": [
                        {
                            "scenarioName": "$clientId",
                            "requiredScenarioState": "Started",
                            "newScenarioState": "ok",
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
                                "status": 400,
                                "jsonBody": {
                                    "error": "use_dpop_nonce"
                                },
                                "headers": {
                                    "DPoP-Nonce": "$nonce"
                                }
                            }
                        },
                        {
                            "scenarioName": "$clientId",
                            "requiredScenarioState": "ok",
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
                                "status": 500,
                                "body": "Something went wrong"
                            }
                        }
                    ]
                }
            """.trimIndent()

private fun createDpopWithoutNonceMappingJson(clientId: String) = """
                {
                    "mappings": [
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
                                "status": 400,
                                "jsonBody": {
                                    "error": "use_dpop_nonce"
                                }
                            }
                        }
                    ]
                }
            """.trimIndent()
