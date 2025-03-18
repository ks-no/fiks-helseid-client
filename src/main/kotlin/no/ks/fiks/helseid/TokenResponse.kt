package no.ks.fiks.helseid

data class TokenResponse(
    val accessToken: String,
    val expiresIn: Int,
    val tokenType: String,
    val scope: String,
)
