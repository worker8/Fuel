package com.github.kittinunf.fuel.core

header class Url(url: String) {
    constructor(context: Url, reference: String)

    val protocol: String
    val port: Int
    val path: String
    val query: String
    val ref: String
    val host: String
}

/*val Url.hostname: String
    get() = host + if (port != -1) ":$port" else ""*/