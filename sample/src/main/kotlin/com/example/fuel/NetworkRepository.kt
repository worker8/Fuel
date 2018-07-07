package com.example.fuel

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result

interface NetworkRepository {

    suspend fun httpGetCoroutine(): Result<String, FuelError>
    fun httpResponseObject(): Result<MainActivity.Issue, FuelError>
    fun httpListResponseObject(): Result<List<MainActivity.Issue>, FuelError>
    fun httpGsonResponseObject(): Result<MainActivity.Issue, FuelError>

    fun httpGet(): Result<String, FuelError>
    fun httpPut(): Result<String, FuelError>
    fun httpPost(): Result<String, FuelError>
    fun httpDelete(): Result<String, FuelError>
    httpDownload(progressHandler: (read: Long, total: Long) -> Unit): Result<String, FuelError>
}