package com.github.kittinunf.fuel.toolbox

import com.github.kittinunf.fuel.core.Client
import com.github.kittinunf.fuel.core.requests.DefaultBody
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.HeaderName
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.requests.isCancelled
import com.github.kittinunf.fuel.util.ProgressInputStream
import com.github.kittinunf.fuel.util.ProgressOutputStream
import com.github.kittinunf.fuel.util.decode
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URLConnection
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max

class HttpClient(
    private val proxy: Proxy? = null,
    private var stethoHook: StethoHook? = null,
    var useHttpCache: Boolean = true,
    var decodeContent: Boolean = true
) : Client {
    override fun executeRequest(request: Request): Response {
        return try {
            doRequest(request)
        } catch (exception: Exception) {
            throw FuelError.wrap(exception, Response(request.url))
        } finally {
            // As per Android documentation, a connection that is not explicitly disconnected
            // will be pooled and reused!  So, don't close it as we need inputStream later!
            // connection.disconnect()
        }
    }

    @Throws(InterruptedException::class)
    private fun ensureRequestActive(request: Request, connection: HttpURLConnection? = null) {
        val cancelled = request.isCancelled
        if (!cancelled && !Thread.currentThread().isInterrupted) {
            return
        }

        // Flush all the pipes. This is necessary because we don't want the other end to wait for a timeout or hang.
        // This interrupts the connection correctly and makes the connection available later. This does break any
        // keep-alive on this particular connection
        connection?.disconnect()

        throw InterruptedException("[HttpClient] could not ensure Request was active: cancelled=$cancelled")
    }

    override suspend fun awaitRequest(request: Request): Response = suspendCoroutine { continuation ->
        try {
            continuation.resume(doRequest(request))
        } catch (exception: Exception) {
            continuation.resumeWithException(FuelError.wrap(exception, Response(request.url)))
        }
    }

    @Throws
    private fun doRequest(request: Request): Response {
        val connection = establishConnection(request) as HttpURLConnection
        sendRequest(request, connection)
        return retrieveResponse(request, connection)
    }

    @Throws(InterruptedException::class)
    private fun sendRequest(request: Request, connection: HttpURLConnection) {
        ensureRequestActive(request, connection)
        connection.apply {
            connectTimeout = max(request.executionOptions.timeoutInMillisecond, 0)
            readTimeout = max(request.executionOptions.timeoutReadInMillisecond, 0)
            requestMethod = HttpClient.coerceMethod(request.method).value
            doInput = true
            useCaches = request.executionOptions.useHttpCache ?: useHttpCache
            instanceFollowRedirects = false

            request.headers.transformIterate(
                { key, values -> setRequestProperty(key, values) },
                { key, value -> addRequestProperty(key, value) }
            )

            // By default, the Android implementation of HttpURLConnection requests that servers use gzip compression
            //   and it automatically decompresses the data for callers of URLConnection.getInputStream().
            //   The Content-Encoding and Content-Length response headers are cleared in this case. Gzip compression can
            //   be disabled by setting the acceptable encodings in the request header:
            //
            //      .header(Headers.ACCEPT_ENCODING, "identity")
            //
            // However, on the JVM, this behaviour might be different. Content-Encoding SHOULD NOT be used, in HTTP/1x
            //  to act as Transfer Encoding. In HTTP/2, Transfer-Encoding is not part of the Connection field and should
            //  not be injected here. HttpURLConnection is only HTTP/1x, whereas Java 9 introduces a new HttpClient for
            //  HTTP/2.
            //
            // This adds the TE header for HTTP/1 connections, and automatically decodes it using decodeTransfer.

            // The TE (Accept Transfer Encoding) can only be one of these, should match decodeTransfer.
            setRequestProperty(
                Headers.ACCEPT_TRANSFER_ENCODING,
                Headers.collapse(HeaderName(Headers.ACCEPT_TRANSFER_ENCODING), SUPPORTED_DECODING)
            )

            // The underlying HttpURLConnection does not support PATCH.
            if (request.method == Method.PATCH) {
                setRequestProperty("X-HTTP-Method-Override", Method.PATCH.value)
            }

            setDoOutput(connection, request.method)
            setBodyIfDoOutput(connection, request)
            stethoHook?.preConnect(connection, request)
        }
    }

    @Throws
    private fun retrieveResponse(request: Request, connection: HttpURLConnection): Response {
        ensureRequestActive(request, connection)

        val headers = Headers.from(connection.headerFields)
        val transferEncoding = headers[Headers.TRANSFER_ENCODING].flatMap { it.split(',') }.map { it.trim() }
        val contentEncoding = headers[Headers.CONTENT_ENCODING].lastOrNull()
        var contentLength = headers[Headers.CONTENT_LENGTH].lastOrNull()?.toLong()
        val shouldDecode = (request.executionOptions.decodeContent ?: decodeContent) && contentEncoding != null && contentEncoding != "identity"

        if (shouldDecode) {
            // `decodeContent` decodes the response, so the final response has no more `Content-Encoding`
            headers.remove(Headers.CONTENT_ENCODING)

            // URLConnection.getContentLength() returns the number of bytes transmitted and cannot be used to predict
            // how many bytes can be read from URLConnection.getInputStream() for compressed streams. Therefore if the
            // stream will be decoded, the length becomes unknown
            //
            headers.remove(Headers.CONTENT_LENGTH)

            contentLength = null
        }

        // `decodeTransfer` decodes the response, so the final response has no more Transfer-Encoding
        headers.remove(Headers.TRANSFER_ENCODING)

        // [RFC 7230, 3.3.2](https://tools.ietf.org/html/rfc7230#section-3.3.2)
        //
        // When a message does not have a Transfer-Encoding header field, a
        //   Content-Length header field can provide the anticipated size, as a
        //   decimal number of octets, for a potential payload body.
        //
        //   A sender MUST NOT send a Content-Length header field in any message
        //   that contains a Transfer-Encoding header field.
        //
        // [RFC 7230, 3.3.3](https://tools.ietf.org/html/rfc7230#section-3.3.3)
        //
        // Any 2xx (Successful) response to a CONNECT request implies that
        //   the connection will become a tunnel immediately after the empty
        //   line that concludes the header fields.  A client MUST ignore any
        //   Content-Length or Transfer-Encoding header fields received in
        //   such a message.
        //
        if (transferEncoding.any { encoding -> encoding.isNotBlank() && encoding != "identity" }) {
            headers.remove(Headers.CONTENT_LENGTH)
            contentLength = -1
        }

        val contentStream = dataStream(connection)?.decode(transferEncoding) ?: ByteArrayInputStream(ByteArray(0))
        val inputStream = if (shouldDecode && contentEncoding != null) {contentStream.decode(contentEncoding)} else {contentStream}

        val cancellationConnection = WeakReference<HttpURLConnection>(connection)
        val progressStream = ProgressInputStream(
            inputStream, onProgress = { readBytes ->
                request.executionOptions.responseProgress(readBytes, contentLength ?: readBytes)
                ensureRequestActive(request, cancellationConnection.get())
            }
        )

        // The input and output streams returned by connection are not buffered. In order to give consistent progress
        // reporting, by means of flushing, the input stream here is buffered.
        return Response(
            url = request.url,
            headers = headers,
            contentLength = contentLength ?: -1,
            statusCode = connection.responseCode,
            responseMessage = connection.responseMessage.orEmpty(),
            body = DefaultBody.from(
                { progressStream.buffered(FuelManager.progressBufferSize) },
                { contentLength ?: -1 }
            )
        )
    }

    private fun dataStream(connection: HttpURLConnection): InputStream? {
        return try {
            try {
                val inputStream = stethoHook?.interpretResponseStream(connection.inputStream) ?: connection.inputStream
                stethoHook?.postConnect()
                BufferedInputStream(inputStream)
            } catch (_: IOException) {
                // The InputStream SHOULD be closed, but just in case the backing implementation is faulty, this ensures
                // the InputStream ís actually always closed.
                try { connection.inputStream?.close() } catch (_: IOException) {}

                connection.errorStream?.let { BufferedInputStream(it) }
            } finally {
                // We want the stream to live. Closing the stream is handled by Deserialize
            }
        } catch (exception: IOException) {

            // The ErrorStream SHOULD be closed, but just in case the backing implementation is faulty, this ensures the
            // ErrorStream ís actually always closed.
            try { connection.errorStream?.close() } catch (_: IOException) {}

            stethoHook?.httpExchangeFailed(exception)

            ByteArrayInputStream(exception.message?.toByteArray() ?: ByteArray(0))
        } finally {
            // We want the stream to live. Closing the stream is handled by Deserialize
        }
    }

    private fun establishConnection(request: Request): URLConnection {
        val urlConnection = if (proxy != null) request.url.openConnection(proxy) else request.url.openConnection()
        return if (request.url.protocol == "https") {
            (urlConnection as HttpsURLConnection).apply {
                sslSocketFactory = request.executionOptions.socketFactory
                hostnameVerifier = request.executionOptions.hostnameVerifier
            }
        } else {
            urlConnection as HttpURLConnection
        }
    }

    private fun setBodyIfDoOutput(connection: HttpURLConnection, request: Request) {
        val body = request.body
        if (!connection.doOutput || body.isEmpty()) {
            return
        }

        val contentLength = body.length
        if (contentLength != null && contentLength != -1L) {
            // The content has a known length, so no need to chunk
            connection.setFixedLengthStreamingMode(contentLength.toLong())
        } else {
            // The content doesn't have a known length, so turn it into chunked
            connection.setChunkedStreamingMode(4096)
        }

        val totalBytes = if ((contentLength ?: -1L).toLong() > 0) { contentLength!!.toLong() } else { null }

        // The input and output streams returned by connection are not buffered. In order to give consistent progress
        // reporting, by means of flushing, the output stream here is buffered.
        body.writeTo(
            ProgressOutputStream(
                connection.outputStream,
                onProgress = { writtenBytes ->
                    request.executionOptions.requestProgress(writtenBytes, totalBytes ?: writtenBytes)
                    ensureRequestActive(request, connection)
                }
            ).buffered(FuelManager.progressBufferSize)
        )

        connection.outputStream.flush()
    }

    private fun setDoOutput(connection: HttpURLConnection, method: Method) = when (method) {
        Method.GET, Method.HEAD, Method.OPTIONS, Method.TRACE -> connection.doOutput = false
        Method.DELETE, Method.POST, Method.PUT, Method.PATCH -> connection.doOutput = true
    }

    companion object {
        private val SUPPORTED_DECODING = listOf("gzip", "deflate; q=0.5")
        private fun coerceMethod(method: Method) = if (method == Method.PATCH) Method.POST else method
    }

    interface StethoHook {
        fun preConnect(connection: HttpURLConnection, request: Request)
        fun interpretResponseStream(inputStream: InputStream): InputStream
        fun postConnect()
        fun httpExchangeFailed(exception: IOException)
    }
}