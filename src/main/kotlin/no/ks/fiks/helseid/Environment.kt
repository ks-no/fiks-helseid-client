package no.ks.fiks.helseid


class Environment(
    val issuer: String,
    val audience: String,
) {
    companion object {
        val TEST = Environment(
            issuer = "https://helseid-sts.test.nhn.no",
            audience = "https://helseid-sts.test.nhn.no",
        )

        val PROD = Environment(
            issuer = "https://helseid-sts.nhn.no",
            audience = "https://helseid-sts.nhn.no",
        )
    }
}
