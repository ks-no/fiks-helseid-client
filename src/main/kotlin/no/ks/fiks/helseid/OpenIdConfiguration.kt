package no.ks.fiks.helseid

import com.google.common.base.Suppliers
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

    private val configuration = Suppliers.memoizeWithExpiration(
        { resolveConfiguration() },
        Duration.ofHours(6),
    )

    override fun getTokenEndpoint(): URI = configuration.get().tokenEndpointURI

    private fun resolveConfiguration() = OIDCProviderMetadata.resolve(Issuer(issuer))

}