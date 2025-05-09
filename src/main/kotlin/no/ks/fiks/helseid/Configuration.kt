package no.ks.fiks.helseid

import java.time.Duration

class Configuration(
    val clientId: String,
    val jwk: String,
    val environment: Environment,
    val accessTokenLifetime: Duration = Duration.ofSeconds(60),
    val accessTokenRenewalThreshold: Duration = Duration.ofSeconds(10),
)


