package no.ks.fiks.helseid.http

private const val SCHEME = "Bearer"

object HeaderHelper {

    fun setHeaders(
        accessToken: String,
        setHeaderFunction: (headerName: String, headerValue: String) -> Any,
    ) {
        setHeaderFunction.invoke(Headers.AUTHORIZATION, "$SCHEME $accessToken")
    }

}