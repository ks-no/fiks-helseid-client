package no.ks.fiks.helseid

class HttpException(val status: Int, val body: String) : RuntimeException("HTTP request failed with status $status")