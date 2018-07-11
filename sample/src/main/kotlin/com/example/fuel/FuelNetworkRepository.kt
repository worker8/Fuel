package com.example.fuel

import awaitStringResponse
import com.github.kittinunf.fuel.*
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.gson.responseObject
import com.github.kittinunf.fuel.livedata.liveDataObject
import com.github.kittinunf.fuel.rx.rx_object
import com.github.kittinunf.result.Result
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File

class FuelNetworkRepository(private val filesDir: File) : NetworkRepository {

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

    override fun httpResponseObject(): Result<Issue, FuelError> {
        return "https://api.github.com/repos/kittinunf/Fuel/issues/1".httpGet()
                .responseObject(Issue.Deserializer()).third
    }

    override fun httpListResponseObject(): Result<List<Issue>, FuelError> {
        return "https://api.github.com/repos/kittinunf/Fuel/issues".httpGet()
                .responseObject(Issue.ListDeserializer()).third
    }

    override fun httpGsonResponseObject(): Result<Issue, FuelError> {
        return "https://api.github.com/repos/kittinunf/Fuel/issues/1".httpGet()
                .responseObject<Issue>().third
    }

    override fun httpGet(): Result<String, FuelError> {
        return Fuel.get("/get", listOf("foo" to "foo", "bar" to "bar"))
                .responseString().third
    }

    override fun httpGetRequest(): Request {
        return Fuel.get("/delay/10").interrupt {

        }
    }

    override fun httpPut(): Result<String, FuelError> {
        return "/put".httpPut(listOf("foo" to "foo", "bar" to "bar"))
                .responseString().third
    }

    override fun httpPost(): Result<String, FuelError> {
        return "/post".httpPost(listOf("foo" to "foo", "bar" to "bar"))
                .responseString().third
    }

    override fun httpDelete(): Result<String, FuelError> = "/delete".httpDelete(listOf("foo" to "foo", "bar" to "bar"))
            .responseString().third


    override fun httpDownload(progressHandler: (read: Long, total: Long) -> Unit): Result<String, FuelError> =
            Fuel.download("/bytes/${1024 * 100}").destination { _, _ ->
                File(filesDir, "test.tmp")
            }.progress(progressHandler).responseString().third


    override fun httpUpload(progressHandler: (read: Long, total: Long) -> Unit) =
            Fuel.upload("/post").source { _, _ ->
                //create random file with some non-sense string
                val file = File(filesDir, "out.tmp")
                file.writer().use { writer ->
                    repeat(100) {
                        writer.appendln("abcdefghijklmnopqrstuvwxyz")
                    }
                }
                file
            }.progress(progressHandler)
                    .responseString().third

    override fun httpBasicAuthentication(username: String, password: String): Result<String, FuelError> =
            Fuel.get("/basic-auth/$username/$password").authenticate(username, password)
                    .responseString().third

    override fun httpRxSupport() = "https://api.github.com/repos/kittinunf/Fuel/issues/1".httpGet()
            .rx_object(Issue.Deserializer())
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())!!

    override fun httpLiveDataSupport() = "https://api.github.com/repos/kittinunf/Fuel/issues/1".httpGet()
            .liveDataObject(Issue.Deserializer())

}