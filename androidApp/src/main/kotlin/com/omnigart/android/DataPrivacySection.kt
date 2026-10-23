package com.omnigart.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.health.connect.client.PermissionController
import com.omnigart.design.theme.OmniGartColors
import com.omnigart.design.theme.OmniGartSpacing
import com.omnigart.shared.OmniGartCore
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Profile → Data and privacy: export/import, sync status, conflicts, restore. */
@Composable
fun DataPrivacySection(core: OmniGartCore) {
    val context = LocalContext.current
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var selectedDetailId by remember { mutableStateOf<String?>(null) }
    var removedIds by remember { mutableStateOf(core.localHealth.removedFromPlatformIds()) }
    var conflictLines by remember {
        mutableStateOf(core.localHealth.conflictSummaries().map(::formatConflict))
    }
    val importLauncher =
        rememberImportLauncher(core) { message ->
            importMessage = message
            conflictLines = core.localHealth.conflictSummaries().map(::formatConflict)
        }
    val createDocLauncher = rememberJsonExportLauncher(core) { exportMessage = it }

    Column(verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space2)) {
        DataPrivacyHeader(core)
        RemovedWorkoutsRestoreBlock(
            core = core,
            removedIds = removedIds,
            onSelect = { selectedDetailId = it },
            onRemovedChanged = { removedIds = it }
        )
        selectedDetailId?.let { id ->
            LocalHealthWorkoutDetailSection(core = core, workoutId = id) {
                removedIds = core.localHealth.removedFromPlatformIds()
                if (!core.localHealth.isRemovedFromPlatform(id)) selectedDetailId = null
            }
        }
        ExportImportButtons(
            core = core,
            context = context,
            onExportMessage = { exportMessage = it },
            onCreateDoc = { createDocLauncher.launch("omnigart-health.json") },
            onImport = { importLauncher.launch(arrayOf("application/json", "text/*")) }
        )
        ConflictReview(core = core, conflictLines = conflictLines) {
            conflictLines = emptyList()
        }
        exportMessage?.let {
            Text(text = it, modifier = Modifier.testTag("data_privacy_export_message"))
        }
        importMessage?.let {
            Text(text = it, modifier = Modifier.testTag("data_privacy_import_message"))
        }
        Text(
            text = stringResource(R.string.data_privacy_permission_note),
            style = MaterialTheme.typography.labelSmall,
            color = OmniGartColors.colorTextLightSecondary
        )
    }
}

@Composable
private fun RemovedWorkoutsRestoreBlock(
    core: OmniGartCore,
    removedIds: List<String>,
    onSelect: (String) -> Unit,
    onRemovedChanged: (List<String>) -> Unit
) {
    if (removedIds.isEmpty()) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var restoreMessage by remember { mutableStateOf<String?>(null) }
    var restoringAll by remember { mutableStateOf(false) }
    val permissionLauncher =
        rememberLauncherForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) {
            scope.launch {
                restoringAll = true
                val (ok, failed) = HealthConnectWorkoutRestorer.restoreAll(context, core)
                restoreMessage = context.getString(R.string.health_restore_all_result, ok, failed)
                onRemovedChanged(core.localHealth.removedFromPlatformIds())
                restoringAll = false
            }
        }
    Text(
        text = stringResource(R.string.health_restore_removed_section),
        style = MaterialTheme.typography.titleSmall
    )
    removedIds.forEach { id ->
        Text(
            text = core.localHealth.workoutDetailTitle(id),
            modifier = Modifier.testTag("health_restore_row_$id").clickable { onSelect(id) }
        )
        Text(
            text = core.localHealth.workoutDetailSubtitle(id),
            style = MaterialTheme.typography.labelSmall,
            color = OmniGartColors.colorTextLightSecondary
        )
    }
    Button(
        onClick = {
            scope.launch {
                restoringAll = true
                if (!HealthConnectWorkoutRestorer.hasWriteAccess(context)) {
                    restoringAll = false
                    runCatching {
                        permissionLauncher.launch(HealthConnectWorkoutRestorer.writePermissions())
                    }.onFailure {
                        restoreMessage =
                            context.getString(R.string.health_restore_permission_needed)
                    }
                    return@launch
                }
                val (ok, failed) = HealthConnectWorkoutRestorer.restoreAll(context, core)
                restoreMessage = context.getString(R.string.health_restore_all_result, ok, failed)
                onRemovedChanged(core.localHealth.removedFromPlatformIds())
                restoringAll = false
            }
        },
        enabled = !restoringAll,
        modifier = Modifier.testTag("health_restore_all_health_connect")
    ) {
        if (restoringAll) {
            CircularProgressIndicator()
        } else {
            Text(text = stringResource(R.string.health_restore_all_health_connect))
        }
    }
    restoreMessage?.let {
        Text(text = it, modifier = Modifier.testTag("health_restore_bulk_message"))
    }
}

