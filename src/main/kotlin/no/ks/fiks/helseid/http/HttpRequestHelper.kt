package no.ks.fiks.helseid.http

import no.ks.fiks.helseid.AccessTokenRequestBuilder
import no.ks.fiks.helseid.Configuration
import no.ks.fiks.helseid.HelseIdClient
import no.ks.fiks.helseid.TokenType

class HttpRequestHelper(
    private val helseIdClient: HelseIdClient,
) {

    constructor(configuration: Configuration) : this(
        HelseIdClient(configuration = configuration),
    )

    fun addAuthorizationHeader(
        accessTokenRequestBuilder: AccessTokenRequestBuilder = AccessTokenRequestBuilder(),
        setHeaderFunction: (headerName: String, headerValue: String) -> Any,
    ) {
        val accessToken = helseIdClient.getAccessToken(accessTokenRequestBuilder.tokenType(TokenType.BEARER).build()).accessToken
        HeaderHelper.setHeaders(accessToken, setHeaderFunction)
    }

}