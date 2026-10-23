package com.omnigart.shared.util

/** Simple one-decimal formatting that works on Kotlin/Native (no JVM String.format). */
fun formatOneDecimal(value: Double): String {
    val negative = value < 0
    val abs = if (negative) -value else value
    val scaled = kotlin.math.round(abs * 10.0).toInt()
    val whole = scaled / 10
    val frac = scaled % 10
    val body = "$whole.$frac"
    return if (negative) "-$body" else body
}
