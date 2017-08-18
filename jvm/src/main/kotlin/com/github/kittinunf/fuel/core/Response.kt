package com.github.kittinunf.fuel.core

import com.github.kittinunf.fuel.core.internal.BaseResponse
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

impl class Response : BaseResponse() {
    var dataStream: InputStream = ByteArrayInputStream(ByteArray(0))
    impl override val data: ByteArray  by lazy {
        try {
            dataStream.use { dataStream.readBytes() }
        } catch (ex: IOException) {  // If dataStream closed by deserializer
            ByteArray(0)
        }
    }
}