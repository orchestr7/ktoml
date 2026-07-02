/**
 * This file contains datetime-related AST node types for TOML parsing.
 */

package com.akuleshov7.ktoml.tree.nodes.pairs.values

import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.TomlWritingException
import com.akuleshov7.ktoml.writers.TomlEmitter
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

private const val FRACTIONAL_SECOND_PRECISION = 3

/**
 * Preserves the original textual representation of an offset date-time while still exposing the parsed [instant].
 *
 * @property raw original TOML date-time text
 * @property instant parsed instant value
 */
@OptIn(ExperimentalTime::class)
public data class TomlOffsetDateTime(
    public val raw: String,
    public val instant: Instant,
) {
    override fun toString(): String = raw

    internal fun toRfc3339String(): String {
        val normalized = raw
            .replaceFirst(' ', 'T')
            .replaceFirst('t', 'T')
            .replaceFirst('z', 'Z')
        val timeStart = normalized.indexOf('T')
        if (timeStart == -1) {
            return normalized
        }

        val offsetStart = normalized.indexOfAny(charArrayOf('Z', '+', '-'), startIndex = timeStart + 1)
        if (offsetStart == -1) {
            return normalized
        }

        val timePart = normalized.substring(timeStart + 1, offsetStart)
        val dotIndex = timePart.indexOf('.')
        if (dotIndex == -1) {
            return normalized
        }

        val fraction = timePart.substring(dotIndex + 1)
        if (fraction.length >= FRACTIONAL_SECOND_PRECISION) {
            return normalized
        }

        val paddedTime = buildString {
            append(timePart.substring(0, dotIndex + 1))
            append(fraction.padEnd(FRACTIONAL_SECOND_PRECISION, '0'))
        }
        return buildString {
            append(normalized.substring(0, timeStart + 1))
            append(paddedTime)
            append(normalized.substring(offsetStart))
        }
    }
}

/**
 * Toml AST Node for a representation of date-time types (offset date-time, local date-time, local date, local time)
 * @property content
 */
public class TomlDateTime
internal constructor(
    override var content: Any
) : TomlValue() {
    public constructor(content: String, lineNo: Int) : this(content.trim().parseToDateTime())

    override fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig
    ) {
        @OptIn(ExperimentalTime::class)
        when (val content = content) {
            is Instant -> emitter.emitValue(content)
            is TomlOffsetDateTime -> emitter.emitValue(content)
            is LocalDateTime -> emitter.emitValue(content)
            is LocalDate -> emitter.emitValue(content)
            is LocalTime -> emitter.emitValue(content)
            else ->
                throw TomlWritingException(
                    "Unknown date type ${content::class.simpleName} of <$content>"
                )
        }
    }

    public companion object {
        @OptIn(ExperimentalTime::class)
        private fun String.parseToDateTime(): Any = try {
            // Offset date-time
            // TOML spec allows a space instead of the T, try replacing the first space by a T
            val normalized = this
                .replaceFirst(' ', 'T')
                .replaceFirst('t', 'T')
                .replaceFirst('z', 'Z')
            val instant = Instant.parse(normalized)
            if (normalized.hasExplicitOffset()) {
                TomlOffsetDateTime(this, instant)
            } else {
                instant
            }
        } catch (e: IllegalArgumentException) {
            try {
                // Local date-time
                LocalDateTime.parse(this)
            } catch (e: IllegalArgumentException) {
                try {
                    // Local date
                    LocalDate.parse(this)
                } catch (e: IllegalArgumentException) {
                    // Local time
                    LocalTime.parse(this)
                }
            }
        }

        private fun String.hasExplicitOffset(): Boolean {
            val timeStart = indexOf('T')
            if (timeStart == -1) {
                return false
            }
            return indexOfAny(charArrayOf('+', '-'), startIndex = timeStart + 1) != -1
        }
    }
}
