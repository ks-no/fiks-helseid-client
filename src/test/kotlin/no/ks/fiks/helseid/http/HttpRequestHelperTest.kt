package no.ks.fiks.helseid.http

import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.ks.fiks.helseid.*
import java.util.*

class HttpRequestHelperTest : StringSpec({

    "Should be able to build client from configuration" {
        HttpRequestHelper(Configuration("", RSAKeyGenerator(2048).generate().toRSAKey().toString(), Environment("", "")))
    }

    "Should call client to get token with default parameters" {
        val client = mockk<HelseIdClient> { every { getAccessToken(any()) } returns TokenResponse(UUID.randomUUID().toString(), 0, "", "") }

        HttpRequestHelper(client).addAuthorizationHeader { _, _ -> }

        verify(exactly = 1) { client.getAccessToken(StandardAccessTokenRequest(TokenType.BEARER)) }
    }

    "Should call client to get token with passed parameters" {
        val client = mockk<HelseIdClient> { every { getAccessToken(any()) } returns TokenResponse(UUID.randomUUID().toString(), 0, "", "") }
        val parentOrganizationNumber = randomOrganizationNumber()
        val childOrganizationNumber = randomOrganizationNumber()
        val request = AccessTokenRequestBuilder()
            .tokenType(TokenType.BEARER)
            .tenancyType(TenancyType.MULTI)
            .parentOrganizationNumber(parentOrganizationNumber)
            .childOrganizationNumber(childOrganizationNumber)

        HttpRequestHelper(client).addAuthorizationHeader(request) { _, _ -> }

        verify(exactly = 1) { client.getAccessToken(MultiTenantOrganizationNumberAccessTokenRequest(parentOrganizationNumber, childOrganizationNumber, TokenType.BEARER)) }
    }

    "Should force request token type to BEARER" {
        val client = mockk<HelseIdClient> { every { getAccessToken(any()) } returns TokenResponse(UUID.randomUUID().toString(), 0, "", "") }
        val childOrganizationNumber = randomOrganizationNumber()
        val request = AccessTokenRequestBuilder()
            .tokenType(TokenType.DPOP)
            .tenancyType(TenancyType.SINGLE)
            .childOrganizationNumber(childOrganizationNumber)

        HttpRequestHelper(client).addAuthorizationHeader(request) { _, _ -> }

        verify(exactly = 1) { client.getAccessToken(SingleTenantOrganizationNumberAccessTokenRequest(childOrganizationNumber, TokenType.BEARER)) }
    }

    "Should pass expected name and value" {
        val accessToken = UUID.randomUUID().toString()
        val client = mockk<HelseIdClient> { every { getAccessToken(any()) } returns TokenResponse(accessToken, 0, "", "") }

        val parameters = mutableListOf<Pair<String, String>>()
        HttpRequestHelper(client).addAuthorizationHeader { headerName, headerValue ->
            parameters.add(headerName to headerValue)
        }

        parameters shouldContainExactly listOf(
            parameters[0] shouldBe Pair("Authorization", "Bearer $accessToken"),
        )
    }

})