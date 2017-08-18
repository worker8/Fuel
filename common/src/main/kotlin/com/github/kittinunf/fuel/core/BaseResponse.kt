package com.github.kittinunf.fuel.core

import com.github.kittinunf.fuel.util.Utils

abstract class BaseResponse {
    lateinit var url: Url

    var httpStatusCode = -1
    var httpResponseMessage = ""
    var httpResponseHeaders = emptyMap<String, List<String>>()
    var httpContentLength = 0L

    abstract val data: ByteArray

    override fun toString(): String {
        val elements = mutableListOf("<-- $httpStatusCode ($url)")

        //response message
        elements.add("Response : $httpResponseMessage")

        //content length
        elements.add("Length : $httpContentLength")

        //body
        elements.add("Body : ${if (data.isNotEmpty()) Utils.byteArrayToString(data) else "(empty)"}")

        //headers
        //headers
        elements.add("Headers : (${httpResponseHeaders.size})")
        for ((key, value) in httpResponseHeaders) {
            elements.add("$key : $value")
        }

        return elements.joinToString("\n")
    }

}