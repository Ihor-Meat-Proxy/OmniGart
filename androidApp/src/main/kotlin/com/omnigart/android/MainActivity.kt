package com.omnigart.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.omnigart.android.compliance.WellnessInfoSheet
import com.omnigart.android.debug.MaybeDebugMenu
import com.omnigart.android.food.FoodLoggingSection
import com.omnigart.android.insights.HealthTabSection
import com.omnigart.android.insights.InsightsTabSection
import com.omnigart.android.nutrition.FoodTabNutritionSection
import com.omnigart.android.nutrition.NutritionGoalSettingsSection
import com.omnigart.android.nutrition.NutritionPrefsStore
import com.omnigart.android.nutrition.NutritionReminderScheduler
import com.omnigart.android.nutrition.NutritionReminderSettingsSection
import com.omnigart.design.icons.OmniGartIcons
import com.omnigart.design.theme.OmniGartColors
import com.omnigart.design.theme.OmniGartSpacing
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.compliance.FeatureFlags
import com.omnigart.shared.content.AssetContentStore
import com.omnigart.shared.data.DatabaseDriverFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NutritionPrefsStore.init(applicationContext)
        NutritionReminderScheduler.reschedule(applicationContext)
        val core =
            OmniGartCore(
                DatabaseDriverFactory(applicationContext),
                contentStore = AssetContentStore(applicationContext.assets)
            )
        val profile = core.ensureDemoProfile()
        setContent {
            OmniGartTheme {
                OmniGartPhoneShell(core = core, profileId = profile.id)
            }
        }
    }
}

@Composable
fun OmniGartTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme =
        lightColorScheme(
            primary = OmniGartColors.colorBrandAccent,
            onPrimary = OmniGartColors.colorBrandAccentOn,
            primaryContainer = OmniGartColors.colorBrandAccentMuted,
            secondary = OmniGartColors.colorReadinessEasy,
            background = OmniGartColors.colorSurfaceLightCanvas,
            surface = OmniGartColors.colorSurfaceLightRaised,
            onBackground = OmniGartColors.colorTextLightPrimary,
            onSurface = OmniGartColors.colorTextLightPrimary,
            onSurfaceVariant = OmniGartColors.colorTextLightSecondary
        ),
        content = content
    )
}

private enum class PhoneTab(val labelRes: Int, val testTag: String, val icon: ImageVector) {
    Food(R.string.tab_food, "tab_food", OmniGartIcons.food),
    Workouts(R.string.tab_workouts, "tab_workouts", OmniGartIcons.workouts),
    Insights(R.string.tab_insights, "tab_insights", OmniGartIcons.insights),
    Health(R.string.tab_health, "tab_health", OmniGartIcons.health),
    Recovery(R.string.tab_recovery, "tab_recovery", OmniGartIcons.recovery),
    Planner(R.string.tab_planner, "tab_planner", OmniGartIcons.planner),
    Profile(R.string.tab_profile, "tab_profile", OmniGartIcons.profile)
}

private data class PhoneShellUiState(val selected: Int, val showInfoSheet: Boolean, val showMeasurements: Boolean, val formulaEpoch: Int)

@Composable
fun OmniGartPhoneShell(core: OmniGartCore, profileId: String) {
    var ui by remember {
        mutableStateOf(
            PhoneShellUiState(
                selected = PhoneTab.Profile.ordinal,
                showInfoSheet = false,
                showMeasurements = false,
                formulaEpoch = 0
            )
        )
    }
    val tabs = PhoneTab.entries
    val profile = remember(profileId) { core.profiles.get(profileId) }
    val summaryLines =
        remember(ui.selected, profileId, ui.formulaEpoch) {
            profile?.let { core.summaryLines(it) }.orEmpty()
        }
    Scaffold(
        modifier = Modifier.testTag("phone_shell"),
        containerColor = OmniGartColors.colorSurfaceLightCanvas,
        bottomBar = {
            PhoneBottomBar(
                tabs = tabs,
                selected = ui.selected,
                onSelect = { ui = ui.copy(selected = it) }
            )
        }
    ) { padding ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(OmniGartSpacing.space6)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space2)
        ) {
            PhoneShellHeader(tabs = tabs, selected = ui.selected, summaryLines = summaryLines)
            PhoneShellTabContent(core = core, profileId = profileId, ui = ui, onUi = { ui = it })
            MaybeDebugMenu()
        }
    }
}

