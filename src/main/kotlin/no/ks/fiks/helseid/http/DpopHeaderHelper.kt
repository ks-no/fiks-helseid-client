package no.ks.fiks.helseid.http

private const val SCHEME = "DPoP"

object DpopHeaderHelper {

    fun setHeaders(
        accessToken: String,
        dpopProof: String,
        setHeaderFunction: (headerName: String, headerValue: String) -> Any,
    ) {
        setHeaderFunction.invoke(Headers.AUTHORIZATION, "$SCHEME $accessToken")
        setHeaderFunction.invoke(Headers.DPOP, dpopProof)
    }

}