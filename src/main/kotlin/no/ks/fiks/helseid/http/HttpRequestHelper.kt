package no.ks.fiks.helseid.http

import no.ks.fiks.helseid.Configuration
import no.ks.fiks.helseid.HelseIdClient
import no.ks.fiks.helseid.dpop.Endpoint
import no.ks.fiks.helseid.dpop.ProofBuilder

class HttpRequestHelper(configuration: Configuration) {

    private val helseIdClient = HelseIdClient(configuration = configuration)
    private val proofBuilder = ProofBuilder(configuration = configuration)

    fun addAuthorizationHeader(
        setHeaderFunction: (headerName: String, headerValue: String) -> Any,
    ) {
        val accessToken = helseIdClient.getAccessToken().accessToken
        HeaderHelper.setHeaders(accessToken, setHeaderFunction)
    }

    fun addDpopAuthorizationHeader(
        endpoint: Endpoint,
        setHeaderFunction: (headerName: String, headerValue: String) -> Any,
    ) {
        val accessToken = helseIdClient.getDpopAccessToken().accessToken
        val dpopProof = proofBuilder.buildProof(endpoint, accessToken = accessToken)
        HeaderHelper.setHeaders(accessToken, dpopProof, setHeaderFunction)
    }

}