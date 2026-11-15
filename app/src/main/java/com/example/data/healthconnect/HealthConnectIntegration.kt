package com.example.data.healthconnect

import android.content.Context
import android.content.Intent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.domain.model.PhysicalActivityKind
import java.time.Duration
import java.time.Instant

enum class HealthConnectSdkAvailability(val label: String, val detail: String) {
    AVAILABLE(
        label = "Connected Ready",
        detail = "Health Connect is installed and ready on this device."
    ),
    PROVIDER_UPDATE_REQUIRED(
        label = "Install / Update Required",
        detail = "Install or update the Health Connect provider app to sync verified workouts."
    ),
    UNAVAILABLE(
        label = "Not Supported",
        detail = "Health Connect is unavailable on this Android environment."
    )
}

data class HealthConnectPermissionState(
    val exerciseReadGranted: Boolean = false,
    val distanceReadGranted: Boolean = false,
    val heartRateReadGranted: Boolean = false,
    val sleepReadGranted: Boolean = false
) {
    val hasCoreExercisePermission: Boolean
        get() = exerciseReadGranted

    val grantedCount: Int
        get() = listOf(exerciseReadGranted, distanceReadGranted, heartRateReadGranted, sleepReadGranted).count { it }

    val totalCount: Int = 4
}

data class ExternalExerciseSample(
    val sourceId: String,
    val originPackage: String,
    val exerciseTypeCode: Int,
    val activityKind: PhysicalActivityKind,
    val title: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val distanceMeters: Double? = null,
    val avgHeartRateBpm: Int? = null,
    val notes: String = ""
)

data class HealthConnectSyncFetchResult(
    val permissionState: HealthConnectPermissionState,
    val exerciseSamples: List<ExternalExerciseSample>,
    val sleepHoursLast24h: Double? = null,
    val restingHeartRateEstimateBpm: Int? = null,
    val errorMessage: String? = null
)

interface HealthConnectDataSource {
    fun getAvailability(): HealthConnectSdkAvailability
    suspend fun getPermissionState(): HealthConnectPermissionState
    suspend fun fetchRecentActivity(sinceEpochMs: Long, untilEpochMs: Long): HealthConnectSyncFetchResult
}

