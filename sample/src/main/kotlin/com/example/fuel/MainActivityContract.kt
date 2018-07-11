package com.example.fuel

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result

interface MainActivityContract {

    interface View {
       fun <T : Any> Result<T, FuelError>.update()
       fun clearText()
    }

    interface Presenter{
        fun onStart(view:View)
        fun onFinish()
        fun resetText()
        suspend fun httpGetCoroutine()


    }
}