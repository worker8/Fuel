package com.example.fuel

import android.os.Handler
import io.reactivex.disposables.CompositeDisposable

class MainActivityPresenter(private val networkRepository: NetworkRepository) : MainActivityContract.Presenter,
        NetworkRepository by networkRepository {

    private val bag by lazy { CompositeDisposable() }

    var view: MainActivityContract.View? = null

    override fun onStart(view: MainActivityContract.View) {
        this.view = view
    }

    override fun onFinish() {
        bag.clear()
        view = null
    }


    override fun resetText() {
        view?.clearText()
    }

    override fun executeAll() {
        view?.update(httpGet())
        view?.update(httpPut())
        view?.update(httpPost())
        view?.update(httpDelete())
        view?.update(httpDownload{read, total ->
            view?.appendToMainText("$read / $total")
        })
        view?.update(httpUpload{written, total ->
            view?.appendToMainText("Upload: ${written.toFloat() / total.toFloat()}")
        })
        view?.update(httpBasicAuthentication("U$3|2|\\|@me", "P@$\$vv0|2|)"))
        view?.update(httpListResponseObject())
        view?.update(httpResponseObject())
        view?.update(httpGsonResponseObject())
        httpRxSupport()
        rx()
        httpLiveDataSupport().observeForever {
           it?.let { view?.update(it) }
        }
        httpCancel()
    }

    private fun rx(){
        val disposable = httpRxSupport().subscribe { result ->
            view?.update(result)
        }
        bag.add(disposable)

    }

    private fun httpCancel() {
        val request = httpGetRequest()
        request.responseString { _, _, _ ->
        }

        Handler().postDelayed({
            request.cancel()
            view?.appendToMainText("request canceled")
        }, 1000)
    }

}


