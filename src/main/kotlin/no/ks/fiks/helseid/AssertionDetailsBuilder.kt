package no.ks.fiks.helseid

private const val CLAIM_TYPE = "helseid_authorization"
private const val IDENTIFIER_TYPE = "ENH"

private const val SINGLE_TENANT_SYSTEM = "urn:oid:2.16.578.1.12.4.1.4.101"
private const val MULTI_TENANT_SYSTEM = "urn:oid:1.0.6523"

/**
 * @see <a href="https://utviklerportal.nhn.no/informasjonstjenester/helseid/bruksmoenstre-og-eksempelkode/bruk-av-helseid/docs/tekniske-mekanismer/organisasjonsnumre_enmd">HelseID documentation</a>
 */
object AssertionDetailsBuilder {

    fun buildSingleTenantClaim(childOrganizationNumber: String) = buildClaim(
        system = SINGLE_TENANT_SYSTEM,
        value = childOrganizationNumber,
    )

    fun buildMultiTenantClaim(parentOrganizationNumber: String, childOrganizationNumber: String?) = buildClaim(
        system = MULTI_TENANT_SYSTEM,
        value = "NO:ORGNR:$parentOrganizationNumber${childOrganizationNumber?.prependColon() ?: ""}",
    )

    private fun String.prependColon() = let { ":$it" }

    private fun buildClaim(system: String, value: String) = mapOf(
        "type" to CLAIM_TYPE,
        "practitioner_role" to mapOf(
            "organization" to mapOf(
                "identifier" to mapOf(
                    "system" to system,
                    "type" to IDENTIFIER_TYPE,
                    "value" to value
                )
            )
        )
    )

}