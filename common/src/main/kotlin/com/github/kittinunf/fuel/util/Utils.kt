package com.github.kittinunf.fuel.util

header object Utils {
    fun byteArrayToString(byteArray: ByteArray): String
    fun stringToByteArray(string: String): ByteArray
}

fun StringBuilder.appendln(string: String) = append(string).append('\n')