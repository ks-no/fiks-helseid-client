package no.ks.fiks.helseid.builder

import no.ks.fiks.helseid.Environment
import no.ks.fiks.helseid.Configuration
import java.time.Duration

class ConfigurationBuilder {

    private var clientId: String? = null
    private var jwk: String? = null
    private var environment: Environment? = null
    private var jwtRequestExpirationTime: Duration? = null

    fun clientId(clientId: String?) = this.also { this.clientId = clientId }
    fun jwk(jwk: String?) = this.also { this.jwk = jwk }
    fun environment(environment: Environment?) = this.also { this.environment = environment }
    fun jwtRequestExpirationTime(time: Duration?) = this.also { this.jwtRequestExpirationTime = time}

    fun build() = Configuration(
        clientId = clientId ?: throw IllegalArgumentException("clientId is required"),
        jwk = jwk ?: throw IllegalArgumentException("jwk is required"),
        environment = environment ?: throw IllegalArgumentException("environment is required"),
        jwtRequestExpirationTime = jwtRequestExpirationTime ?: Duration.ofSeconds(60),
    )

}