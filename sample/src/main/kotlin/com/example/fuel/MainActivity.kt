package com.example.fuel

import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.livedata.liveDataObject
import com.github.kittinunf.result.Result
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class MainActivity : AppCompatActivity(), MainActivityContract.View {

    private val TAG = MainActivity::class.java.simpleName

    private val bag by lazy { CompositeDisposable() }

    private lateinit var networkRepository: NetworkRepository

    lateinit var presenter: MainActivityContract.Presenter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        networkRepository = FuelNetworkRepository(filesDir)

        presenter = MainActivityPresenter(networkRepository).also { it.onStart(this) }

        mainGoCoroutineButton.setOnClickListener {
            launch(UI) {
                presenter.httpGetCoroutine()
            }
        }

        mainGoButton.setOnClickListener {
            execute()
        }

        mainClearButton.setOnClickListener {
            presenter.resetText()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bag.clear()
        presenter.onFinish()
    }

    override fun clearText() {
        mainResultText.text = ""
        mainAuxText.text = ""
    }

    private fun execute() {
        httpGet()
        httpPut()
        httpPost()
        httpDelete()
        httpDownload()
        httpUpload()
        httpBasicAuthentication()
        httpListResponseObject()
        httpResponseObject()
        httpGsonResponseObject()
        httpCancel()
        httpRxSupport()
        httpLiveDataSupport()
    }

    private fun httpCancel() {
        val request = networkRepository.httpGetRequest()
        request.responseString { _, _, _ ->
        }

        Handler().postDelayed({
            request.cancel()
        }, 1000)
    }

    private fun httpResponseObject() {
        networkRepository.httpResponseObject().update()
    }


    private fun httpListResponseObject() {
        networkRepository.httpListResponseObject().update()
    }

    private fun httpGsonResponseObject() {
        networkRepository.httpGsonResponseObject().update()
    }

    private fun httpGet() {
        networkRepository.httpGet().update()
    }

    private fun httpPut() {
        networkRepository.httpPut().update()
    }

    private fun httpPost() {
        networkRepository.httpPost().update()
    }

    private fun httpDelete() {
        networkRepository.httpDelete().update()
    }

    private fun httpDownload() {
        networkRepository.httpDownload { readBytes, totalBytes ->
            val progress = "$readBytes / $totalBytes"
            runOnUiThread {
                mainAuxText.text = progress
            }
            Log.v(TAG, progress)
        }.update()
    }

    private fun httpUpload() {
        networkRepository.httpUpload { writtenBytes, totalBytes ->
            Log.v(TAG, "Upload: ${writtenBytes.toFloat() / totalBytes.toFloat()}")
        }.update()
    }

    private fun httpBasicAuthentication() {
        networkRepository.httpBasicAuthentication("U$3|2|\\|@me", "P@$\$vv0|2|)").update()
    }

    private fun httpRxSupport() {
        val disposable = networkRepository.httpRxSupport().subscribe { result ->
            result.update()
        }
        bag.add(disposable)
    }

    private fun httpLiveDataSupport() {
        "https://api.github.com/repos/kittinunf/Fuel/issues/1".httpGet()
                .liveDataObject(Issue.Deserializer())
                .observeForever { result ->
                    Log.d(TAG, result.toString())
                }
    }

    override fun <T : Any> Result<T, FuelError>.update() {
        this.fold(success = {
            mainResultText.append(it.toString())
        }, failure = {
            mainResultText.append(String(it.errorData))
        })
    }

}
