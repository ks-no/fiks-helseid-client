package no.ks.fiks.helseid.http

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.util.*

class DpopHeaderHelperTest : StringSpec({

    "Should pass expected names and values" {
        val accessToken = UUID.randomUUID().toString()
        val dpopProof = UUID.randomUUID().toString()

        val parameters = mutableListOf<Pair<String, String>>()
        DpopHeaderHelper.setHeaders(
            accessToken = accessToken,
            dpopProof = dpopProof,
            setHeaderFunction = { headerName, headerValue ->
                parameters.add(headerName to headerValue)
            }
        )

        parameters shouldContainExactly listOf(
            parameters[0] shouldBe Pair("Authorization", "DPoP $accessToken"),
            parameters[1] shouldBe Pair("DPoP", dpopProof),
        )
    }

})