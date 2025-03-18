package no.ks.fiks.helseid.builder

import no.ks.fiks.helseid.Environment
import no.ks.fiks.helseid.Configuration
import java.security.PrivateKey
import java.time.Duration

class ConfigurationBuilder {

    private var clientId: String? = null
    private var privateKey: PrivateKey? = null
    private var keyId: String? = null
    private var environment: Environment? = null
    private var jwtRequestExpirationTime: Duration? = null

    fun clientId(clientId: String?) = this.also { this.clientId = clientId }
    fun privateKey(privateKey: PrivateKey?) = this.also { this.privateKey = privateKey }
    fun keyId(keyId: String?) = this.also { this.keyId = keyId }
    fun environment(environment: Environment?) = this.also { this.environment = environment }
    fun jwtRequestExpirationTime(time: Duration?) = this.also { this.jwtRequestExpirationTime = time}

    fun build() = Configuration(
        clientId = clientId ?: throw IllegalArgumentException("clientId is required"),
        privateKey = privateKey ?: throw IllegalArgumentException("privateKey is required"),
        keyId = keyId ?: throw IllegalArgumentException("keyId is required"),
        environment = environment ?: throw IllegalArgumentException("environment is required"),
        jwtRequestExpirationTime = jwtRequestExpirationTime ?: Duration.ofSeconds(60),
    )

}