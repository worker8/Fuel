package com.github.kittinunf.fuel.core

import java.net.URL

//FIXME use native URL
//impl typealias Url = URL

impl class Url(private val url: URL) {
    impl constructor(url: String): this(URL(url))
    impl constructor(context: Url, reference: String): this(URL(context.toURL(), reference))
    impl val protocol: String get() = url.protocol
    impl val port: Int get() = url.port
    impl val path: String get() = url.path
    impl val query: String get() = url.query
    impl val ref: String get() = url.ref
    impl val host: String get() = url.host

    fun toURL() = url

    override fun hashCode(): Int = url.hashCode()
    override fun equals(other: Any?): Boolean = other is Url && url == other.url
    override fun toString(): String = url.toString()
}

