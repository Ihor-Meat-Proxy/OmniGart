package com.omnigart.body

import com.omnigart.body.health.BODY_CLIENT_RECORD_VERSION
import com.omnigart.body.health.BodyFatSample
import com.omnigart.body.health.BodyHealthSyncPolicy
import com.omnigart.body.health.BodyHealthWriter
import com.omnigart.body.health.BodyMassSample
import com.omnigart.body.health.BodyReadSourceFilter
import com.omnigart.body.health.BodySyncIdentity
import com.omnigart.body.health.FakeBodyHealthStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class BodyHealthStoreTest {

    @Test
    fun appendBodyMass_isAppendOnly_skipsWhenClientRecordIdPresent() = runTest {
        val store = FakeBodyHealthStore()
        val id = BodySyncIdentity("w1")
        val first = store.appendBodyMass(BodyMassSample(id, 70.0, 10))
        assertTrue(first.ok)
        assertFalse(first.skippedAlreadyPresent)
        assertEquals(BODY_CLIENT_RECORD_VERSION, id.clientRecordVersion)

        val second = store.appendBodyMass(BodyMassSample(id, 71.0, 20))
        assertTrue(second.ok)
        assertTrue(second.skippedAlreadyPresent)
        assertEquals(first.platformRecordId, second.platformRecordId)
        // Original mass retained — no rewrite.
        assertEquals(70.0, store.readLatestBodyMassKg(0, 100))

        store.foreignMassKg = 90.0
        assertEquals(70.0, store.readLatestBodyMassKg(0, 100, BodyReadSourceFilter.ALL_SOURCES))
        assertTrue(BodyHealthSyncPolicy.PHOTOS_LOCAL_ONLY_EN_GB.contains("not", ignoreCase = true))
    }

    @Test
    fun bodyFatAppendCarriesMethodMetadata() = runTest {
        val store = FakeBodyHealthStore()
        val result =
            store.appendBodyFat(
                BodyFatSample(
                    identity = BodySyncIdentity("bf1"),
                    bodyFatPercent = 22.5,
                    epochMs = 1,
                    methodId = "cun_bae",
                ),
            )
        assertTrue(result.ok)
        assertFalse(result.skippedAlreadyPresent)
        assertEquals("cun_bae", store.fat.getValue("bf1").sample.methodId)
        val skip =
            store.appendBodyFat(
                BodyFatSample(
                    identity = BodySyncIdentity("bf1"),
                    bodyFatPercent = 30.0,
                    epochMs = 2,
                    methodId = "us_navy",
                ),
            )
        assertTrue(skip.skippedAlreadyPresent)
        assertEquals("cun_bae", store.fat.getValue("bf1").sample.methodId)
    }

    @Test
    fun writerContract_hasNoUpsertOrDeleteMethods() {
        val methods = BodyHealthWriter::class.java.methods.map { it.name }
        assertFalse(methods.any { it.startsWith("upsert") })
        assertFalse(methods.any { it.contains("delete", ignoreCase = true) })
        assertTrue(methods.contains("appendBodyMass"))
        assertTrue(methods.contains("appendHeight"))
        assertTrue(methods.contains("appendBodyFat"))
        assertTrue(methods.contains("appendWaist"))
    }
}
