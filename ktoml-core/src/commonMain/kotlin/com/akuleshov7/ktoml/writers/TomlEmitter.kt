package com.akuleshov7.ktoml.writers

import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.tree.nodes.TableType
import com.akuleshov7.ktoml.utils.isBareKey
import com.akuleshov7.ktoml.utils.isLiteralKeyCandidate
import com.akuleshov7.ktoml.writers.IntegerRepresentation.DECIMAL
import com.akuleshov7.ktoml.writers.IntegerRepresentation.GROUPED
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

/**
 * Abstracts the specifics of writing TOML files into "emit" operations.
 */
public abstract class TomlEmitter(config: TomlOutputConfig) {
    private val indentation = config.indentation.value

    /**
     * The current indent depth, set by [indent] and [dedent].
     */
    @Suppress("CUSTOM_GETTERS_SETTERS")
    public var indentDepth: Int = 0
        protected set

    /**
     * Increments [indentDepth], returning its value after incrementing.
     *
     * @return The new indent depth.
     */
    public fun indent(): Int = ++indentDepth

    /**
     * Decrements [indentDepth], returning its value after decrementing.
     *
     * @return The new indent depth.
     */
    public fun dedent(): Int = --indentDepth

    /**
     * Emits [fragment] as a raw [String] to the output.
     *
     * @param fragment The raw text to write to the output.
     * @return this instance
     */
    protected abstract fun emit(fragment: String): TomlEmitter

    /**
     * Emits [fragment] as a raw [Char] to the output.
     *
     * @param fragment The raw text to write to the output.
     * @return this instance
     */
    protected abstract fun emit(fragment: Char): TomlEmitter

    /**
     * Emits a newline character.
     *
     * @return this instance
     */
    public fun emitNewLine(): TomlEmitter = emit('\n')

    /**
     * Emits indentation up to the current [indentDepth].
     *
     * @return this instance
     */
    public fun emitIndent(): TomlEmitter {
        repeat(indentDepth) { emit(indentation) }

        return this
    }

    /**
     * Emits [count] whitespace characters.
     *
     * @param count The number of whitespace characters to write.
     * @return this instance
     */
    public fun emitWhitespace(count: Int = 1): TomlEmitter {
        repeat(count) { emit(' ') }

        return this
    }

    /**
     * Emits a [comment], optionally making it end-of-line.
     *
     * @param comment
     * @param inline Whether the comment is at the end of a line, e.g. after a
     * table header.
     * @return this instance
     */
    public fun emitComment(comment: String, inline: Boolean = false): TomlEmitter =
        emit(if (inline) " # " else "# ")
            .emit(comment)

    /**
     * Emits a [key]. Its type is inferred by its content, with bare keys being
     * preferred. [emitBareKey] is called for simple keys, [emitQuotedKey] for
     * non-simple keys.
     *
     * @param key
     * @return this instance
     */
    public fun emitKey(key: String): TomlEmitter =
        if (key.isBareKey()) {
            emitBareKey(key)
        } else {
            emitQuotedKey(key, isLiteral = key.isLiteralKeyCandidate())
        }

    /**
     * Emits a [key] as a bare key.
     *
     * @param key
     * @return this instance
     */
    public fun emitBareKey(key: String): TomlEmitter = emit(key)

    /**
     * Emits a [key] as a quoted key, optionally making it literal (single-quotes).
     *
     * @param key
     * @param isLiteral Whether the key should be emitted as a literal string
     * (single-quotes).
     * @return `this`
     */
    public fun emitQuotedKey(key: String, isLiteral: Boolean = false): TomlEmitter =
        emitValue(string = key, isLiteral)

    /**
     * Emits a key separator.
     *
     * @return this instance
     */
    public fun emitKeyDot(): TomlEmitter = emit('.')

    /**
     * Emits the table header start character(s).
     *
     * @param tableType The table type.
     * @return this instance
     */
    public fun startTableHeader(tableType: TableType): TomlEmitter = emit(tableType.open)

    /**
     * Emits the table header end character(s).
     *
     * @param tableType The table type.
     * @return this instance
     */
    public fun endTableHeader(tableType: TableType): TomlEmitter = emit(tableType.close)

    /**
     * Emits the table header start character.
     *
     * @return this instance
     */
    @Deprecated(
        message = "startTableHeader and startTableArrayHeader were merged; use " +
                "startTableHeader(TableType.PRIMITIVE). Will be removed in next" +
                " releases.",
        replaceWith = ReplaceWith(
            "startTableHeader(TableType.PRIMITIVE)",
            "com.akuleshov7.ktoml.tree.nodes.TableType"
        )
    )
    @Suppress("WRONG_OVERLOADING_FUNCTION_ARGUMENTS")
    public fun startTableHeader(): TomlEmitter = startTableHeader(TableType.PRIMITIVE)

    /**
     * Emits the table header end character.
     *
     * @return this instance
     */
    @Deprecated(
        message = "endTableHeader and endTableArrayHeader were merged; use end" +
                "TableHeader(TableType.PRIMITIVE). Will be removed in next releases.",
        replaceWith = ReplaceWith(
            "endTableHeader(TableType.PRIMITIVE)",
            "com.akuleshov7.ktoml.tree.nodes.TableType"
        )
    )
    @Suppress("WRONG_OVERLOADING_FUNCTION_ARGUMENTS")
    public fun endTableHeader(): TomlEmitter = endTableHeader(TableType.PRIMITIVE)

