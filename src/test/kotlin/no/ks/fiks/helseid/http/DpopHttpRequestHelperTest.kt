package no.ks.fiks.helseid.http

import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.*
import no.ks.fiks.helseid.*
import no.ks.fiks.helseid.dpop.Endpoint
import no.ks.fiks.helseid.dpop.HttpMethod
import no.ks.fiks.helseid.dpop.ProofBuilder
import java.util.*

class DpopHttpRequestHelperTest : StringSpec({

    "Should be able to build client from configuration" {
        DpopHttpRequestHelper(Configuration("", RSAKeyGenerator(2048).generate().toRSAKey().toString(), Environment("", "")))
    }

    "Should call client to get token with default parameters" {
        val accessToken = UUID.randomUUID().toString()
        val client = mockk<HelseIdClient> { every { getAccessToken(any()) } returns TokenResponse(accessToken, 0, "", "") }
        val proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() }

        val endpoint = Endpoint(HttpMethod.entries.random(), UUID.randomUUID().toString())
        DpopHttpRequestHelper(client, proofBuilder).addAuthorizationHeader(endpoint) { _, _ -> }

        verifySequence {
            client.getAccessToken(StandardAccessTokenRequest(TokenType.DPOP))
            proofBuilder.buildProof(endpoint, null, accessToken)
        }
    }

    "Should call client to get token with passed parameters" {
        val accessToken = UUID.randomUUID().toString()
        val client = mockk<HelseIdClient> { every { getAccessToken(any()) } returns TokenResponse(accessToken, 0, "", "") }
        val proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() }

        val childOrganizationNumber = randomOrganizationNumber()
        val request = AccessTokenRequestBuilder()
            .tokenType(TokenType.DPOP)
            .tenancyType(TenancyType.SINGLE)
            .childOrganizationNumber(childOrganizationNumber)

        val endpoint = Endpoint(HttpMethod.entries.random(), UUID.randomUUID().toString())
        DpopHttpRequestHelper(client, proofBuilder).addAuthorizationHeader(endpoint, request) { _, _ -> }

        verifySequence {
            client.getAccessToken(SingleTenantOrganizationNumberAccessTokenRequest(childOrganizationNumber, TokenType.DPOP))
            proofBuilder.buildProof(endpoint, null, accessToken)
        }
    }

    "Should force request token type to DPOP" {
        val accessToken = UUID.randomUUID().toString()
        val client = mockk<HelseIdClient> { every { getAccessToken(any()) } returns TokenResponse(accessToken, 0, "", "") }
        val proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns UUID.randomUUID().toString() }

        val parentOrganizationNumber = randomOrganizationNumber()
        val childOrganizationNumber = randomOrganizationNumber()
        val request = AccessTokenRequestBuilder()
            .tokenType(TokenType.BEARER)
            .tenancyType(TenancyType.MULTI)
            .parentOrganizationNumber(parentOrganizationNumber)
            .childOrganizationNumber(childOrganizationNumber)

        val endpoint = Endpoint(HttpMethod.entries.random(), UUID.randomUUID().toString())
        DpopHttpRequestHelper(client, proofBuilder).addAuthorizationHeader(endpoint, request) { _, _ -> }

        verifySequence {
            client.getAccessToken(MultiTenantOrganizationNumberAccessTokenRequest(parentOrganizationNumber, childOrganizationNumber, TokenType.DPOP))
            proofBuilder.buildProof(endpoint, null, accessToken)
        }
    }

    "Should pass expected names and values" {
        val accessToken = UUID.randomUUID().toString()
        val dpopProof = UUID.randomUUID().toString()
        val client = mockk<HelseIdClient> { every { getAccessToken(any()) } returns TokenResponse(accessToken, 0, "", "") }
        val proofBuilder = mockk<ProofBuilder> { every { buildProof(any(), any(), any()) } returns dpopProof }

        val parameters = mutableListOf<Pair<String, String>>()
        val endpoint = Endpoint(HttpMethod.entries.random(), UUID.randomUUID().toString())
        DpopHttpRequestHelper(client, proofBuilder).addAuthorizationHeader(endpoint) { headerName, headerValue ->
            parameters.add(headerName to headerValue)
        }

        parameters shouldContainExactly listOf(
            parameters[0] shouldBe Pair("Authorization", "DPoP $accessToken"),
            parameters[1] shouldBe Pair("DPoP", dpopProof),
        )
    }

})