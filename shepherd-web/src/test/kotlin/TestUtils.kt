package com.github.mvysny.shepherd.web

fun charSetOf(vararg range: CharRange): Set<Char> =
    range.flatMap { it.toSet() }.toSet()

private val offlineKeyAllowedChars = charSetOf('a'..'z', 'A'..'Z', '0'..'9') + setOf('_', '.', '-')

operator fun Set<Char>.times(count: Int): String = buildString(count) {
    repeat(count) {
        append(this@times.random())
    }
}

/**
 * Previously the offline key size was 1403 characters; new one is 2267.
 */
fun generateRandomOfflineKey(): String = offlineKeyAllowedChars * 2267
