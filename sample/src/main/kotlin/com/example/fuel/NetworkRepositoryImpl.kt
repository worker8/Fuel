package com.example.fuel

import android.util.Log
import awaitStringResponse
import com.github.kittinunf.fuel.*
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.gson.responseObject
import com.github.kittinunf.fuel.livedata.liveDataObject
import com.github.kittinunf.fuel.rx.rx_object
import com.github.kittinunf.result.Result
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File

class NetworkRepositoryImpl(private val filesDir: File) : NetworkRepository {

    init {
        FuelManager.instance.apply {
            basePath = "http://httpbin.org"
            baseHeaders = mapOf("Device" to "Android")
            baseParams = listOf("key" to "value")
        }
    }

    override suspend fun httpGetCoroutine(): Result<String, FuelError> {
        return Fuel.get("/get", listOf("userId" to "123")).awaitStringResponse().third
    }

    override fun httpResponseObject(): Result<MainActivity.Issue, FuelError> {
        return "https://api.github.com/repos/kittinunf/Fuel/issues/1".httpGet()
                .responseObject(MainActivity.Issue.Deserializer()).third
    }

    override fun httpListResponseObject(): Result<List<MainActivity.Issue>, FuelError> {
        return "https://api.github.com/repos/kittinunf/Fuel/issues".httpGet()
                .responseObject(MainActivity.Issue.ListDeserializer()).third
    }

    override fun httpGsonResponseObject(): Result<MainActivity.Issue, FuelError> {
        return "https://api.github.com/repos/kittinunf/Fuel/issues/1".httpGet()
                .responseObject<MainActivity.Issue>().third
    }

    override fun httpGet(): Result<String, FuelError> {
        return Fuel.get("/get", listOf("foo" to "foo", "bar" to "bar"))
                .responseString().third
    }

    override fun httpPut(): Result<String, FuelError> {
        return "/put".httpPut(listOf("foo" to "foo", "bar" to "bar"))
                .responseString().third
    }

    override fun httpPost(): Result<String, FuelError> {
        return "/post".httpPost(listOf("foo" to "foo", "bar" to "bar"))
                .responseString().third
    }

    override fun httpDelete(): Result<String, FuelError> {
        return "/delete".httpDelete(listOf("foo" to "foo", "bar" to "bar"))
                .responseString().third
    }

    override fun httpDownload(progressHandler: (read: Long, total: Long) -> Unit): Result<String, FuelError> {
       return Fuel.download("/bytes/${1024 * 100}").destination { _, _ ->
            File(filesDir, "test.tmp")
       }.progress (progressHandler).responseString ().third
    }

    private fun httpUpload() {
        Fuel.upload("/post").source { _, _ ->
            //create random file with some non-sense string
            val file = File(filesDir, "out.tmp")
            file.writer().use { writer ->
                repeat(100) {
                    writer.appendln("abcdefghijklmnopqrstuvwxyz")
                }
            }
            file
        }.progress { writtenBytes, totalBytes ->
            Log.v(TAG, "Upload: ${writtenBytes.toFloat() / totalBytes.toFloat()}")
        }.responseString { request, _, result ->
            Log.d(TAG, request.toString())
            update(result)
        }
    }

    private fun httpBasicAuthentication() {
        val username = "U$3|2|\\|@me"
        val password = "P@$\$vv0|2|)"
        Fuel.get("/basic-auth/$username/$password").authenticate(username, password)
                .responseString { request, _, result ->
                    Log.d(TAG, request.toString())
                    update(result)
                }

        "/basic-auth/$username/$password".httpGet().authenticate(username, password)
                .responseString { request, _, result ->
                    Log.d(TAG, request.toString())
                    update(result)
                }
    }

    private fun httpRxSupport() {
        val disposable = "https://api.github.com/repos/kittinunf/Fuel/issues/1".httpGet()
                .rx_object(MainActivity.Issue.Deserializer())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { result ->
                    Log.d(TAG, result.toString())
                }
        bag.add(disposable)
    }

    private fun httpLiveDataSupport() {
        "https://api.github.com/repos/kittinunf/Fuel/issues/1".httpGet()
                .liveDataObject(MainActivity.Issue.Deserializer())
                .observeForever { result ->
                    Log.d(TAG, result.toString())
                }
    }


}