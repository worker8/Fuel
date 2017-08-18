package com.github.kittinunf.fuel.util

impl object Utils {
    impl fun byteArrayToString(byteArray: ByteArray): String = String(byteArray)
    impl fun stringToByteArray(string: String): ByteArray = string.toByteArray()
}