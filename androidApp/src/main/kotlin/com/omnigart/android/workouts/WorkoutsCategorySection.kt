package com.omnigart.android.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.omnigart.android.R
import com.omnigart.android.StrengthLoggingSection
import com.omnigart.android.StrengthProgramSection
import com.omnigart.android.treadmill.TreadmillRunSection
import com.omnigart.design.icons.OmniGartIcons
import com.omnigart.design.theme.OmniGartColors
import com.omnigart.design.theme.OmniGartSpacing
import com.omnigart.shared.OmniGartCore

private enum class WorkoutCategory(
    val titleRes: Int,
    val accent: Color,
    val icon: ImageVector,
    val historyTypes: List<String>,
    val searchTokens: List<String>
) {
    Running(
        R.string.workout_cat_running,
        OmniGartColors.colorHrZoneZ4,
        OmniGartIcons.running,
        listOf("RUNNING"),
        listOf("run", "zone", "treadmill", "outdoor")
    ),
    Strength(
        R.string.workout_cat_strength,
        OmniGartColors.colorBrandAccent,
        OmniGartIcons.strength,
        listOf("STRENGTH", "HIIT"),
        listOf("strength", "guided", "free", "functional", "equipment", "routine")
    ),
    YogaPilates(
        R.string.workout_cat_yoga_pilates,
        OmniGartColors.colorReadinessRest,
        OmniGartIcons.yoga,
        listOf("YOGA", "PILATES"),
        listOf("yoga", "pilates", "mindful")
    ),
    Swimming(
        R.string.workout_cat_swimming,
        OmniGartColors.colorHrZoneZ1,
        OmniGartIcons.swimming,
        listOf("SWIMMING"),
        listOf("swim", "pool", "open")
    ),
    Walking(
        R.string.workout_cat_walking,
        OmniGartColors.colorHrZoneZ2,
        Icons.AutoMirrored.Filled.DirectionsWalk,
        listOf("WALKING"),
        listOf("walk", "treadmill", "outdoor")
    )
}

@Composable
fun WorkoutsCategorySection(core: OmniGartCore, profileId: String) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<WorkoutCategory?>(null) }
    val openA11y = stringResource(R.string.workout_cat_a11y_open)

    if (selected != null) {
        CategoryDetail(
            core = core,
            profileId = profileId,
            category = selected!!,
            onBack = { selected = null }
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth().testTag("workout_cat_grid"),
        verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space3)
    ) {
        Text(
            text = stringResource(R.string.workout_intro),
            style = MaterialTheme.typography.bodySmall,
            color = OmniGartColors.colorTextLightSecondary
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().testTag("workout_cat_search"),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.workout_cat_search_placeholder)) },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(
                            Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.workout_cat_search_clear)
                        )
                    }
                }
            }
        )
        Text(
            text = stringResource(R.string.workout_cat_categories_title),
            style = MaterialTheme.typography.titleSmall
        )
        val filtered =
            WorkoutCategory.entries.filter { cat ->
                val q = query.trim().lowercase()
                if (q.isEmpty()) {
                    true
                } else {
                    stringResource(cat.titleRes).lowercase().contains(q) ||
                        cat.searchTokens.any { it.contains(q) || q.contains(it) }
                }
            }
        CategoryGrid(categories = filtered, openA11y = openA11y, onSelect = { selected = it })
        if (query.isNotBlank()) {
            SearchHits(core = core, profileId = profileId, query = query, onOpen = { selected = it })
        }
    }
}

