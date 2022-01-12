package com.akuleshov7.ktoml.writers

import com.akuleshov7.ktoml.KtomlConf
import com.akuleshov7.ktoml.writers.IntegerRepresentation.BINARY
import com.akuleshov7.ktoml.writers.IntegerRepresentation.DECIMAL
import com.akuleshov7.ktoml.writers.IntegerRepresentation.GROUPED
import com.akuleshov7.ktoml.writers.IntegerRepresentation.HEX
import com.akuleshov7.ktoml.writers.IntegerRepresentation.OCTAL
import kotlin.jvm.JvmStatic

/**
 * Abstracts the specifics of writing TOML files into "emit" operations.
 */
public abstract class TomlEmitter(ktomlConf: KtomlConf) {
    private val indentation = ktomlConf.indentation.value

    @Suppress("CUSTOM_GETTERS_SETTERS")
    public var indentDepth: Int = 0
        protected set

    public fun indent(): Int = ++indentDepth
    public fun dedent(): Int = --indentDepth

    protected abstract fun emit(fragment: String)
    protected abstract fun emit(fragment: Char)

    public fun emitNewLine(): Unit = emit('\n')

    public fun emitIndent(): Unit = repeat(indentDepth) { emit(indentation) }

    public fun emitWhitespace(count: Int = 1): Unit = repeat(count) { emit(' ') }

    public fun emitComment(comment: String, endOfLine: Boolean = false) {
        emit(if (endOfLine) " # " else "# ")

        emit(comment)
    }

    public fun emitKey(key: String) {
        if (key matches bareKeyRegex) {
            emitBareKey(key)
        } else {
            emitQuotedKey(key, isLiteral = key matches literalKeyCandidateRegex)
        }
    }

    public fun emitBareKey(key: String): Unit = emit(key)

    public fun emitQuotedKey(key: String, isLiteral: Boolean = false): Unit =
            emitValue(string = key, isLiteral)

    public fun emitKeyDot(): Unit = emit('.')

    public fun startTableHeader(): Unit = emit('[')

    public fun endTableHeader(): Unit = emit(']')

    public fun emitTableArrayHeaderStart(): Unit = emit("[[")

    public fun emitTableArrayHeaderEnd(): Unit = emit("]]")

    public fun emitValue(
        string: String,
        isLiteral: Boolean = false,
        isMultiline: Boolean = false): Unit =
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

    public fun emitValue(float: Double): Unit =
            emit(when (float) {
                Double.NaN -> "nan"
                Double.POSITIVE_INFINITY -> "inf"
                Double.NEGATIVE_INFINITY -> "-inf"
                else -> float.toString()
            })

    public fun emitValue(boolean: Boolean): Unit = emit(boolean.toString())

    public fun startArray(): Unit = emit('[')

    public fun endArray(): Unit = emit(']')

    public fun startInlineTable(): Unit = emit('{')

    public fun endInlineTable(): Unit = emit('}')

    public fun emitElementDelimiter(): Unit = emit(", ")

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
