package com.omnigart.android.debug

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Developer-only tooling. Lives under the `debug` package and must only be
 * composed when the APK is debuggable ([ApplicationInfo.FLAG_DEBUGGABLE]).
 * Release builds must never show this UI (enforced by CI + call-site gate).
 */
@Composable
fun DebugMenu(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val debuggable =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    if (!debuggable) return

    Column(
        modifier =
        modifier
            .padding(8.dp)
            .testTag("debug_menu")
    ) {
        Text("DebugMenu — development only")
    }
}

/** True only for debuggable installs; use before requesting debug UI. */
fun isDebugBuild(context: android.content.Context): Boolean = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
