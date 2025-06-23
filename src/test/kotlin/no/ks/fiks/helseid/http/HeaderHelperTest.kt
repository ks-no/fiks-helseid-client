package no.ks.fiks.helseid.http

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.util.*

class HeaderHelperTest : StringSpec({

    "Should pass expected name and value" {
        val accessToken = UUID.randomUUID().toString()

        val parameters = mutableListOf<Pair<String, String>>()
        HeaderHelper.setHeaders(
            accessToken = accessToken,
            setHeaderFunction = { headerName, headerValue ->
                parameters.add(headerName to headerValue)
            }
        )

        parameters shouldContainExactly listOf(
            parameters[0] shouldBe Pair("Authorization", "Bearer $accessToken"),
        )
    }

})