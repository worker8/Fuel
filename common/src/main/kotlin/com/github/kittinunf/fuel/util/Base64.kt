package com.github.kittinunf.fuel.util

/**
 * Utilities for encoding the Base64 representation of
 * binary data.  See RFCs <a
 * href="http://www.ietf.org/rfc/rfc2045.txt">2045</a> and <a
 * href="http://www.ietf.org/rfc/rfc3548.txt">3548</a>.
 */
header object Base64 {
    /**
     * Base64-encode the given data and return a newly allocated
     * byte[] with the result.
     */
    fun encode(data: ByteArray): ByteArray
}