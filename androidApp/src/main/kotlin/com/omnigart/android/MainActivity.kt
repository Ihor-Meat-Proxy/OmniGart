package com.omnigart.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.omnigart.design.icons.OmniGartIcons
import com.omnigart.design.theme.OmniGartColors
import com.omnigart.design.theme.OmniGartSpacing
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.data.DatabaseDriverFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val core = OmniGartCore(DatabaseDriverFactory(applicationContext))
        val profile = core.ensureDemoProfile()
        val summary = core.summaryLines(profile)
        setContent {
            OmniGartTheme {
                OmniGartPhoneShell(summaryLines = summary)
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
    Recovery(R.string.tab_recovery, "tab_recovery", OmniGartIcons.recovery),
    Planner(R.string.tab_planner, "tab_planner", OmniGartIcons.planner),
    Profile(R.string.tab_profile, "tab_profile", OmniGartIcons.profile)
}

@Composable
fun OmniGartPhoneShell(summaryLines: List<String>) {
    var selected by remember { mutableIntStateOf(PhoneTab.Profile.ordinal) }
    val tabs = PhoneTab.entries
    Scaffold(
        modifier = Modifier.testTag("phone_shell"),
        containerColor = OmniGartColors.colorSurfaceLightCanvas,
        bottomBar = {
            NavigationBar(
                containerColor = OmniGartColors.colorSurfaceLightRaised
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
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
    ) { padding ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(OmniGartSpacing.space6),
            verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space2)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = OmniGartColors.colorBrandAccent,
                modifier = Modifier.testTag("brand_title")
            )
            Text(
                text = stringResource(R.string.shell_placeholder),
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
    }
}
