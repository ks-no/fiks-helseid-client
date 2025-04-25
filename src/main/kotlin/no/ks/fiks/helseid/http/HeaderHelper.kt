package no.ks.fiks.helseid.http

private object Scheme {
    const val BEARER = "Bearer"
    const val DPOP = "DPoP"
}

object HeaderHelper {

    fun setHeaders(
        accessToken: String,
        setHeaderFunction: (headerName: String, headerValue: String) -> Any,
    ) {
        setHeaderFunction.invoke(Headers.AUTHORIZATION, buildAuthorizationHeaderValue(accessToken))
    }

    private fun buildAuthorizationHeaderValue(accessToken: String) = "${Scheme.BEARER} $accessToken"

    fun setHeaders(
        accessToken: String,
        dpopProof: String,
        setHeaderFunction: (headerName: String, headerValue: String) -> Any,
    ) {
        setHeaderFunction.invoke(Headers.AUTHORIZATION, buildDpopAuthorizationHeaderValue(accessToken))
        setHeaderFunction.invoke(Headers.DPOP, dpopProof)
    }

    private fun buildDpopAuthorizationHeaderValue(accessToken: String) = "${Scheme.DPOP} $accessToken"

}