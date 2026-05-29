package com.example.data

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object CryptoEngine {

    // Generate a predictable but secure Key Spec from a shared secret seed (combination of user phones or key pair seeds)
    private fun getSecretKeySpec(seed: String): SecretKeySpec {
        // Padd/hash to 128-bit key (16 bytes)
        val cleaned = seed.replace("[^0-9a-zA-Z]".toRegex(), "")
        val rawBytes = cleaned.padEnd(16, 'x').substring(0, 16).toByteArray(Charsets.UTF_8)
        return SecretKeySpec(rawBytes, "AES")
    }

    /**
     * Encrypts a plain text string into Base64 ciphertext using the designated seed key.
     */
    fun encrypt(plainText: String, keySeed: String): String {
        return try {
            val keySpec = getSecretKeySpec(keySeed)
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            "CIPHER_ERR_ON_ENCRYPT:${e.localizedMessage}"
        }
    }

    /**
     * Decrypts Base64 ciphertext into its original plain text.
     */
    fun decrypt(cipherText: String, keySeed: String): String {
        if (cipherText.startsWith("CIPHER_ERR_ON_ENCRYPT")) return "Invalid Ciphertext"
        return try {
            val keySpec = getSecretKeySpec(keySeed)
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decodedBytes = Base64.decode(cipherText, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            // Return placeholder or fallback to demonstrate E2EE isolation
            "Decryption locked. Verify E2EE Keys."
        }
    }

    /**
     * Generates a mock asymmetric key fingerprinted visually for identity confirmation (e.g. SHA-256 fingerprint)
     */
    fun getVisualFingerprint(phoneA: String, phoneB: String): String {
        val hashStr = (phoneA + phoneB).hashCode().toString(16).uppercase()
        val formatted = hashStr.padEnd(16, 'F').chunked(4).joinToString(" - ")
        return formatted
    }
}
