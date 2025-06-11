package no.ks.fiks.helseid

import io.kotest.assertions.asClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.util.*

class AssertionDetailsBuilderTest : StringSpec({

    "Test building single tenant claim" {
        val childOrganizationNumber = UUID.randomUUID().toString()
        AssertionDetailsBuilder.buildSingleTenantClaim(childOrganizationNumber).asClue { claim ->
            claim.assertContents("urn:oid:2.16.578.1.12.4.1.4.101", childOrganizationNumber)
        }
    }

    "Test building multi tenant claim with child organization number" {
        val parentOrganizationNumber = UUID.randomUUID().toString()
        val childOrganizationNumber = UUID.randomUUID().toString()
        AssertionDetailsBuilder.buildMultiTenantClaim(parentOrganizationNumber, childOrganizationNumber).asClue { claim ->
            claim.assertContents("urn:oid:1.0.6523", "NO:ORGNR:$parentOrganizationNumber:$childOrganizationNumber")
        }
    }

    "Test building multi tenant claim without child organization number" {
        val parentOrganizationNumber = UUID.randomUUID().toString()
        AssertionDetailsBuilder.buildMultiTenantClaim(parentOrganizationNumber, null).asClue { claim ->
            claim.assertContents("urn:oid:1.0.6523", "NO:ORGNR:$parentOrganizationNumber")
        }
    }

})

private fun Map<String, Any>.assertContents(expectedSystem: String, expectedValue: String) {
    this shouldHaveSize 2

    this shouldContainKey "type"
    this["type"] shouldBe "helseid_authorization"

    this shouldContainKey "practitioner_role"
    this["practitioner_role"]
        .let { it as Map<*, *> }
        .asClue { practitionerRole ->
            practitionerRole shouldHaveSize 1

            practitionerRole["organization"]
                .let { it as Map<*, *> }
                .asClue { organization ->
                    organization shouldHaveSize 1

                    organization["identifier"]
                        .let { it as Map<*, *> }
                        .asClue { identifier ->
                            identifier shouldHaveSize 3

                            identifier["system"] shouldBe expectedSystem
                            identifier["type"] shouldBe "ENH"
                            identifier["value"] shouldBe expectedValue
                        }
                }
        }
}
