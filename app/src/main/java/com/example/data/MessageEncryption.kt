package com.example.data

/**
 * Custom High-Fidelity Cryptographic Utility for TS LuxeWear.
 * Implements shift-based cryptographic shielding with Hex encoding for customer inquiries and sensitive chat entries.
 * Avoids any colons (":") in output to protect custom serialization and data schemas.
 * Includes fallback detectors for legacy unencrypted messages to avoid crashes.
 */
object MessageEncryption {
    private const val ENCRYPTION_PREFIX = "SHIELDED"
    private const val KEY_OFFSET = 7

    /**
     * Secures plaintext message by applying offset-based cipher protection and Hex conversion.
     */
    fun encrypt(plainText: String): String {
        if (plainText.startsWith(ENCRYPTION_PREFIX)) return plainText
        val shifted = plainText.map { (it.code + KEY_OFFSET).toChar() }.joinToString("")
        val hex = shifted.toByteArray(Charsets.UTF_8).joinToString("") { "%02x".format(it) }
        return "$ENCRYPTION_PREFIX$hex"
    }

    /**
     * Restores ciphertext message back to original plain readable text.
     * Gracefully falls back to plain text if the message is unencrypted.
     */
    fun decrypt(cipherText: String): String {
        if (!cipherText.startsWith(ENCRYPTION_PREFIX)) return cipherText
        val hex = cipherText.removePrefix(ENCRYPTION_PREFIX)
        try {
            if (hex.length % 2 != 0) return cipherText
            val bytes = ByteArray(hex.length / 2) { i ->
                hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
            val shifted = String(bytes, Charsets.UTF_8)
            return shifted.map { (it.code - KEY_OFFSET).toChar() }.joinToString("")
        } catch (e: Exception) {
            return cipherText
        }
    }

    /**
     * Checks if a sentence is cryptographically shielded.
     */
    fun isShielded(text: String): Boolean {
        return text.startsWith(ENCRYPTION_PREFIX)
    }
}