    /**
     * Emits the table array header start characters.
     *
     * @return this instance
     */
    @Deprecated(
        message = "startTableHeader and startTableArrayHeader were merged; use " +
                "startTableHeader(TableType.ARRAY). Will be removed in next releases.",
        replaceWith = ReplaceWith(
            "startTableHeader(TableType.ARRAY)",
            "import com.akuleshov7.ktoml.tree.nodes.TableType"
        )
    )
    public fun startTableArrayHeader(): TomlEmitter = startTableHeader(TableType.ARRAY)

    /**
     * Emits the table array header end characters.
     *
     * @return this instance
     */
    @Deprecated(
        message = "endTableHeader and endTableArrayHeader were merged; use end" +
                "TableHeader(TableType.ARRAY). Will be removed in next releases.",
        replaceWith = ReplaceWith(
            "endTableHeader(TableType.PRIMITIVE)",
            "import com.akuleshov7.ktoml.tree.nodes.TableType"
        )
    )
    public fun endTableArrayHeader(): TomlEmitter = endTableHeader(TableType.ARRAY)

    /**
     * Emit a string value, optionally making it literal and/or multiline.
     *
     * @param string
     * @param isLiteral Whether the string is literal (single-quotes).
     * @param isMultiline Whether the string is multiline.
     * @return this instance
     */
    public fun emitValue(
        string: String,
        isLiteral: Boolean = false,
        isMultiline: Boolean = false
    ): TomlEmitter =
        if (isMultiline) {
            val quotes = if (isLiteral) "'''" else "\"\"\""

            emit(quotes)
                .emitNewLine()
                .emit(string)
                .emitNewLine()
                .emit(quotes)
        } else {
            val quote = if (isLiteral) '\'' else '"'

            emit(quote)
                .emit(string)
                .emit(quote)
        }

    /**
     * Emits an integer value, optionally changing its representation from decimal.
     *
     * @param integer
     * @param representation How the integer will be represented in TOML.
     * @param groupSize The digit group size, or less than `1` for no grouping. For
     * example, a group size of `3` emits `1_000_000`, `4` emits `0b1111_1111`, etc.
     * @return this instance
     */
    @Suppress("SAY_NO_TO_VAR")
    public fun emitValue(
        integer: Long,
        representation: IntegerRepresentation = DECIMAL,
        groupSize: Int = 0
    ): TomlEmitter {
        // Todo: Add groupSize to the annotation and AST and remove GROUPED.
        if (representation == GROUPED) {
            return emitValue(integer, representation = DECIMAL, groupSize = 3)
        }

        if (integer < 0) {
            emit('-')
        }

        var digits = integer.toString(representation.radix).trimStart('-')

        if (groupSize > 0) {
            digits = (digits as CharSequence).reversed()
                .chunked(groupSize, CharSequence::reversed)
                .asReversed()
                .joinToString(separator = "_")
        }

        return emit(representation.prefix).emit(digits)
    }

    /**
     * Emits a floating-point value.
     *
     * @param float
     * @return this instance
     */
    public fun emitValue(float: Double): TomlEmitter =
        emit(when {
            float.isNaN() -> "nan"
            float.isInfinite() -> if (float > 0) "inf" else "-inf"
            // Whole-number floats are formatted as integers on JS.
            else -> float.toString().let {
                if ('.' in it) it else "$it.0"
            }
        })

    /**
     * Emits a boolean value.
     *
     * @param boolean
     * @return this instance
     */
    public fun emitValue(boolean: Boolean): TomlEmitter = emit(boolean.toString())

    /**
     * Emits an [Instant] value.
     *
     * @param instant
     * @return this instance
     */
    public fun emitValue(instant: Instant): TomlEmitter = emit(instant.toString())

    /**
     * Emits a [LocalDateTime] value.
     *
     * @param dateTime
     * @return this instance
     */
    public fun emitValue(dateTime: LocalDateTime): TomlEmitter = emit(dateTime.toString())

    /**
     * Emits a [LocalDate] value.
     *
     * @param date
     * @return this instance
     */
    public fun emitValue(date: LocalDate): TomlEmitter = emit(date.toString())

    /**
     * Emits a [LocalTime] value.
     *
     * @param time
     * @return this instance
     */
    public fun emitValue(time: LocalTime): TomlEmitter = emit(time.toString())

    /**
     * Emits a null value.
     *
     * @return this instance
     */
    public fun emitNullValue(): TomlEmitter = emit("null")

    /**
     * Emits the array start character.
     *
     * @return this instance
     */
    public fun startArray(): TomlEmitter = emit('[')

    /**
     * Emits the array end character.
     *
     * @return this instance
     */
    public fun endArray(): TomlEmitter = emit(']')

    /**
     * Emits the inline table start character.
     *
     * @return this instance
     */
    public fun startInlineTable(): TomlEmitter = emit('{')

    /**
     * Emits the inline table end character.
     *
     * @return this instance
     */
    public fun endInlineTable(): TomlEmitter = emit('}')

    /**
     * Emits an array/inline table element delimiter.
     *
     * @return this instance
     */
    public fun emitElementDelimiter(): TomlEmitter = emit(",")

    /**
     * Emits a key-value delimiter.
     *
     * @return this instance
     */
    public fun emitPairDelimiter(): TomlEmitter = emit(" = ")
}
