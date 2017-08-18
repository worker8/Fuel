package com.github.kittinunf.fuel.core

import com.github.kittinunf.fuel.core.internal.BaseRequest

header class Request : BaseRequest() {
    override val httpBody: ByteArray
}