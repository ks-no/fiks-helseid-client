package no.ks.fiks.helseid

import com.github.benmanes.caffeine.cache.Caffeine
import com.nimbusds.oauth2.sdk.id.Issuer
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata
import java.net.URI
import java.time.Duration

interface OpenIdConfiguration {
    fun getTokenEndpoint(): URI
}

class CachedHttpDiscoveryOpenIdConfiguration(
    private val issuer: String,
) : OpenIdConfiguration {

    private val configuration = Caffeine.newBuilder()
        .refreshAfterWrite(Duration.ofHours(6))
        .build<String, OIDCProviderMetadata> { resolveConfiguration() }

    override fun getTokenEndpoint(): URI = configuration.get(issuer).tokenEndpointURI

    private fun resolveConfiguration() = OIDCProviderMetadata.resolve(Issuer(issuer))

}