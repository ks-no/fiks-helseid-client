package no.ks.fiks.helseid.http

import no.ks.fiks.helseid.*
import no.ks.fiks.helseid.dpop.Endpoint
import no.ks.fiks.helseid.dpop.ProofBuilder

class HttpRequestHelper(configuration: Configuration) {

    private val helseIdClient = HelseIdClient(configuration = configuration)
    private val proofBuilder = ProofBuilder(configuration = configuration)

    fun addAuthorizationHeader(
        accessTokenRequestBuilder: AccessTokenRequestBuilder = AccessTokenRequestBuilder(),
        setHeaderFunction: (headerName: String, headerValue: String) -> Any,
    ) {
        val accessToken = helseIdClient.getAccessToken(accessTokenRequestBuilder.tokenType(TokenType.BEARER).build()).accessToken
        HeaderHelper.setHeaders(accessToken, setHeaderFunction)
    }

    fun addDpopAuthorizationHeader(
        endpoint: Endpoint,
        accessTokenRequestBuilder: AccessTokenRequestBuilder = AccessTokenRequestBuilder(),
        setHeaderFunction: (headerName: String, headerValue: String) -> Any,
    ) {
        val accessToken = helseIdClient
            .getAccessToken(
                accessTokenRequestBuilder
                    .tokenType(TokenType.DPOP)
                    .build()
            )
            .accessToken
        val dpopProof = proofBuilder.buildProof(endpoint, accessToken = accessToken)
        HeaderHelper.setHeaders(accessToken, dpopProof, setHeaderFunction)
    }

}