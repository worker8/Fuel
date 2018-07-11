package com.example.fuel

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.Reader

data class Issue(
        val id: Int = 0,
        val title: String = "",
        val url: String = ""
) {
    class Deserializer : ResponseDeserializable<Issue> {
        override fun deserialize(reader: Reader) = Gson().fromJson(reader, Issue::class.java)!!
    }

    class ListDeserializer : ResponseDeserializable<List<Issue>> {
        override fun deserialize(reader: Reader): List<Issue> {
            val type = object : TypeToken<List<Issue>>() {}.type
            return Gson().fromJson(reader, type)
        }
    }
}