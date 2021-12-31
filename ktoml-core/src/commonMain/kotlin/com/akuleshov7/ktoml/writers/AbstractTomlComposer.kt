package com.akuleshov7.ktoml.writers

import com.akuleshov7.ktoml.KtomlConf
import com.akuleshov7.ktoml.writers.IntegerRepresentation.*
import kotlin.jvm.JvmStatic

public abstract class AbstractTomlComposer(ktomlConf: KtomlConf) : TomlComposer {
    private val indentation = ktomlConf.indentation.string

    final override var indentDepth: Int = 0; protected set

    final override fun indent(): Int = ++indentDepth
    final override fun dedent(): Int = --indentDepth

    protected abstract fun emit(fragment: String)
    protected abstract fun emit(fragment: Char)

    protected abstract fun emit(fragment1: String, fragment2: String)
    protected abstract fun emit(fragment1: String, fragment2: String, fragment3: String)
    protected abstract fun emit(fragment1: Char, fragment2: String, fragment3: Char)

    protected open fun emit(vararg fragments: String): Unit = fragments.forEach(::emit)

    protected open fun emit(fragment: String, count: Int): Unit = repeat(count) { emit(fragment) }
    protected open fun emit(fragment: Char, count: Int): Unit = repeat(count) { emit(fragment) }

    final override fun emitNewLine(): Unit = emit('\n')

    final override fun emitIndent(): Unit = emit(indentation, indentDepth)

    final override fun emitWhitespace(count: Int): Unit = emit(' ', count)

    final override fun emitComment(comment: String, endOfLine: Boolean): Unit =
        if (endOfLine)
            emit(" # ", comment)
        else
            emit("# ", comment)

    final override fun emitKey(key: String) {
        if (key matches bareKeyRegex)
            emitBareKey(key)
        else
            emitQuotedKey(key, isLiteral = key matches literalKeyCandidateRegex)
    }

    final override fun emitBareKey(key: String): Unit = emit(key)

    final override fun emitQuotedKey(key: String, isLiteral: Boolean): Unit =
        emitValue(string = key, isLiteral)

    final override fun emitKeyDot(): Unit = emit('.')

    final override fun startTableHeader(): Unit = emit('[')

    final override fun endTableHeader(): Unit = emit(']')

    final override fun startTableArrayHeaderStart(): Unit = emit("[[")

    final override fun emitTableArrayHeaderEnd(): Unit = emit("]]")

    final override fun emitValue(string: String, isLiteral: Boolean, isMultiline: Boolean): Unit =
        if (isMultiline) {
            val quotes = if (isLiteral) "'''" else "\"\"\""

            emit("$quotes\n", string, quotes)
        }
        else {
            val quote = if (isLiteral) '\'' else '"'

            emit(quote, string, quote)
        }

    final override fun emitValue(integer: Long, representation: IntegerRepresentation): Unit =
        when (representation) {
            Decimal -> emit("$integer")
            Hex     -> emit("0x", integer.toString(16))
            Binary  -> emit("0b", integer.toString(2 ))
            Octal   -> emit("0o", integer.toString(8 ))
            Grouped -> TODO()
        }

    final override fun emitValue(float: Double): Unit =
        emit(when (float) {
            Double.NaN               -> "nan"
            Double.POSITIVE_INFINITY -> "inf"
            Double.NEGATIVE_INFINITY -> "-inf"
            else                     -> "$float"
        })

    final override fun emitValue(boolean: Boolean): Unit = emit("$boolean")

    final override fun startArray(): Unit = emit('[')

    final override fun endArray(): Unit = emit(']')

    final override fun startInlineTable(): Unit = emit('{')

    final override fun endInlineTable(): Unit = emit('}')

    final override fun emitElementDelimiter(): Unit = emit(", ")

    final override fun emitPairDelimiter(): Unit = emit(" = ")

    public companion object
    {
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