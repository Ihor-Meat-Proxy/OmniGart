package com.omnigart.android.debug

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Release variant: no debug UI. */
@Composable
fun MaybeDebugMenu(modifier: Modifier = Modifier) {
    // Intentionally empty — debug tooling is excluded from release builds.
}