class AndroidHealthConnectDataSource(
    private val context: Context
) : HealthConnectDataSource {

    override fun getAvailability(): HealthConnectSdkAvailability {
        val status = runCatching {
            HealthConnectClient.getSdkStatus(context)
        }.getOrDefault(HealthConnectClient.SDK_UNAVAILABLE)

        return when (status) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectSdkAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectSdkAvailability.PROVIDER_UPDATE_REQUIRED
            else -> HealthConnectSdkAvailability.UNAVAILABLE
        }
    }

    private fun getClientOrNull(): HealthConnectClient? {
        if (getAvailability() != HealthConnectSdkAvailability.AVAILABLE) return null
        return runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
    }

    override suspend fun getPermissionState(): HealthConnectPermissionState {
        val client = getClientOrNull() ?: return HealthConnectPermissionState()
        val granted = runCatching {
            client.permissionController.getGrantedPermissions()
        }.getOrDefault(emptySet())

        return HealthConnectPermissionState(
            exerciseReadGranted = PERMISSION_EXERCISE in granted,
            distanceReadGranted = PERMISSION_DISTANCE in granted,
            heartRateReadGranted = PERMISSION_HEART_RATE in granted,
            sleepReadGranted = PERMISSION_SLEEP in granted
        )
    }

    override suspend fun fetchRecentActivity(
        sinceEpochMs: Long,
        untilEpochMs: Long
    ): HealthConnectSyncFetchResult {
        val client = getClientOrNull()
            ?: return HealthConnectSyncFetchResult(
                permissionState = HealthConnectPermissionState(),
                exerciseSamples = emptyList(),
                errorMessage = "Health Connect provider is not available on this device."
            )

        val permissions = try {
            val granted = client.permissionController.getGrantedPermissions()
            HealthConnectPermissionState(
                exerciseReadGranted = PERMISSION_EXERCISE in granted,
                distanceReadGranted = PERMISSION_DISTANCE in granted,
                heartRateReadGranted = PERMISSION_HEART_RATE in granted,
                sleepReadGranted = PERMISSION_SLEEP in granted
            )
        } catch (e: SecurityException) {
            return HealthConnectSyncFetchResult(
                permissionState = HealthConnectPermissionState(),
                exerciseSamples = emptyList(),
                errorMessage = "Health Connect permissions denied or revoked: ${e.message}"
            )
        } catch (e: Exception) {
            return HealthConnectSyncFetchResult(
                permissionState = HealthConnectPermissionState(),
                exerciseSamples = emptyList(),
                errorMessage = "Unable to query Health Connect permissions: ${e.message}"
            )
        }

        if (!permissions.exerciseReadGranted) {
            return HealthConnectSyncFetchResult(
                permissionState = permissions,
                exerciseSamples = emptyList(),
                errorMessage = "Exercise read permission is required before synchronizing sessions."
            )
        }

        val startInstant = Instant.ofEpochMilli(sinceEpochMs)
        val endInstant = Instant.ofEpochMilli(untilEpochMs)

        val samples = mutableListOf<ExternalExerciseSample>()
        try {
            var pageToken: String? = null
            do {
                val response = client.readRecords(
                    ReadRecordsRequest(
                        recordType = ExerciseSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startInstant, endInstant),
                        pageToken = pageToken
                    )
                )

                for (record in response.records) {
                    val distanceMeters = if (permissions.distanceReadGranted) {
                        runCatching {
                            val agg = client.aggregate(
                                AggregateRequest(
                                    metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                                    timeRangeFilter = TimeRangeFilter.between(record.startTime, record.endTime)
                                )
                            )
                            agg[DistanceRecord.DISTANCE_TOTAL]?.inMeters
                        }.getOrNull()
                    } else null

                    val avgHr = if (permissions.heartRateReadGranted) {
                        runCatching {
                            val agg = client.aggregate(
                                AggregateRequest(
                                    metrics = setOf(HeartRateRecord.BPM_AVG),
                                    timeRangeFilter = TimeRangeFilter.between(record.startTime, record.endTime)
                                )
                            )
                            agg[HeartRateRecord.BPM_AVG]?.toInt()
                        }.getOrNull()
                    } else null

                    val kind = mapExerciseType(record.exerciseType)
                    val title = record.title?.takeIf { it.isNotBlank() } ?: kind.label

                    samples.add(
                        ExternalExerciseSample(
                            sourceId = record.metadata.id,
                            originPackage = record.metadata.dataOrigin.packageName,
                            exerciseTypeCode = record.exerciseType,
                            activityKind = kind,
                            title = title,
                            startEpochMs = record.startTime.toEpochMilli(),
                            endEpochMs = record.endTime.toEpochMilli(),
                            distanceMeters = distanceMeters,
                            avgHeartRateBpm = avgHr,
                            notes = record.notes.orEmpty()
                        )
                    )
                }
                pageToken = response.pageToken
            } while (pageToken != null)
        } catch (e: SecurityException) {
            return HealthConnectSyncFetchResult(
                permissionState = permissions,
                exerciseSamples = emptyList(),
                errorMessage = "Health Connect access revoked during sync."
            )
        } catch (e: Exception) {
            return HealthConnectSyncFetchResult(
                permissionState = permissions,
                exerciseSamples = emptyList(),
                errorMessage = "Health Connect sync error: ${e.message ?: "Unknown failure"}"
            )
        }

        val sleepHours = if (permissions.sleepReadGranted) {
            runCatching {
                val sleepStart = endInstant.minus(Duration.ofHours(30))
                val sleepResponse = client.readRecords(
                    ReadRecordsRequest(
                        recordType = SleepSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(sleepStart, endInstant)
                    )
                )
                if (sleepResponse.records.isEmpty()) {
                    null
                } else {
                    val totalMinutes = sleepResponse.records.sumOf {
                        Duration.between(it.startTime, it.endTime).toMinutes().coerceAtLeast(0L)
                    }
                    if (totalMinutes > 0L) totalMinutes / 60.0 else null
                }
            }.getOrNull()
        } else null

        val minHrEstimate = if (permissions.heartRateReadGranted) {
            runCatching {
                val hrStart = endInstant.minus(Duration.ofHours(24))
                val agg = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(HeartRateRecord.BPM_MIN),
                        timeRangeFilter = TimeRangeFilter.between(hrStart, endInstant)
                    )
                )
                agg[HeartRateRecord.BPM_MIN]?.toInt()
            }.getOrNull()
        } else null

        return HealthConnectSyncFetchResult(
            permissionState = permissions,
            exerciseSamples = samples,
            sleepHoursLast24h = sleepHours,
            restingHeartRateEstimateBpm = minHrEstimate
        )
    }

    companion object {
        val PERMISSION_EXERCISE: String = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
        val PERMISSION_DISTANCE: String = HealthPermission.getReadPermission(DistanceRecord::class)
        val PERMISSION_HEART_RATE: String = HealthPermission.getReadPermission(HeartRateRecord::class)
        val PERMISSION_SLEEP: String = HealthPermission.getReadPermission(SleepSessionRecord::class)

        val ALL_REQUIRED_PERMISSIONS: Set<String> = setOf(
            PERMISSION_EXERCISE,
            PERMISSION_DISTANCE,
            PERMISSION_HEART_RATE,
            PERMISSION_SLEEP
        )

        fun createPermissionRequestContract() =
            PermissionController.createRequestPermissionResultContract()

        fun createHealthConnectSettingsIntent(): Intent =
            Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)

        fun mapExerciseType(exerciseType: Int): PhysicalActivityKind = when (exerciseType) {
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> PhysicalActivityKind.RUNNING
            ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> PhysicalActivityKind.WALKING
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
            ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> PhysicalActivityKind.STRENGTH_TRAINING
            ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS -> PhysicalActivityKind.CALISTHENICS
            ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
            ExerciseSessionRecord.EXERCISE_TYPE_BOOT_CAMP -> PhysicalActivityKind.CONDITIONING
            ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING,
            ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> PhysicalActivityKind.MOBILITY
            else -> PhysicalActivityKind.JOGGING
        }
    }
}
