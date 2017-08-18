package com.github.kittinunf.fuel.core

import com.github.kittinunf.fuel.core.deserializers.ByteArrayDeserializer
import com.github.kittinunf.fuel.core.deserializers.StringDeserializer
import com.github.kittinunf.fuel.core.internal.BaseRequest
import com.github.kittinunf.fuel.core.requests.DownloadTaskRequest
import com.github.kittinunf.fuel.core.requests.TaskRequest
import com.github.kittinunf.fuel.core.requests.UploadTaskRequest
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSocketFactory

impl class Request : BaseRequest() {
    //body
    var bodyCallback: ((Request, OutputStream?, Long) -> Long)? = null
    impl override val httpBody: ByteArray
        get() {
            return ByteArrayOutputStream().apply {
                bodyCallback?.invoke(request, this, 0)
            }.toByteArray()
        }

    lateinit var client: Client

    //underlying task request
    val taskRequest: TaskRequest by lazy {
        when (type) {
            RequestType.DOWNLOAD -> DownloadTaskRequest(this)
            RequestType.UPLOAD -> UploadTaskRequest(this)
            else -> TaskRequest(this)
        }
    }

    var taskFuture: Future<*>? = null

    //configuration
    var socketFactory: SSLSocketFactory? = null
    var hostnameVerifier: HostnameVerifier? = null

    //callers
    lateinit var executor: ExecutorService
    lateinit var callbackExecutor: Executor

    fun body(body: ByteArray): Request {
        bodyCallback = { request, outputStream, totalLength ->
            outputStream?.write(body)
            body.size.toLong()
        }
        return this
    }

    fun body(body: String, charset: Charset = Charsets.UTF_8): Request = body(body.toByteArray(charset))


    fun progress(handler: (readBytes: Long, totalBytes: Long) -> Unit): Request {
        if (taskRequest as? DownloadTaskRequest != null) {
            val download = taskRequest as DownloadTaskRequest
            download.apply {
                progressCallback = handler
            }
        } else if (taskRequest as? UploadTaskRequest != null) {
            val upload = taskRequest as UploadTaskRequest
            upload.apply {
                progressCallback = handler
            }
        } else {
            throw IllegalStateException("progress is only used with RequestType.DOWNLOAD or RequestType.UPLOAD")
        }

        return this
    }

    fun dataParts(dataParts: (Request, Url) -> Iterable<DataPart>): Request {
        val uploadTaskRequest = taskRequest as? UploadTaskRequest ?: throw IllegalStateException("source is only used with RequestType.UPLOAD")
        val parts = dataParts.invoke(request, request.url)

        mediaTypes.apply {
            clear()
            addAll(parts.map { it.type })
        }

        names.apply {
            clear()
            addAll(parts.map { it.name })
        }

        uploadTaskRequest.apply {
            sourceCallback = { _, _ -> parts.map { it.file } }
        }

        return this
    }

    fun sources(sources: (Request, Url) -> Iterable<File>): Request {
        mediaTypes.clear()
        names.clear()

        val uploadTaskRequest = taskRequest as? UploadTaskRequest ?: throw IllegalStateException("source is only used with RequestType.UPLOAD")

        uploadTaskRequest.apply {
            sourceCallback = sources
        }
        return this
    }

    fun source(source: (Request, Url) -> File): Request {
        sources { request, _ ->
            listOf(source.invoke(request, request.url))
        }

        return this
    }

    fun name(name: () -> String): Request {
        this.name = name()
        return this
    }

    fun destination(destination: (Response, Url) -> File): Request {
        val downloadTaskRequest = taskRequest as? DownloadTaskRequest ?: throw IllegalStateException("destination is only used with RequestType.DOWNLOAD")

        downloadTaskRequest.apply {
            destinationCallback = destination
        }
        return this
    }

    fun interrupt(interrupt: (Request) -> Unit): Request {
        taskRequest.apply {
            interruptCallback = interrupt
        }
        return this
    }

    fun submit(callable: Callable<*>) {
        taskFuture = executor.submit(callable)
    }

    fun callback(f: () -> Unit) {
        callbackExecutor.execute {
            f.invoke()
        }
    }

    fun cancel() {
        taskFuture?.cancel(true)
    }

    override val request: Request
        get() = this


    //byte array
    fun response(handler: (Request, Response, Result<ByteArray, FuelError>) -> Unit) =
            response(byteArrayDeserializer(), handler)

    fun response(handler: Handler<ByteArray>) = response(byteArrayDeserializer(), handler)

    fun response() = response(byteArrayDeserializer())

    //string
    fun responseString(charset: Charset = Charsets.UTF_8, handler: (Request, Response, Result<String, FuelError>) -> Unit) =
            response(stringDeserializer(charset), handler)

    fun responseString(charset: Charset, handler: Handler<String>) = response(stringDeserializer(charset), handler)

    fun responseString(handler: Handler<String>) = response(stringDeserializer(), handler)

    @JvmOverloads
    fun responseString(charset: Charset = Charsets.UTF_8) = response(stringDeserializer(charset))

    //object
    fun <T : Any> responseObject(deserializer: ResponseDeserializable<T>, handler: (Request, Response, Result<T, FuelError>) -> Unit) = response(deserializer, handler)

    fun <T : Any> responseObject(deserializer: ResponseDeserializable<T>, handler: Handler<T>) = response(deserializer, handler)

    fun <T : Any> responseObject(deserializer: ResponseDeserializable<T>) = response(deserializer)

    companion object {
        fun byteArrayDeserializer() = ByteArrayDeserializer()

        fun stringDeserializer(charset: Charset = Charsets.UTF_8) = StringDeserializer(charset)
    }

}
