package com.omnigart.shared.health

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class HealthConnectClientImplTest {
    @Test
    fun gatewayBackedClientRoundTrips() = runTest {
        val gateway = FakeHealthConnectGateway()
        gateway.underlying.grant(
            setOf(HealthPermission.READ_HEART_RATE, HealthPermission.WRITE_HEART_RATE)
        )
        val client = HealthConnectClientImpl(gateway)
        assertEquals(
            AuthStatus.AUTHORIZED,
            client.authorizationStatus(
                setOf(HealthPermission.READ_HEART_RATE, HealthPermission.WRITE_HEART_RATE)
            )
        )
        val at = Instant.parse("2026-10-01T12:00:00Z")
        client.writeHeartRate(listOf(HeartRateSample(90.0, at)))
        val range =
            TimeRange(
                Instant.parse("2026-10-01T00:00:00Z"),
                Instant.parse("2026-10-02T00:00:00Z")
            )
        assertEquals(90.0, client.readHeartRate(range).single().beatsPerMinute)
        val token = client.getOrCreateChangesToken(setOf(HealthDataType.HEART_RATE))
        assertEquals("health_connect", token.platform)
        val changes = client.changes(setOf(HealthDataType.HEART_RATE), token)
        assertEquals("health_connect", changes.nextToken?.platform)
    }
}
