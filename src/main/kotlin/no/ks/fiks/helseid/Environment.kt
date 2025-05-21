package no.ks.fiks.helseid


class Environment(
    val url: String,
    val audience: String,
) {
    companion object {
        val TEST = Environment(
            url = "https://helseid-sts.test.nhn.no",
            audience = "https://helseid-sts.test.nhn.no",
        )

        val PROD = Environment(
            url = "https://helseid-sts.nhn.no",
            audience = "https://helseid-sts.nhn.no",
        )
    }
}
