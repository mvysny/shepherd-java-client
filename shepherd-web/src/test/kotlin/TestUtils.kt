package com.github.mvysny.shepherd.web

fun charSetOf(vararg range: CharRange): Set<Char> =
    range.flatMap { it.toSet() }.toSet()

private val offlineKeyAllowedChars = charSetOf('a'..'z', 'A'..'Z', '0'..'9') + setOf('_', '.', '-')

operator fun Set<Char>.times(count: Int): String = buildString(count) {
    repeat(count) {
        append(this@times.random())
    }
}

fun generateRandomOfflineKey(): String = offlineKeyAllowedChars * 1403
