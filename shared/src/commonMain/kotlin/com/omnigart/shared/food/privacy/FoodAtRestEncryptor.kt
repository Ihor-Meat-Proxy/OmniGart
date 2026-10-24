package com.omnigart.shared.food.privacy

/**
 * At-rest sealing for the food diary JSON blob.
 * Platforms supply AES-GCM (Android) / CryptoKit (iOS). Tests use [NoopFoodAtRestEncryptor].
 * Full SQLCipher for the main OmniGart SQLDelight DB remains Phase 5; the food diary
 * uses this encryptor so erase-all + sealed local storage land with this PR.
 */
interface FoodAtRestEncryptor {
    fun seal(plainUtf8: String): String

    fun open(sealed: String): String
}

/** Identity transform — unit tests and hosts without a keystore yet. */
object NoopFoodAtRestEncryptor : FoodAtRestEncryptor {
    override fun seal(plainUtf8: String): String = plainUtf8

    override fun open(sealed: String): String = sealed
}
