package com.akuleshov7.ktoml.writers

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.writers.IntegerRepresentation.*

import kotlin.jvm.JvmStatic
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * Abstracts the specifics of writing TOML files into "emit" operations.
 */
public abstract class TomlEmitter(config: TomlConfig) {
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
     */
    protected abstract fun emit(fragment: String)

    /**
     * Emits [fragment] as a raw [Char] to the output.
     *
     * @param fragment The raw text to write to the output.
     */
    protected abstract fun emit(fragment: Char)

    /**
     * Emits a newline character.
     */
    public fun emitNewLine(): Unit = emit('\n')

    /**
     * Emits indentation up to the current [indentDepth].
     */
    public fun emitIndent(): Unit = repeat(indentDepth) { emit(indentation) }

    /**
     * Emits [count] whitespace characters.
     *
     * @param count The number of whitespace characters to write.
     */
    public fun emitWhitespace(count: Int = 1): Unit = repeat(count) { emit(' ') }

    /**
     * Emits a [comment], optionally making it end-of-line.
     *
     * @param comment
     * @param endOfLine Whether the comment is at the end of a line, e.g. after a
     * table header.
     */
    public fun emitComment(comment: String, endOfLine: Boolean = false) {
        emit(if (endOfLine) " # " else "# ")

        emit(comment)
    }

    /**
     * Emits a [key]. Its type is inferred by its content, with bare keys being
     * preferred. [emitBareKey] is called for simple keys, [emitQuotedKey] for
     * non-simple keys.
     *
     * @param key
     */
    public fun emitKey(key: String) {
        if (key matches bareKeyRegex) {
            emitBareKey(key)
        } else {
            emitQuotedKey(key, isLiteral = key matches literalKeyCandidateRegex)
        }
    }

    /**
     * Emits a [key] as a bare key.
     *
     * @param key
     */
    public fun emitBareKey(key: String): Unit = emit(key)

    /**
     * Emits a [key] as a quoted key, optionally making it literal (single-quotes).
     *
     * @param key
     * @param isLiteral Whether the key should be emitted as a literal string
     * (single-quotes).
     */
    public fun emitQuotedKey(key: String, isLiteral: Boolean = false): Unit =
            emitValue(string = key, isLiteral)

    /**
     * Emits a key separator.
     */
    public fun emitKeyDot(): Unit = emit('.')

    /**
     * Emits the table header start character.
     */
    public fun startTableHeader(): Unit = emit('[')

    /**
     * Emits the table header end character.
     */
    public fun endTableHeader(): Unit = emit(']')

    /**
     * Emits the table array header start characters.
     */
    public fun startTableArrayHeader(): Unit = emit("[[")

    /**
     * Emits the table array header end characters.
     */
    public fun endTableArrayHeader(): Unit = emit("]]")

    /**
     * Emit a string value, optionally making it literal and/or multiline.
     *
     * @param string
     * @param isLiteral Whether the string is literal (single-quotes).
     * @param isMultiline Whether the string is multiline.
     */
    public fun emitValue(
        string: String,
        isLiteral: Boolean = false,
        isMultiline: Boolean = false
    ): Unit =
            if (isMultiline) {
                val quotes = if (isLiteral) "'''" else "\"\"\""

                emit(quotes)
                emitNewLine()
                emit(string)
                emit(quotes)
            } else {
                val quote = if (isLiteral) '\'' else '"'

                emit(quote)
                emit(string)
                emit(quote)
            }

    /**
     * Emits an integer value, optionally changing its representation from decimal.
     *
     * @param integer
     * @param representation How the integer will be represented in TOML.
     */
    public fun emitValue(integer: Long, representation: IntegerRepresentation = DECIMAL): Unit =
            when (representation) {
                DECIMAL -> emit(integer.toString())
                HEX -> {
                    emit("0x")
                    emit(integer.toString(16))
                }
                BINARY -> {
                    emit("0b")
                    emit(integer.toString(2))
                }
                OCTAL -> {
                    emit("0o")
                    emit(integer.toString(8))
                }
                GROUPED -> TODO()
            }

    /**
     * Emits a floating-point value.
     *
     * @param float
     */
    public fun emitValue(float: Double): Unit =
            emit(when {
                float.isNaN() -> "nan"
                float.isInfinite() ->
                    if (float > 0) "inf" else "-inf"
                else -> float.toString()
            })

    /**
     * Emits a boolean value.
     *
     * @param boolean
     */
    public fun emitValue(boolean: Boolean): Unit = emit(boolean.toString())

    /**
     * Emits an [Instant] value.
     *
     * @param instant
     */
    public fun emitValue(instant: Instant): Unit = emit(instant.toString())

    /**
     * Emits a [LocalDateTime] value.
     *
     * @param dateTime
     */
    public fun emitValue(dateTime: LocalDateTime): Unit = emit(dateTime.toString())

    /**
     * Emits a [LocalDate] value.
     *
     * @param date
     */
    public fun emitValue(date: LocalDate): Unit = emit(date.toString())

    /**
     * Emits a null value.
     */
    public fun emitNullValue(): Unit = emit("null")

    /**
     * Emits the array start character.
     */
    public fun startArray(): Unit = emit('[')

    /**
     * Emits the array end character.
     */
    public fun endArray(): Unit = emit(']')

    /**
     * Emits the inline table start character.
     */
    public fun startInlineTable(): Unit = emit('{')

    /**
     * Emits the inline table end character.
     */
    public fun endInlineTable(): Unit = emit('}')

    /**
     * Emits an array/inline table element delimiter.
     */
    public fun emitElementDelimiter(): Unit = emit(",")

    /**
     * Emits a key-value delimiter.
     */
    public fun emitPairDelimiter(): Unit = emit(" = ")

    public companion object {
        @JvmStatic
        private val bareKeyRegex = Regex("[A-Za-z0-9_-]+")

        /**
         * Matches a key with at least one unescaped double quote and no single
         * quotes.
         */
        @JvmStatic
        private val literalKeyCandidateRegex = Regex("""[^'"]*((?<!\\)")((?<!\\)"|[^'"])*""")
    }
}
