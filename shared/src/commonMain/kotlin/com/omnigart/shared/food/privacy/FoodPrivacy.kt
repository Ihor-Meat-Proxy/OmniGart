package com.omnigart.shared.food.privacy

import com.omnigart.shared.food.diary.FoodDiaryStore

/**
 * Erase-all for the food diary (local only). Does not touch HealthKit / Health Connect —
 * platform dietary samples remain (append-only policy forbids platform deletes).
 */
object FoodPrivacy {
    fun eraseAllFoodData(store: FoodDiaryStore, profileId: String) {
        store.eraseAll(profileId)
    }

    const val ENCRYPTION_NOTE_EN_GB: String =
        "Food diary rows live in OmniGart’s local database. Full SQLCipher encryption " +
            "for the on-device store is planned with encrypted backup; until then, protect " +
            "the device with the system lock screen."
}
