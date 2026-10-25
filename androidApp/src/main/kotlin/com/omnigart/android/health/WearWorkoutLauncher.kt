package com.omnigart.android.health

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.omnigart.shared.health.store.device.PhoneWorkoutSelection
import com.omnigart.shared.health.store.device.PhoneWorkoutSelectionCodec
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Phone → Wear launch via Wearable [MessageClient] on [PhoneWorkoutSelection.LAUNCH_PATH].
 * Callers fall back to phone-only recording when no node is reachable.
 */
class WearWorkoutLauncher(
    context: Context,
    private val messageClient: MessageClient = Wearable.getMessageClient(context.applicationContext),
    private val nodeClient: NodeClient = Wearable.getNodeClient(context.applicationContext)
) {
    enum class Outcome {
        WEAR_STARTED,
        WEAR_UNAVAILABLE,
        SKIPPED_BY_PREFERENCE
    }

    suspend fun startOnWear(context: Context, selection: PhoneWorkoutSelection, respectPreference: Boolean = true): Outcome =
        withContext(Dispatchers.IO) {
            if (respectPreference && !WearLaunchPrefs.isStartOnWatchEnabled(context)) {
                return@withContext Outcome.SKIPPED_BY_PREFERENCE
            }
            runCatching {
                val nodes: List<Node> = Tasks.await(nodeClient.connectedNodes, 3, TimeUnit.SECONDS)
                if (nodes.isEmpty()) return@runCatching Outcome.WEAR_UNAVAILABLE
                val payload = PhoneWorkoutSelectionCodec.encode(selection).toByteArray(StandardCharsets.UTF_8)
                var any = false
                for (node in nodes) {
                    Tasks.await(
                        messageClient.sendMessage(node.id, PhoneWorkoutSelection.LAUNCH_PATH, payload),
                        3,
                        TimeUnit.SECONDS
                    )
                    any = true
                }
                if (any) Outcome.WEAR_STARTED else Outcome.WEAR_UNAVAILABLE
            }.getOrDefault(Outcome.WEAR_UNAVAILABLE)
        }
}

/** SharedPreferences toggle — default on. Mirrors iOS `omnigart.workout.start_on_watch.enabled`. */
object WearLaunchPrefs {
    private const val PREFS = "omnigart_workout_launch_prefs"
    const val KEY_START_ON_WATCH = "omnigart.workout.start_on_watch.enabled"

    fun isStartOnWatchEnabled(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_START_ON_WATCH)) return true
        return prefs.getBoolean(KEY_START_ON_WATCH, true)
    }

    fun setStartOnWatchEnabled(context: Context, enabled: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_START_ON_WATCH, enabled)
            .apply()
    }
}
