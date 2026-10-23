package com.omnigart.android.debug

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Debug variant: shows developer tooling. */
@Composable
fun MaybeDebugMenu(modifier: Modifier = Modifier) {
    DebugMenu(modifier = modifier)
}
