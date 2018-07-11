package com.example.fuel

import android.arch.lifecycle.LiveData
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.result.Result
import io.reactivex.Single

interface NetworkRepository {

    suspend fun httpGetCoroutine(): Result<String, FuelError>
    fun httpResponseObject(): Result<Issue, FuelError>
    fun httpListResponseObject(): Result<List<Issue>, FuelError>
    fun httpGsonResponseObject(): Result<Issue, FuelError>

    fun httpGetRequest(): Request
    fun httpGet(): Result<String, FuelError>
    fun httpPut(): Result<String, FuelError>
    fun httpPost(): Result<String, FuelError>
    fun httpDelete(): Result<String, FuelError>
    fun httpDownload(progressHandler: (read: Long, total: Long) -> Unit): Result<String, FuelError>
    fun httpUpload(progressHandler: (read: Long, total: Long) -> Unit): Result<String, FuelError>
    fun httpBasicAuthentication(username: String, password: String): Result<String, FuelError>
    fun httpRxSupport(): Single<Result<Issue, FuelError>>
    fun httpLiveDataSupport(): LiveData<Result<Issue, FuelError>>
}