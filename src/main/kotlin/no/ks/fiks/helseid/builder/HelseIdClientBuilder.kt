package no.ks.fiks.helseid.builder

import no.ks.fiks.helseid.CachedHttpDiscoveryOpenIdConfiguration
import no.ks.fiks.helseid.Configuration
import no.ks.fiks.helseid.HelseIdClient
import no.ks.fiks.helseid.OpenIdConfiguration
import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients

class HelseIdClientBuilder {

    private var configuration: Configuration? = null
    private var httpClient: HttpClient? = null
    private var openIdConfiguration: OpenIdConfiguration? = null

    fun httpClient(httpClient: HttpClient?) = this.also { this.httpClient = httpClient }
    fun configuration(konfigurasjon: Configuration) = this.also { this.configuration = konfigurasjon }
    fun openIdConfiguration(openIdConfiguration: OpenIdConfiguration) = this.also { this.openIdConfiguration = openIdConfiguration }

    fun build(): HelseIdClient {
        val configuration = configuration ?: throw IllegalArgumentException("configuration is required")
        return HelseIdClient(
            configuration = configuration,
            openIdConfiguration = openIdConfiguration ?: CachedHttpDiscoveryOpenIdConfiguration(configuration.environment.issuer),
            httpClient = httpClient ?: HttpClients.createMinimal()
        )
    }

}