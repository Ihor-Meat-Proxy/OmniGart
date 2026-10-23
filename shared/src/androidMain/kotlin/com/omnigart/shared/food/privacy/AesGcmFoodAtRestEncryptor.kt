package com.omnigart.shared.food.privacy

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-GCM seal for the food diary blob. Key material must come from Android Keystore
 * (callers wrap [SecretKey]); never log or persist the raw key beside ciphertext.
 */
class AesGcmFoodAtRestEncryptor(private val key: SecretKey) : FoodAtRestEncryptor {
    override fun seal(plainUtf8: String): String {
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        val cipherBytes = cipher.doFinal(plainUtf8.encodeToByteArray())
        val out = ByteArray(iv.size + cipherBytes.size)
        iv.copyInto(out)
        cipherBytes.copyInto(out, destinationOffset = iv.size)
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    override fun open(sealed: String): String {
        val all = Base64.decode(sealed, Base64.NO_WRAP)
        require(all.size > IV_BYTES) { "ciphertext too short" }
        val iv = all.copyOfRange(0, IV_BYTES)
        val cipherBytes = all.copyOfRange(IV_BYTES, all.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(cipherBytes).decodeToString()
    }

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
    }
}
