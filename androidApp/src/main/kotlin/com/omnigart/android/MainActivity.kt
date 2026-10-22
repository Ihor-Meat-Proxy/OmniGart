package com.omnigart.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
        colorScheme = lightColorScheme(),
        content = content
    )
}

private enum class PhoneTab(val labelRes: Int, val testTag: String) {
    Food(R.string.tab_food, "tab_food"),
    Workouts(R.string.tab_workouts, "tab_workouts"),
    Recovery(R.string.tab_recovery, "tab_recovery"),
    Planner(R.string.tab_planner, "tab_planner"),
    Profile(R.string.tab_profile, "tab_profile")
}

@Composable
fun OmniGartPhoneShell(summaryLines: List<String>) {
    var selected by remember { mutableIntStateOf(PhoneTab.Profile.ordinal) }
    val tabs = PhoneTab.entries
    Scaffold(
        modifier = Modifier.testTag("phone_shell"),
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = {
                            Icon(
                                imageVector =
                                when (tab) {
                                    PhoneTab.Food -> Icons.Outlined.Restaurant
                                    PhoneTab.Workouts -> Icons.Outlined.FitnessCenter
                                    PhoneTab.Recovery -> Icons.Outlined.SelfImprovement
                                    PhoneTab.Planner -> Icons.Outlined.CalendarMonth
                                    PhoneTab.Profile -> Icons.Outlined.Person
                                },
                                contentDescription = stringResource(tab.labelRes)
                            )
                        },
                        label = { Text(stringResource(tab.labelRes)) },
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.testTag("brand_title")
            )
            Text(text = stringResource(R.string.shell_placeholder))
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
