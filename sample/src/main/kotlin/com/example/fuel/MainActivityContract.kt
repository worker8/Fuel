package com.example.fuel

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result

interface MainActivityContract {

    interface View {
        fun <T : Any> update(result: Result<T, FuelError>)
        fun clearText()
        fun appendToMainText(text: String)
    }

    interface Presenter : NetworkRepository {
        fun onStart(view: View)
        fun onFinish()
        fun resetText()
        fun executeAll()
    }
}