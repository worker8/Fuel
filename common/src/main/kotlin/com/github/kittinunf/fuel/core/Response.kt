package com.github.kittinunf.fuel.core

import com.github.kittinunf.fuel.core.internal.BaseResponse

header class Response : BaseResponse {
    override val data: ByteArray
}
