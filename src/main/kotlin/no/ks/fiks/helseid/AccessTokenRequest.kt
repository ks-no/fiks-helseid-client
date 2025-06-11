package no.ks.fiks.helseid

private val ORGNR_REGEX = Regex("[0-9]{9}")

enum class TenancyType { SINGLE, MULTI }
enum class TokenType { BEARER, DPOP }

sealed class AccessTokenRequest(
    val tokenType: TokenType,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AccessTokenRequest

        return tokenType == other.tokenType
    }

    override fun hashCode(): Int {
        return tokenType.hashCode()
    }
}

class StandardAccessTokenRequest(
    tokenType: TokenType = TokenType.BEARER,
) : AccessTokenRequest(tokenType)

sealed class OrganizationNumberAccessTokenRequest(
    tokenType: TokenType = TokenType.BEARER,
) : AccessTokenRequest(tokenType)

class SingleTenantOrganizationNumberAccessTokenRequest(
    val childOrganizationNumber: String,
    tokenType: TokenType = TokenType.BEARER,
) : OrganizationNumberAccessTokenRequest(tokenType) {

    init {
        if (!childOrganizationNumber.matches(ORGNR_REGEX)) throw IllegalArgumentException("Child organization number has invalid format: $childOrganizationNumber")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as SingleTenantOrganizationNumberAccessTokenRequest

        return childOrganizationNumber == other.childOrganizationNumber
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + childOrganizationNumber.hashCode()
        return result
    }

}

class MultiTenantOrganizationNumberAccessTokenRequest(
    val parentOrganizationNumber: String,
    val childOrganizationNumber: String? = null,
    tokenType: TokenType = TokenType.BEARER,
) : OrganizationNumberAccessTokenRequest(tokenType) {
    init {
        if (!parentOrganizationNumber.matches(ORGNR_REGEX)) throw IllegalArgumentException("Parent organization number has invalid format: $parentOrganizationNumber")
        if (childOrganizationNumber != null && !childOrganizationNumber.matches(ORGNR_REGEX)) throw IllegalArgumentException("Child organization number has invalid format: $childOrganizationNumber")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as MultiTenantOrganizationNumberAccessTokenRequest

        if (parentOrganizationNumber != other.parentOrganizationNumber) return false
        if (childOrganizationNumber != other.childOrganizationNumber) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + parentOrganizationNumber.hashCode()
        result = 31 * result + (childOrganizationNumber?.hashCode() ?: 0)
        return result
    }


}

class AccessTokenRequestBuilder {

    private var parentOrganizationNumber: String? = null
    private var childOrganizationNumber: String? = null
    private var tenancyType: TenancyType? = null
    private var tokenType: TokenType? = null

    fun parentOrganizationNumber(parentOrganizationNumber: String?) = this.also { this.parentOrganizationNumber = parentOrganizationNumber }
    fun childOrganizationNumber(childOrganizationNumber: String) = this.also { this.childOrganizationNumber = childOrganizationNumber }
    fun tenancyType(tenancyType: TenancyType) = this.also { this.tenancyType = tenancyType }
    fun tokenType(tokenType: TokenType) = this.also { this.tokenType = tokenType }

    fun build(): AccessTokenRequest {
        if (parentOrganizationNumber != null || childOrganizationNumber != null) {
            return buildOrganizationNumberAccessTokenRequest()
        }

        return when (tokenType) {
            TokenType.BEARER, null -> StandardAccessTokenRequest(TokenType.BEARER)
            TokenType.DPOP -> StandardAccessTokenRequest(TokenType.DPOP)
        }
    }

    private fun buildOrganizationNumberAccessTokenRequest() = when (tenancyType) {
        TenancyType.SINGLE, null -> {
            SingleTenantOrganizationNumberAccessTokenRequest(
                childOrganizationNumber = childOrganizationNumber ?: throw IllegalArgumentException("Child organization is required for single tenant clients"),
                tokenType = tokenType ?: TokenType.BEARER,
            )
        }
        TenancyType.MULTI -> {
            MultiTenantOrganizationNumberAccessTokenRequest(
                parentOrganizationNumber = parentOrganizationNumber ?: throw IllegalArgumentException("Parent organization is required for multi tenant clients"),
                childOrganizationNumber = childOrganizationNumber,
                tokenType = tokenType ?: TokenType.BEARER,
            )
        }
    }

}
