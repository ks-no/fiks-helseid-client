package no.ks.fiks.helseid.builder

import no.ks.fiks.helseid.Environment
import no.ks.fiks.helseid.Configuration
import java.time.Duration

class ConfigurationBuilder {

    private var clientId: String? = null
    private var jwk: String? = null
    private var environment: Environment? = null
    private var accessTokenLifetime: Duration? = null
    private var accessTokenRenewalThreshold: Duration? = null

    fun clientId(clientId: String?) = this.also { this.clientId = clientId }
    fun jwk(jwk: String?) = this.also { this.jwk = jwk }
    fun environment(environment: Environment?) = this.also { this.environment = environment }
    fun accessTokenLifetime(time: Duration?) = this.also { this.accessTokenLifetime = time}
    fun accessTokenRenewalThreshold(time: Duration?) = this.also { this.accessTokenRenewalThreshold = time}

    fun build() = Configuration(
        clientId = clientId ?: throw IllegalArgumentException("clientId is required"),
        jwk = jwk ?: throw IllegalArgumentException("jwk is required"),
        environment = environment ?: throw IllegalArgumentException("environment is required"),
        accessTokenLifetime = accessTokenLifetime ?: Duration.ofSeconds(60),
        accessTokenRenewalThreshold = accessTokenRenewalThreshold ?: Duration.ofSeconds(10),
    )

}