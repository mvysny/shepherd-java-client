package com.github.mvysny.shepherd.web.ui.components

import com.github.mvysny.kaributools.BrowserTimeZone
import com.vaadin.flow.data.renderer.BasicRenderer
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer
import com.vaadin.flow.function.SerializableSupplier
import com.vaadin.flow.function.ValueProvider
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Renders [Duration] as `m:ss`.
 */
class DurationRenderer<T>(valueProvider: ValueProvider<T, Duration>) : BasicRenderer<T, Duration>(valueProvider) {
    override fun getFormattedValue(duration: Duration?): String {
        if (duration == null) return ""
        val totalSeconds = duration.seconds
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}

/**
 * Formats [Instant] using [FormatStyle.SHORT].
 */
class InstantRenderer<T>(
    valueProvider: ValueProvider<T, Instant?>,
) : BasicRenderer<T, Instant>(valueProvider) {

    private val formatter: DateTimeFormatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.SHORT)
        .withZone(BrowserTimeZone.get)

    override fun getFormattedValue(instant: Instant?): String {
        return instant?.let { formatter.format(it) } ?: ""
    }
}