@Composable
private fun PhoneShellHeader(tabs: List<PhoneTab>, selected: Int, summaryLines: List<String>) {
    Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.headlineMedium,
        color = OmniGartColors.colorBrandAccent,
        modifier = Modifier.testTag("brand_title")
    )
    Text(
        text = stringResource(R.string.shell_intro),
        color = OmniGartColors.colorTextLightSecondary
    )
    Text(
        text = stringResource(tabs[selected].labelRes),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.testTag("selected_tab_label")
    )
    summaryLines.forEach { line ->
        Text(text = line, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PhoneShellTabContent(core: OmniGartCore, profileId: String, ui: PhoneShellUiState, onUi: (PhoneShellUiState) -> Unit) {
    if (ui.selected == PhoneTab.Profile.ordinal || ui.selected == PhoneTab.Recovery.ordinal) {
        WellnessTabSection(
            showInfoSheet = ui.showInfoSheet,
            onToggleInfoSheet = { onUi(ui.copy(showInfoSheet = !ui.showInfoSheet)) }
        )
    }
    if (ui.selected == PhoneTab.Food.ordinal) {
        // Diary first so food_add / search stay above the fold for UI tests.
        FoodLoggingSection(core = core, profileId = profileId)
        FoodTabNutritionSection(
            core = core,
            profileId = profileId,
            showMeasurements = ui.showMeasurements,
            onShowMeasurements = { onUi(ui.copy(showMeasurements = it)) }
        )
    }
    if (ui.selected == PhoneTab.Workouts.ordinal) {
        com.omnigart.android.workouts.WorkoutsCategorySection(
            core = core,
            profileId = profileId
        )
    }
    if (ui.selected == PhoneTab.Insights.ordinal) {
        InsightsTabSection(core = core, profileId = profileId)
    }
    if (ui.selected == PhoneTab.Health.ordinal) {
        HealthTabSection()
    }
    if (ui.selected == PhoneTab.Profile.ordinal) {
        ProfileNutritionBlocks(core, profileId) {
            onUi(ui.copy(formulaEpoch = ui.formulaEpoch + 1))
        }
    }
    // Unfinished surfaces stay behind flags that are off in release.
    if (FeatureFlags.healthSyncEnabled) {
        Text(text = stringResource(R.string.perm_explainer_health_body))
    }
}

@Composable
private fun ProfileNutritionBlocks(core: OmniGartCore, profileId: String, onFormulaChanged: () -> Unit) {
    CalculationsSettingsSection(
        core = core,
        profileId = profileId,
        onSelectionChanged = onFormulaChanged
    )
    NutritionGoalSettingsSection()
    NutritionReminderSettingsSection()
    DataPrivacySection(core = core)
}

@Composable
private fun PhoneBottomBar(tabs: List<PhoneTab>, selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar(containerColor = OmniGartColors.colorSurfaceLightRaised) {
        tabs.forEachIndexed { index, tab ->
            NavigationBarItem(
                selected = selected == index,
                onClick = { onSelect(index) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = stringResource(tab.labelRes)
                    )
                },
                label = { Text(stringResource(tab.labelRes)) },
                colors =
                NavigationBarItemDefaults.colors(
                    selectedIconColor = OmniGartColors.colorBrandAccent,
                    selectedTextColor = OmniGartColors.colorBrandAccent,
                    indicatorColor = OmniGartColors.colorBrandAccentMuted,
                    unselectedIconColor = OmniGartColors.colorTextLightSecondary,
                    unselectedTextColor = OmniGartColors.colorTextLightSecondary
                ),
                modifier = Modifier.testTag(tab.testTag)
            )
        }
    }
}

@Composable
private fun WellnessTabSection(showInfoSheet: Boolean, onToggleInfoSheet: () -> Unit) {
    Text(
        text = stringResource(R.string.onboarding_wellness_title),
        style = MaterialTheme.typography.titleSmall
    )
    Text(
        text = stringResource(R.string.wellness_disclaimer),
        style = MaterialTheme.typography.bodySmall,
        color = OmniGartColors.colorTextLightSecondary,
        modifier = Modifier.testTag("wellness_disclaimer")
    )
    Text(
        text = stringResource(R.string.health_warning_chest_pain),
        style = MaterialTheme.typography.bodySmall,
        color = OmniGartColors.colorTextLightSecondary
    )
    IconButton(
        onClick = onToggleInfoSheet,
        modifier = Modifier.testTag("info_sheet_toggle")
    ) {
        Text(stringResource(R.string.info_sheet_title))
    }
    if (showInfoSheet) {
        WellnessInfoSheet()
    }
}
