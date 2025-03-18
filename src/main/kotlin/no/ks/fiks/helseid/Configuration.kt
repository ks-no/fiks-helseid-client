package no.ks.fiks.helseid

import java.security.PrivateKey
import java.time.Duration

class Configuration(
    val clientId: String,
    val privateKey: PrivateKey,
    val keyId: String,
    val environment: Environment,
    val jwtRequestExpirationTime: Duration = Duration.ofSeconds(60),
)


