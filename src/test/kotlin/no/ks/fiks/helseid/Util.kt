package no.ks.fiks.helseid

import kotlin.random.Random.Default.nextInt

fun randomOrganizationNumber() = nextInt(100000000, 1000000000).toString()