@Composable
private fun CategoryGrid(
    categories: List<WorkoutCategory>,
    openA11y: String,
    onSelect: (WorkoutCategory) -> Unit
) {
    val rows = categories.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space2)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(OmniGartSpacing.space2)
            ) {
                row.forEach { cat ->
                    CategoryTile(
                        category = cat,
                        openA11y = openA11y,
                        onClick = { onSelect(cat) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryTile(
    category: WorkoutCategory,
    openA11y: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = stringResource(category.titleRes)
    Column(
        modifier =
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(OmniGartColors.colorSurfaceLightRaised)
            .clickable(onClick = onClick)
            .padding(OmniGartSpacing.space3)
            .height(88.dp)
            .testTag("workout_cat_tile_${category.name.lowercase()}")
            .semantics { contentDescription = "$title. $openA11y" },
        verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space2)
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = null,
            tint = category.accent,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = OmniGartColors.colorTextLightPrimary
        )
    }
}

@Composable
private fun SearchHits(
    core: OmniGartCore,
    profileId: String,
    query: String,
    onOpen: (WorkoutCategory) -> Unit
) {
    val q = query.trim().lowercase()
    val ready =
        remember(profileId, q) {
            core.strengthPrograms.readyMadeWorkouts(profileId).filter { w ->
                // Match id stem; full localized name lives in string resources keyed by nameKey.
                w.id.contains(q) || w.nameKey.lowercase().contains(q)
            }
        }
    Text(
        text = stringResource(R.string.workout_cat_search_results_title),
        style = MaterialTheme.typography.titleSmall
    )
    if (ready.isEmpty()) {
        Text(
            text = stringResource(R.string.workout_cat_search_no_results),
            style = MaterialTheme.typography.bodySmall,
            color = OmniGartColors.colorTextLightSecondary,
            modifier = Modifier.testTag("workout_cat_search_empty")
        )
    } else {
        ready.take(12).forEach { workout ->
            TextButton(
                onClick = { onOpen(WorkoutCategory.Strength) },
                modifier = Modifier.testTag("workout_cat_search_hit_${workout.id}")
            ) {
                Text(text = workout.id.replace('-', ' '))
            }
        }
    }
}

@Composable
private fun CategoryDetail(
    core: OmniGartCore,
    profileId: String,
    category: WorkoutCategory,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("workout_cat_detail_${category.name.lowercase()}"),
        verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space2)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("workout_cat_back")) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.workout_cat_categories_title)
                )
            }
            Text(
                text = stringResource(category.titleRes),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            text = stringResource(R.string.workout_cat_sessions_title),
            style = MaterialTheme.typography.titleSmall
        )
        when (category) {
            WorkoutCategory.Running -> {
                Text(
                    text = stringResource(R.string.workouts_zone2_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = OmniGartColors.colorTextLightSecondary
                )
                TreadmillRunSection(core = core, profileId = profileId, initialWalking = false)
            }
            WorkoutCategory.Strength -> {
                StrengthProgramSection(core = core, profileId = profileId)
                StrengthLoggingSection(core = core, profileId = profileId)
            }
            WorkoutCategory.YogaPilates -> {
                Text(
                    text = stringResource(R.string.workout_mindful_body_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = OmniGartColors.colorTextLightSecondary
                )
            }
            WorkoutCategory.Swimming -> {
                Text(
                    text = stringResource(R.string.workout_cat_swimming_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = OmniGartColors.colorTextLightSecondary
                )
            }
            WorkoutCategory.Walking -> {
                TreadmillRunSection(core = core, profileId = profileId, initialWalking = true)
            }
        }
        HistoryBlock(core = core, category = category)
    }
}

@Composable
private fun HistoryBlock(core: OmniGartCore, category: WorkoutCategory) {
    Text(
        text = stringResource(R.string.workout_cat_history_title),
        style = MaterialTheme.typography.titleSmall
    )
    val ids =
        remember(category) {
            core.localHealth.listRecentWorkoutIds(category.historyTypes, 12)
        }
    if (ids.isEmpty()) {
        Column(
            modifier = Modifier.testTag("workout_cat_empty_${category.name.lowercase()}")
        ) {
            Text(
                text = stringResource(R.string.workout_cat_empty_title),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.workout_cat_empty_body),
                style = MaterialTheme.typography.bodySmall,
                color = OmniGartColors.colorTextLightSecondary
            )
        }
    } else {
        ids.forEach { id ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("workout_cat_history_$id")
                    .padding(vertical = OmniGartSpacing.space1)
            ) {
                Text(
                    text = core.localHealth.workoutDetailTitle(id),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = core.localHealth.workoutDetailSubtitle(id),
                    style = MaterialTheme.typography.bodySmall,
                    color = OmniGartColors.colorTextLightSecondary
                )
            }
        }
    }
}
