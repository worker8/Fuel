package com.github.kittinunf.fuel.core.internal

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.*
import com.github.kittinunf.fuel.util.Base64
import com.github.kittinunf.fuel.util.Utils
import com.github.kittinunf.fuel.util.appendln

abstract class BaseRequest : Fuel.RequestConvertible {
    override val request: Request
        get() = this as Request

    var timeoutInMillisecond = 15000
    var timeoutReadInMillisecond = timeoutInMillisecond

    var type: RequestType = RequestType.REQUEST
    lateinit var httpMethod: Method
    lateinit var path: String
    lateinit var url: Url

    //body
    abstract val httpBody: ByteArray

    //headers
    val httpHeaders: MutableMap<String, String> = mutableMapOf<String, String>()

    //params
    var parameters: List<Pair<String, Any?>> = listOf<Pair<String, Any?>>()

    var name: String = ""

    val names: MutableList<String> = mutableListOf<String>()
    val mediaTypes: MutableList<String> = mutableListOf<String>()

    //interceptor
    var requestInterceptor: ((Request) -> Request)? = null
    var responseInterceptor: ((Request, Response) -> Response)? = null

    //interfaces
    fun timeout(timeout: Int): Request {
        timeoutInMillisecond = timeout
        return this as Request
    }

    fun timeoutRead(timeout: Int): Request {
        timeoutReadInMillisecond = timeout
        return this as Request
    }

    fun header(vararg pairs: Pair<String, Any>?): Request {
        pairs.forEach {
            if (it != null)
                httpHeaders += Pair(it.first, it.second.toString())
        }
        return this as Request
    }

    fun header(pairs: Map<String, Any>?): Request = header(pairs, true)

    fun header(pairs: Map<String, Any>?, replace: Boolean): Request {
        pairs?.forEach {
            it.let {
                if (it.key !in httpHeaders || replace) {
                    httpHeaders += Pair(it.key, it.value.toString())
                }
            }
        }
        return this as Request
    }

    fun authenticate(username: String, password: String): Request {
        val auth = "$username:$password"
        val encodedAuth = Base64.encode(Utils.stringToByteArray(auth))
        return header("Authorization" to "Basic " + Utils.byteArrayToString(encodedAuth))
    }

    override fun toString(): String = buildString {
        appendln("\"Body : ${if (httpBody.isNotEmpty()) Utils.byteArrayToString(httpBody) else "(empty)"}\"")
        appendln("\"Headers : (${httpHeaders.size})\"")
        for ((key, value) in httpHeaders) {
            appendln("$key : $value")
        }
    }

    fun cUrlString(): String = buildString {
        append("$ curl -i")

        //method
        if (httpMethod != Method.GET) {
            append("-X $httpMethod")
        }

        //body
        val escapedBody = Utils.byteArrayToString(httpBody).replace("\"", "\\\"")
        if (escapedBody.isNotEmpty()) {
            append("-d \"$escapedBody\"")
        }

        //headers
        for ((key, value) in httpHeaders) {
            append("-H \"$key:$value\"")
        }

        //url
        append("\"$url\"")
    }
}