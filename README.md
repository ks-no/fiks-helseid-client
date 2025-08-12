# HelseID client
![GitHub License](https://img.shields.io/github/license/ks-no/helseid-client)
[![Maven Central](https://img.shields.io/maven-central/v/no.ks.fiks/helseid-client)](https://search.maven.org/artifact/no.ks.fiks/helseid-client)
![GitHub Release Date](https://img.shields.io/github/release-date/ks-no/fiks-helseid-client.svg)
![GitHub Last Commit](https://img.shields.io/github/last-commit/ks-no/fiks-helseid-client.svg)

Used for getting machine-to-machine tokens from the HelseID token endpoint.

## Retrieving access tokens
The `AccessTokenRequestBuilder` is the recommended way to build an `AccessTokenRequest` that can be passed to the client when getting a token.

```kotlin
client.getAccessToken(
    AccessTokenRequestBuilder()
        .tokenType(TokenType.BEARER)
        .build()
)
```

The builder can be used without setting any values explicitly. 
In that case the built request will be a standard Bearer token request.

The client has a built-in cache that caches tokens based on the request parameters. 
Cache times are configurable through the `Configuration` object used when creating the client.

## Documentation
- [HelseID](https://selvbetjening.nhn.no/docs)
- [Sample code](https://github.com/NorskHelsenett/HelseID.Samples)
- [Using client assertions](https://utviklerportal.nhn.no/informasjonstjenester/helseid/bruksmoenstre-og-eksempelkode/bruk-av-helseid/docs/tekniske-mekanismer/bruk_av_client_assertion_enmd)
- [Token endpoint](https://utviklerportal.nhn.no/informasjonstjenester/helseid/bruksmoenstre-og-eksempelkode/bruk-av-helseid/docs/teknisk-referanse/endepunkt/token-endepunktet_enmd)
- [Error messages](https://utviklerportal.nhn.no/informasjonstjenester/helseid/bruksmoenstre-og-eksempelkode/bruk-av-helseid/docs/teknisk-referanse/feilmeldinger_enmd)
- [DPoP](https://utviklerportal.nhn.no/informasjonstjenester/helseid/bruksmoenstre-og-eksempelkode/bruk-av-helseid/docs/dpop/dpop_enmd)

## Environments:
TEST
- issuer = https://helseid-sts.test.nhn.no
- audience = https://helseid-sts.test.nhn.no

PROD
- issuer = https://helseid-sts.nhn.no
- audience = https://helseid-sts.nhn.no

## Notes
- When using a DPoP access token in the `Authorization` header, it must be prefixed with `DPoP` instead of `Bearer`
- The DPoP proof is put in the `DPoP` header without any prefix, and must contain an encoded hash of the used access token
- DPoP proof is valid for 10 seconds, jti can not be reused (which means the entire proof can only be used once)
