package com.github.mvysny.shepherd.web

fun charSetOf(vararg range: CharRange): Set<Char> =
    range.flatMap { it.toSet() }.toSet()

private val offlineKeyAllowedChars = charSetOf('a'..'z', 'A'..'Z', '0'..'9') + setOf('_', '.', '-')

/**
 * Generates a string of length [stringLength]. Every character of the string
 * is randomly picked from given set of [chars].
 */
fun generateString(chars: Set<Char>, stringLength: Int): String = buildString(stringLength) {
    repeat(stringLength) {
        append(chars.random())
    }
}

/**
 * Previously the offline key size was 1403 characters; new one is 2267.
 */
fun generateRandomOfflineKey(): String = generateString(offlineKeyAllowedChars, 2267)
