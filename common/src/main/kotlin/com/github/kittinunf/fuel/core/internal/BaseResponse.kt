package com.github.kittinunf.fuel.core.internal

import com.github.kittinunf.fuel.core.Url
import com.github.kittinunf.fuel.util.Utils
import com.github.kittinunf.fuel.util.appendln

abstract class BaseResponse {
    lateinit var url: Url

    var httpStatusCode = -1
    var httpResponseMessage = ""
    var httpResponseHeaders = emptyMap<String, List<String>>()
    var httpContentLength = 0L

    abstract val data: ByteArray

    override fun toString(): String = buildString {
        appendln("<-- $httpStatusCode ($url)")
        appendln("Response : $httpResponseMessage")
        appendln("Length : $httpContentLength")
        appendln("Body : ${if (data.isNotEmpty()) Utils.byteArrayToString(data) else "(empty)"}")
        appendln("Headers : (${httpResponseHeaders.size})")
        for ((key, value) in httpResponseHeaders) {
            appendln("$key : $value")
        }
    }
}