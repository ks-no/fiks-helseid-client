# HelseID client
Used for getting machine-to-machine tokens from the HelseID token endpoint.

Documentation:
- [HelseID](https://selvbetjening.nhn.no/docs)
- [Sample code](https://github.com/NorskHelsenett/HelseID.Samples)
- [Using client assertions](https://utviklerportal.nhn.no/informasjonstjenester/helseid/bruksmoenstre-og-eksempelkode/bruk-av-helseid/docs/tekniske-mekanismer/bruk_av_client_assertion_enmd)
- [Token endpoint](https://utviklerportal.nhn.no/informasjonstjenester/helseid/bruksmoenstre-og-eksempelkode/bruk-av-helseid/docs/teknisk-referanse/endepunkt/token-endepunktet_enmd)
- [Error messages](https://utviklerportal.nhn.no/informasjonstjenester/helseid/bruksmoenstre-og-eksempelkode/bruk-av-helseid/docs/teknisk-referanse/feilmeldinger_enmd)
- [DPoP](https://utviklerportal.nhn.no/informasjonstjenester/helseid/bruksmoenstre-og-eksempelkode/bruk-av-helseid/docs/dpop/dpop_enmd)

# Notes
- When using a DPoP access token in the `Authorization` header, it must be prefixed with `DPoP` instead of `Bearer`
- The DPoP proof is put in the `DPoP` header without any prefix, and must contain an encoded hash of the used access token
