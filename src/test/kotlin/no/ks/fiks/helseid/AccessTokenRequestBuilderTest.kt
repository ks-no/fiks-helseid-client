package no.ks.fiks.helseid

import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.*

class AccessTokenRequestBuilderTest : FreeSpec({

    "Standard request" - {
        "Default should be standard bearer" {
            AccessTokenRequestBuilder()
                .build()
                .asClue {
                    it.shouldBeInstanceOf<StandardAccessTokenRequest>()
                    it.tokenType shouldBe TokenType.BEARER
                }
        }

        "Build standard bearer request" {
            AccessTokenRequestBuilder()
                .tokenType(TokenType.BEARER)
                .build()
                .asClue {
                    it.shouldBeInstanceOf<StandardAccessTokenRequest>()
                    it.tokenType shouldBe TokenType.BEARER
                }
        }

        "Build standard DPoP request" {
            AccessTokenRequestBuilder()
                .tokenType(TokenType.DPOP)
                .build()
                .asClue {
                    it.shouldBeInstanceOf<StandardAccessTokenRequest>()
                    it.tokenType shouldBe TokenType.DPOP
                }
        }
    }

    "Single tenant request" - {
        "Default tenancy type should be single" {
            val childOrganizationNumber = randomOrganizationNumber()
            AccessTokenRequestBuilder()
                .childOrganizationNumber(childOrganizationNumber)
                .build()
                .asClue {
                    it.shouldBeInstanceOf<SingleTenantAccessTokenRequest>()
                    it.tokenType shouldBe TokenType.BEARER
                    it.childOrganizationNumber shouldBe childOrganizationNumber
                }
        }

        "Should require child organization number" {
            shouldThrow<IllegalArgumentException> {
                AccessTokenRequestBuilder()
                    .parentOrganizationNumber(randomOrganizationNumber())
                    .tenancyType(TenancyType.SINGLE)
                    .build()
            }.asClue { it.message shouldBe "Child organization is required for single tenant clients" }
        }

        "Child organization number should be validated" {
            val childOrganizationNumber = UUID.randomUUID().toString()
            shouldThrow<IllegalArgumentException> {
                AccessTokenRequestBuilder()
                    .childOrganizationNumber(childOrganizationNumber)
                    .tenancyType(TenancyType.SINGLE)
                    .build()
            }.asClue { it.message shouldBe "Child organization number has invalid format: $childOrganizationNumber" }
        }
    }

    "Multi tenant request" - {
        "Should require parent organization number" {
            shouldThrow<IllegalArgumentException> {
                AccessTokenRequestBuilder()
                    .childOrganizationNumber(randomOrganizationNumber())
                    .tenancyType(TenancyType.MULTI)
                    .build()
            }.asClue { it.message shouldBe "Parent organization is required for multi tenant clients" }
        }

        "Should be able to use with only parent organization" {
            val parentOrganizationNumber = randomOrganizationNumber()
            AccessTokenRequestBuilder()
                .parentOrganizationNumber(parentOrganizationNumber)
                .tenancyType(TenancyType.MULTI)
                .build()
                .asClue {
                    it.shouldBeInstanceOf<MultiTenantAccessTokenRequest>()
                    it.tokenType shouldBe TokenType.BEARER
                    it.parentOrganizationNumber shouldBe parentOrganizationNumber
                    it.childOrganizationNumber should beNull()
                }
        }

        "Should be able to use with both parent and child organization" {
            val parentOrganizationNumber = randomOrganizationNumber()
            val childOrganizationNumber = randomOrganizationNumber()
            AccessTokenRequestBuilder()
                .parentOrganizationNumber(parentOrganizationNumber)
                .childOrganizationNumber(childOrganizationNumber)
                .tenancyType(TenancyType.MULTI)
                .build()
                .asClue {
                    it.shouldBeInstanceOf<MultiTenantAccessTokenRequest>()
                    it.tokenType shouldBe TokenType.BEARER
                    it.parentOrganizationNumber shouldBe parentOrganizationNumber
                    it.childOrganizationNumber shouldBe childOrganizationNumber
                }
        }

        "Parent organization number should be validated" {
            val parentOrganizationNumber = UUID.randomUUID().toString()
            shouldThrow<IllegalArgumentException> {
                AccessTokenRequestBuilder()
                    .parentOrganizationNumber(parentOrganizationNumber)
                    .tenancyType(TenancyType.MULTI)
                    .build()
            }.asClue { it.message shouldBe "Parent organization number has invalid format: $parentOrganizationNumber" }
        }

        "Child organization number should be validated" {
            val childOrganizationNumber = UUID.randomUUID().toString()
            shouldThrow<IllegalArgumentException> {
                AccessTokenRequestBuilder()
                    .parentOrganizationNumber(randomOrganizationNumber())
                    .childOrganizationNumber(childOrganizationNumber)
                    .tenancyType(TenancyType.MULTI)
                    .build()
            }.asClue { it.message shouldBe "Child organization number has invalid format: $childOrganizationNumber" }
        }
    }

})
