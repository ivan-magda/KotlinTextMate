package dev.textmate.compose

import androidx.compose.ui.graphics.Color

object CodeBlock {
    const val VERSION = "0.1.0-SNAPSHOT"
}

/**
 * Converts an ARGB [Long] (as produced by [ResolvedStyle]) to a Compose [Color].
 */
fun Long.toComposeColor(): Color = Color(this.toInt())
