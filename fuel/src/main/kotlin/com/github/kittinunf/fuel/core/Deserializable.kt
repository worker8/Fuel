package com.github.kittinunf.fuel.core

import com.github.kittinunf.fuel.core.requests.AsyncTaskRequest
import com.github.kittinunf.fuel.core.requests.CancellableRequest
import com.github.kittinunf.fuel.core.requests.SuspendingRequest
import com.github.kittinunf.fuel.core.requests.toTask
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import com.github.kittinunf.result.mapError
import java.io.InputStream
import java.io.Reader

typealias HandlerWithResult<T> = (Request, Response, Result<T, FuelError>) -> Unit

interface Deserializable<out T : Any> {
    fun deserialize(response: Response): T
}

interface ResponseDeserializable<out T : Any> : Deserializable<T> {
    override fun deserialize(response: Response): T {
        response.body.toStream().use { stream ->
            return deserialize(stream)
                ?: deserialize(stream.reader())
                ?: response.let {
                    // Reassign the body here so it can be read once more.
                    val length = it.body.length
                    it.body = DefaultBody.from({ stream }, length?.let { l -> { l } })

                    deserialize(response.data)
                        ?: deserialize(String(response.data))
                        ?: throw FuelError(IllegalStateException(
                            "One of deserialize(ByteArray) or deserialize(InputStream) or deserialize(Reader) or " +
                                "deserialize(String) must be implemented"
                        ))
                }
        }
    }

    // One of these methods must be implemented
    fun deserialize(bytes: ByteArray): T? = null

    fun deserialize(inputStream: InputStream): T? = null

    fun deserialize(reader: Reader): T? = null

    fun deserialize(content: String): T? = null
}

fun <T : Any, U : Deserializable<T>> Request.response(deserializable: U, handler: HandlerWithResult<T>): CancellableRequest =
    response(deserializable, { _, response, value ->
        handler(this@response, response, Result.Success(value))
    }, { _, response, error ->
        handler(this@response, response, Result.Failure(error))
    })

fun <T : Any, U : Deserializable<T>> Request.response(deserializable: U, handler: Handler<T>): CancellableRequest =
    response(deserializable, { request, response, value ->
        handler.success(request, response, value)
    }, { request, response, error ->
        handler.failure(request, response, error)
    })

private fun <T : Any, U : Deserializable<T>> Request.response(
    deserializable: U,
    success: (Request, Response, T) -> Unit,
    failure: (Request, Response, FuelError) -> Unit
): CancellableRequest {
    val asyncRequest = AsyncTaskRequest(this)

    asyncRequest.successCallback = { response ->
        val deliverable = Result.of<T, Exception> { deserializable.deserialize(response) }
        executionOptions.callback {
            deliverable.fold({
                success(this, response, it)
            }, {
                failure(this, response, FuelError(it))
            })
        }
    }

    asyncRequest.failureCallback = { error, response ->
        executionOptions.callback {
            failure(this, response, error)
        }
    }

    return CancellableRequest(this, future = executionOptions.submit(asyncRequest))
}

fun <T : Any, U : Deserializable<T>> Request.response(deserializable: U): Triple<Request, Response, Result<T, FuelError>> {
    var response: Response? = null
    val result = Result.of<Response, Exception> { toTask().call() }
        .map {
            response = it
            deserializable.deserialize(it)
        }
        .mapError {
            if (it is FuelError) {
                response = it.response
                it
            } else {
                FuelError(it)
            }
        }

    return Triple(this, response ?: Response.error(), result)
}

suspend fun <T : Any, U : Deserializable<T>> Request.awaitResponse(deserializable: U): Triple<Request, Response, Result<T, FuelError>> {
    val r = SuspendingRequest(this).awaitResult()
    val res =
        r.map {
            deserializable.deserialize(it)
        }.mapError <T, Exception, FuelError> {
            it as? FuelError ?: FuelError(it)
        }

    return Triple(this, r.component1() ?: Response.error(), res)
}