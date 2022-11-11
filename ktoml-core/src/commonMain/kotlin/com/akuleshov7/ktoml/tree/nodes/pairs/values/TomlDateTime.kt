package com.akuleshov7.ktoml.tree.nodes.pairs.values

import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.TomlWritingException
import com.akuleshov7.ktoml.writers.TomlEmitter
import kotlinx.datetime.*

/**
 * Toml AST Node for a representation of date-time types (offset date-time, local date-time, local date)
 * @property content
 */
public class TomlDateTime
internal constructor(
    override var content: Any,
    lineNo: Int
) : TomlValue(lineNo) {
    public constructor(content: String, lineNo: Int) : this(content.trim().parseToDateTime(), lineNo)

    override fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig,
        multiline: Boolean
    ) {
        when (val content = content) {
            is Instant -> emitter.emitValue(content)
            is LocalDateTime -> emitter.emitValue(content)
            is LocalDate -> emitter.emitValue(content)
            else ->
                throw TomlWritingException(
                    "Unknown date type ${content::class.simpleName}"
                )
        }
    }

    public companion object {
        private fun String.parseToDateTime(): Any = try {
            // Offset date-time
            toInstant()
        } catch (e: IllegalArgumentException) {
            try {
                // TOML spec allows a space instead of the T, try replacing the first space by a T
                replaceFirst(' ', 'T').toInstant()
            } catch (e: IllegalArgumentException) {
                try {
                    // Local date-time
                    toLocalDateTime()
                } catch (e: IllegalArgumentException) {
                    // Local date
                    toLocalDate()
                }
            }
        }
    }
}
