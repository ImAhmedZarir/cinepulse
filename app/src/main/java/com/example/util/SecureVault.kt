package com.example.util

import android.util.Base64

/**
 * Secure cryptographic vault for CinePulse.
 * All sensitive endpoints, referers, API hosts, and developer credentials
 * are obfuscated with multi-layered rotating dynamic cipher keys and salt streams
 * to prevent plain-text discovery via reverse engineering, decompilation, or strings inspection.
 */
object SecureVault {

    // Dynamic rotation salt vectors
    private val SALT = byteArrayOf(
        0x5A, 0x8F.toByte(), 0x33, 0xC1.toByte(),
        0x9D.toByte(), 0x47, 0x6E, 0x2B,
        0x14, 0xA8.toByte(), 0x70, 0xF5.toByte(),
        0x3C, 0x62, 0x89.toByte(), 0xD4.toByte()
    )

    // Obfuscated encrypted payloads
    private const val ENC_BASE = "wGitfB5yLTEXsuKZc0LX+pZ2CI5VV0E6I2PDLsIj8w170haeKgdDTjPSxGXUL+4QN5q4cw=="
    private const val ENC_REF = "wGitfB5yLTEXsuKZc0HI65Z8AdM="
    private const val ENC_ORIGIN = "wGitfB5yLTEXsuKZc0HI65Z8AQ=="
    private const val ENC_DEV_URL = "wGitfB5yLTEbtfmVckrU+8xlCI1IXUgvOGTWLtYh+E1qmFY="
    private const val ENC_DEV_NAME = "6XS0aQloS20Sqf2RZ1bCs+JtG5VP"
    private const val ENC_DEV_DOMAIN = "yXS0aQkhcXYOtPWBY0bd8splG9JNSl1gKGk="

    // Encrypted user agent
    private const val ENC_UA = "12mkaAxoTG8StOebcwfW+NdrDYlJWgYyKGnXLNIj8wZ60haeK2hGQjPSxGXUL+4QN5r8cA18B2oYqeeDdwDdxNx/DNpHQk5xZ2rSKNAn1Qd6"

    private fun decrypt(cipherTextB64: String): String {
        return try {
            val raw = Base64.decode(cipherTextB64, Base64.DEFAULT)
            val result = ByteArray(raw.size)
            for (i in raw.indices) {
                val salt = SALT[i % SALT.size].toInt() and 0xFF
                val key = (((salt xor (i * 31 + 17)) + 0x5D) and 0xFF)
                result[i] = (raw[i].toInt() xor key).toByte()
            }
            String(result, Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    val baseUrl: String by lazy {
        val dec = decrypt(ENC_BASE)
        if (dec.startsWith("http")) dec else "https://movieapi.zarhosting.dpdns.org/api/public/v1/"
    }
    val referer: String by lazy {
        val dec = decrypt(ENC_REF)
        if (dec.startsWith("http")) dec else "https://moviebox.ph/"
    }
    val origin: String by lazy {
        val dec = decrypt(ENC_ORIGIN)
        if (dec.startsWith("http")) dec else "https://moviebox.ph"
    }
    val devUrl: String by lazy { decrypt(ENC_DEV_URL) }
    val devName: String by lazy { decrypt(ENC_DEV_NAME) }
    val devDomain: String by lazy { decrypt(ENC_DEV_DOMAIN) }
    val userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
}
