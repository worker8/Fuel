package com.github.kittinunf.fuel

import com.github.kittinunf.fuel.core.Request

header class Fuel {
    header interface PathStringConvertible {
        val path: String
    }

    header interface RequestConvertible {
        val request: Request
    }
}