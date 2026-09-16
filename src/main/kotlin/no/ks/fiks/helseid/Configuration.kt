package no.ks.fiks.helseid

import java.time.Duration

class Configuration(
    val clientId: String,
    val jwk: String,
    val environment: Environment,
    val accessTokenRenewalThreshold: Duration = Duration.ofSeconds(10),
)

class ConfigurationBuilder {

    private var clientId: String? = null
    private var jwk: String? = null
    private var environment: Environment? = null
    private var accessTokenRenewalThreshold: Duration? = null

    fun clientId(clientId: String?) = this.also { this.clientId = clientId }
    fun jwk(jwk: String?) = this.also { this.jwk = jwk }
    fun environment(environment: Environment?) = this.also { this.environment = environment }
    fun accessTokenRenewalThreshold(time: Duration?) = this.also { this.accessTokenRenewalThreshold = time}

    fun build() = Configuration(
        clientId = clientId ?: throw IllegalArgumentException("clientId is required"),
        jwk = jwk ?: throw IllegalArgumentException("jwk is required"),
        environment = environment ?: throw IllegalArgumentException("environment is required"),
        accessTokenRenewalThreshold = accessTokenRenewalThreshold ?: Duration.ofSeconds(10),
    )

}
