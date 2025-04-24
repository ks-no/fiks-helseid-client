package no.ks.fiks.helseid.http

private const val HEADER_AUTHORIZATION = "Authorization"
private const val PREFIX_BEARER = "Bearer"

private const val HEADER_DPOP = "DPoP"
private const val PREFIX_DPOP = "DPoP"

object HeaderHelper {

    fun setHeaders(
        accessToken: String,
        setHeaderFunction: (headerName: String, headerValue: String) -> Any,
    ) {
        setHeaderFunction.invoke(HEADER_AUTHORIZATION, buildAuthorizationHeaderValue(accessToken))
    }

    private fun buildAuthorizationHeaderValue(accessToken: String) = "$PREFIX_BEARER $accessToken"

    fun setHeaders(
        accessToken: String,
        dpopProof: String,
        setHeaderFunction: (headerName: String, headerValue: String) -> Any,
    ) {
        setHeaderFunction.invoke(HEADER_AUTHORIZATION, buildDpopAuthorizationHeaderValue(accessToken))
        setHeaderFunction.invoke(HEADER_DPOP, dpopProof)
    }

    private fun buildDpopAuthorizationHeaderValue(accessToken: String) = "$PREFIX_DPOP $accessToken"

}