@Composable
private fun ExportImportButtons(
    core: OmniGartCore,
    context: Context,
    onExportMessage: (String) -> Unit,
    onCreateDoc: () -> Unit,
    onImport: () -> Unit
) {
    Button(onClick = onCreateDoc, modifier = Modifier.testTag("data_privacy_export")) {
        Text(text = stringResource(R.string.data_privacy_export))
    }
    Button(
        onClick = {
            shareHealthExport(context, core)
            onExportMessage(context.getString(R.string.data_privacy_export_ready))
        },
        modifier = Modifier.testTag("data_privacy_export_share")
    ) {
        Text(text = stringResource(R.string.data_privacy_export_share))
    }
    Button(
        onClick = {
            OmnigartPdfShare.shareLatest(context, core)
            onExportMessage(context.getString(R.string.pdf_export_ready))
        },
        modifier = Modifier.testTag("pdf_export_button")
    ) {
        Text(text = stringResource(R.string.pdf_export_button))
    }
    Button(
        onClick = {
            OmnigartPdfShare.shareLast30Days(context, core)
            onExportMessage(context.getString(R.string.pdf_export_ready))
        },
        modifier = Modifier.testTag("pdf_export_range_button")
    ) {
        Text(text = stringResource(R.string.pdf_export_range_button))
    }
    Button(onClick = onImport, modifier = Modifier.testTag("data_privacy_import_json")) {
        Text(text = stringResource(R.string.data_privacy_import_json))
    }
}

@Composable
private fun DataPrivacyHeader(core: OmniGartCore) {
    val context = LocalContext.current
    Text(
        text = stringResource(R.string.data_privacy_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.testTag("data_privacy_title")
    )
    Text(
        text = stringResource(R.string.data_privacy_intro),
        color = OmniGartColors.colorTextLightSecondary,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.testTag("data_privacy_intro")
    )
    Text(
        text =
        stringResource(
            R.string.data_privacy_sync_status_line,
            statusLabel(core.localHealth.authStatus("HEALTH_CONNECT"), context)
        ),
        modifier = Modifier.testTag("data_privacy_sync_status_value")
    )
    core.localHealth.lastSyncEpochMs("HEALTH_CONNECT")?.let { ms ->
        val label =
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(ms))
        Text(
            text = stringResource(R.string.data_privacy_last_sync_line, label),
            modifier = Modifier.testTag("data_privacy_last_sync_value")
        )
    }
}

@Composable
private fun ConflictReview(core: OmniGartCore, conflictLines: List<String>, onResolved: () -> Unit) {
    if (conflictLines.isEmpty()) return
    Text(
        text = stringResource(R.string.data_privacy_conflicts_title),
        style = MaterialTheme.typography.titleSmall
    )
    conflictLines.forEach { line ->
        Text(text = line, style = MaterialTheme.typography.bodySmall)
    }
    Button(
        onClick = {
            core.localHealth.conflictSummaries().forEach { summary ->
                val id = summary.substringBefore("|")
                if (id.isNotBlank()) core.localHealth.resolveConflict(id)
            }
            onResolved()
        },
        modifier = Modifier.testTag("data_privacy_resolve_all")
    ) {
        Text(text = stringResource(R.string.data_privacy_resolve_all))
    }
}

@Composable
private fun rememberImportLauncher(core: OmniGartCore, onResult: (String) -> Unit) = run {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text =
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }
            if (text == null) {
                onResult(context.getString(R.string.data_privacy_import_failed))
                return@launch
            }
            val count = core.localHealth.importJsonUtf8(text)
            onResult(context.getString(R.string.data_privacy_import_ok, count))
        }
    }
}

@Composable
private fun rememberJsonExportLauncher(core: OmniGartCore, onReady: (String) -> Unit) = run {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = core.localHealth.exportJsonUtf8()
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
            }
            onReady(context.getString(R.string.data_privacy_export_ready))
        }
    }
}

private fun shareHealthExport(context: Context, core: OmniGartCore) {
    val json = core.localHealth.exportJsonUtf8()
    val csv = core.localHealth.exportArtifactUtf8("omnigart-workouts.csv").orEmpty()
    val intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "OmniGart health export")
            putExtra(Intent.EXTRA_TEXT, json.take(50_000) + "\n\n---\n" + csv.take(20_000))
        }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.data_privacy_export_share))
    )
}

private fun formatConflict(summary: String): String {
    val parts = summary.split("|")
    if (parts.size < 6) return summary
    return "${parts[2]}: ${parts[3]} vs ${parts[4]} (${parts[5]})"
}

private fun statusLabel(status: String, context: Context): String = when (status) {
    "AUTHORIZED" -> context.getString(R.string.data_privacy_status_authorized)
    "DENIED" -> context.getString(R.string.data_privacy_status_denied)
    "PARTIAL" -> context.getString(R.string.data_privacy_status_partial)
    else -> context.getString(R.string.data_privacy_status_unknown)
}
