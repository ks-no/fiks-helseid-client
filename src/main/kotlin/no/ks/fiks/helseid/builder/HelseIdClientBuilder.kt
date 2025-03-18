package no.ks.fiks.helseid.builder

import no.ks.fiks.helseid.HelseIdClient
import no.ks.fiks.helseid.Configuration
import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients

class HelseIdClientBuilder {

    private var httpClient: HttpClient? = null
    private var configuration: Configuration? = null

    fun httpClient(httpClient: HttpClient?) = this.also { this.httpClient = httpClient }
    fun configuration(konfigurasjon: Configuration) = this.also { this.configuration = konfigurasjon }

    fun build() = HelseIdClient(
        httpClient = httpClient ?: HttpClients.createMinimal(),
        configuration = configuration ?: throw IllegalArgumentException("configuration is required")
    )

}