package no.ks.fiks.helseid.http

import no.ks.fiks.helseid.*
import no.ks.fiks.helseid.dpop.Endpoint
import no.ks.fiks.helseid.dpop.ProofBuilder

class DpopHttpRequestHelper(
    private val helseIdClient: HelseIdClient,
    private val proofBuilder: ProofBuilder,
) {

    constructor(configuration: Configuration): this(
        HelseIdClient(configuration = configuration),
        ProofBuilder(configuration = configuration),
    )

    fun addAuthorizationHeader(
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
        DpopHeaderHelper.setHeaders(accessToken, dpopProof, setHeaderFunction)
    }

}