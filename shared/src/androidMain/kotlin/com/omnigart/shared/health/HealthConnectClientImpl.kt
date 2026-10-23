package com.omnigart.shared.health

/**
 * Health Connect adapter implementing [HealthConnectClient].
 *
 * Production apps inject [SdkHealthConnectGateway]; tests inject a gateway backed by [FakeHealthClient]
 * or `androidx.health.connect:connect-testing` (alpha — see OPEN_QUESTIONS).
 */
class HealthConnectClientImpl(private val gateway: HealthConnectGateway) : HealthConnectClient {
    /** Permission strings for `PermissionController.createRequestPermissionResultContract()`. */
    fun hcPermissionStrings(permissions: Set<HealthPermission>): Set<String> = gateway.permissionStrings(permissions)

    override suspend fun authorizationStatus(permissions: Set<HealthPermission>): AuthStatus {
        val needed = gateway.permissionStrings(permissions)
        if (needed.isEmpty()) return AuthStatus.AUTHORIZED
        val granted = gateway.getGrantedPermissions()
        return when {
            granted.containsAll(needed) -> AuthStatus.AUTHORIZED
            granted.any { it in needed } -> AuthStatus.PARTIAL
            else -> AuthStatus.NOT_DETERMINED
        }
    }

    override suspend fun requestAuthorization(permissions: Set<HealthPermission>): AuthStatus {
        // Host Activity must launch the HC permission contract; we re-read status afterwards.
        return authorizationStatus(permissions)
    }

    override suspend fun readHeartRate(range: TimeRange) = gateway.readHeartRate(range)

    override suspend fun writeHeartRate(samples: List<HeartRateSample>) = gateway.writeHeartRate(samples)

    override suspend fun readRestingHeartRate(range: TimeRange) = gateway.readRestingHeartRate(range)

    override suspend fun writeRestingHeartRate(samples: List<HeartRateSample>) = gateway.writeRestingHeartRate(samples)

    override suspend fun readHrv(range: TimeRange) = gateway.readHrv(range)

    override suspend fun writeHrv(samples: List<HrvSample>) = gateway.writeHrv(samples)

    override suspend fun readSleep(range: TimeRange) = gateway.readSleep(range)

    override suspend fun writeSleep(sessions: List<SleepSession>) = gateway.writeSleep(sessions)

    override suspend fun readBodyMeasurements(types: Set<HealthDataType>, range: TimeRange) = gateway.readBody(types, range)

    override suspend fun writeBodyMeasurements(measurements: List<BodyMeasurement>) = gateway.writeBody(measurements)

    override suspend fun readNutrition(range: TimeRange) = gateway.readNutrition(range)

    override suspend fun writeNutrition(entries: List<NutritionEntry>) = gateway.writeNutrition(entries)

    override suspend fun readWater(range: TimeRange) = gateway.readWater(range)

    override suspend fun writeWater(entries: List<WaterIntake>) = gateway.writeWater(entries)

    override suspend fun readWorkouts(range: TimeRange) = gateway.readWorkouts(range)

    override suspend fun writeWorkouts(workouts: List<HealthWorkout>) = gateway.writeWorkouts(workouts)

    override suspend fun changes(types: Set<HealthDataType>, previous: HealthChangeToken?): HealthChanges {
        val token = previous?.value ?: getOrCreateChangesToken(types).value
        return gateway.getChanges(token)
    }

    override suspend fun getOrCreateChangesToken(types: Set<HealthDataType>): HealthChangeToken = HealthChangeToken(
        platform = "health_connect",
        value = gateway.getChangesToken(types)
    )

    override suspend fun revokeAllPermissions() {
        // User must revoke in system Health Connect settings on current Android versions.
    }
}

/** Adapts [FakeHealthClient] as a [HealthConnectGateway] for JVM/androidHostTest. */
class FakeHealthConnectGateway(private val fake: FakeHealthClient = FakeHealthClient()) : HealthConnectGateway {
    val underlying: FakeHealthClient get() = fake

    override suspend fun getGrantedPermissions(): Set<String> {
        // Mirror whatever the fake has authorised by probing each permission.
        return HealthPermission.entries
            .filter { perm ->
                val status = fake.authorizationStatus(setOf(perm))
                status == AuthStatus.AUTHORIZED || status == AuthStatus.PARTIAL
            }.map { "fake.${it.name}" }
            .toSet()
    }

    override fun permissionStrings(permissions: Set<HealthPermission>): Set<String> = permissions.map { "fake.${it.name}" }.toSet()

    override suspend fun readHeartRate(range: TimeRange) = fake.readHeartRate(range)

    override suspend fun writeHeartRate(samples: List<HeartRateSample>) = fake.writeHeartRate(samples)

    override suspend fun readRestingHeartRate(range: TimeRange) = fake.readRestingHeartRate(range)

    override suspend fun writeRestingHeartRate(samples: List<HeartRateSample>) = fake.writeRestingHeartRate(samples)

    override suspend fun readHrv(range: TimeRange) = fake.readHrv(range)

    override suspend fun writeHrv(samples: List<HrvSample>) = fake.writeHrv(samples)

    override suspend fun readSleep(range: TimeRange) = fake.readSleep(range)

    override suspend fun writeSleep(sessions: List<SleepSession>) = fake.writeSleep(sessions)

    override suspend fun readBody(types: Set<HealthDataType>, range: TimeRange) = fake.readBodyMeasurements(types, range)

    override suspend fun writeBody(measurements: List<BodyMeasurement>) = fake.writeBodyMeasurements(measurements)

    override suspend fun readNutrition(range: TimeRange) = fake.readNutrition(range)

    override suspend fun writeNutrition(entries: List<NutritionEntry>) = fake.writeNutrition(entries)

    override suspend fun readWater(range: TimeRange) = fake.readWater(range)

    override suspend fun writeWater(entries: List<WaterIntake>) = fake.writeWater(entries)

    override suspend fun readWorkouts(range: TimeRange) = fake.readWorkouts(range)

    override suspend fun writeWorkouts(workouts: List<HealthWorkout>) = fake.writeWorkouts(workouts)

    override suspend fun getChangesToken(types: Set<HealthDataType>): String = fake.getOrCreateChangesToken(types).value

    override suspend fun getChanges(token: String): HealthChanges {
        val raw =
            fake.changes(
                setOf(HealthDataType.HEART_RATE),
                HealthChangeToken("health_connect", token)
            )
        return raw.copy(
            nextToken =
            (raw.nextToken ?: HealthChangeToken("health_connect", token)).copy(
                platform = "health_connect"
            )
        )
    }
}
