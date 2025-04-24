package no.ks.fiks.helseid

import java.time.Duration

class Configuration(
    val clientId: String,
    val jwk: String,
    val environment: Environment,
    val jwtRequestExpirationTime: Duration = Duration.ofSeconds(60),
)


