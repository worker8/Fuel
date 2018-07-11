package com.example.fuel

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class MainActivity : AppCompatActivity(), MainActivityContract.View {

    lateinit var presenter: MainActivityContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        presenter = MainActivityPresenter(FuelNetworkRepository(filesDir)).also { it.onStart(this) }

        mainGoCoroutineButton.setOnClickListener {
            launch(UI) {
                presenter.httpGetCoroutine()
            }
        }

        mainGoButton.setOnClickListener {
            presenter.executeAll()
        }

        mainClearButton.setOnClickListener {
            presenter.resetText()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onFinish()
    }

    override fun clearText() {
        mainResultText.text = ""
        mainAuxText.text = ""
    }

    override fun <T : Any> update(result: Result<T, FuelError>) {
        result.fold(success = {
            mainResultText.append(it.toString())
        }, failure = {
            mainResultText.append(String(it.errorData))
        })
    }

    override fun appendToMainText(text: String) {
        runOnUiThread {
            mainAuxText.append(text)
        }
    }

}
