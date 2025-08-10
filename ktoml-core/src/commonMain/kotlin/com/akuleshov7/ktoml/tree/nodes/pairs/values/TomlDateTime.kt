package com.akuleshov7.ktoml.tree.nodes.pairs.values

import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.TomlWritingException
import com.akuleshov7.ktoml.writers.TomlEmitter
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.time.ExperimentalTime

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
            Instant.parse(replaceFirst(' ', 'T'))
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
    }
}
