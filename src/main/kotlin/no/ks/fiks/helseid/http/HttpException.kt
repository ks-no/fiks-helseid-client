package no.ks.fiks.helseid.http

class HttpException(val status: Int, val body: String) : RuntimeException("HTTP request failed with status $status: ${body.take(400)}")