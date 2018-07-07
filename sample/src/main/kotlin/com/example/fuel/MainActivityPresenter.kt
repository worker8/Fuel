package com.example.fuel

import awaitStringResponse
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result

class MainActivityPresenter(private val networkRepository: NetworkRepository) : MainActivityContract.Presenter {

    var view: MainActivityContract.View? = null

    override fun onStart(view: MainActivityContract.View) {
        this.view = view
    }

    override fun onFinish() {
        view = null
    }

    override suspend fun httpGetCoroutine() {
        networkRepository.httpGetCoroutine().updateView()
    }

    override fun resetText() {
        view?.clearText()
    }

    private fun Result<String, FuelError>.updateView() {
        view?.update(this)
    }
}


