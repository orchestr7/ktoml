package com.akuleshov7.ktoml.tree

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.exceptions.TomlWritingException
import com.akuleshov7.ktoml.parsers.splitKeyToTokens
import com.akuleshov7.ktoml.parsers.trimQuotes
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * Class that represents a toml key-value pair.
 * Key has TomlKey type, Value has TomlValue type
 *
 * @property rawContent
 * @property lineNo
 */
public class TomlKey(public val rawContent: String, public val lineNo: Int) {
    internal val keyParts = rawContent.splitKeyToTokens(lineNo)
    public val content: String = keyParts.last().trimQuotes().trim()
    internal val isDotted = isDottedKey()

    /**
     * checking that we face a key in the following format: a."ab.c".my-key
     *
     * @return true if the key is in dotted format (a.b.c)
     */
    private fun isDottedKey(): Boolean {
        var singleQuoteIsClosed = true
        var doubleQuoteIsClosed = true
        rawContent.forEach { ch ->
            when (ch) {
                '\'' -> singleQuoteIsClosed = !singleQuoteIsClosed
                '\"' -> doubleQuoteIsClosed = !doubleQuoteIsClosed
                else -> {
                    // this is a generated else block
                }
            }

            if (ch == '.' && doubleQuoteIsClosed && singleQuoteIsClosed) {
                return true
            }
        }
        return false
    }

    @Deprecated(
        message = "TomlConfig is deprecated",
        replaceWith = ReplaceWith("write(emitter)")
    )
    public fun write(emitter: TomlEmitter, config: TomlConfig): Unit = write(emitter)

    public fun write(emitter: TomlEmitter) {
        val keys = keyParts

        if (keys.isEmpty() || keys.any(String::isEmpty)) {
            throw TomlWritingException(
                "Empty keys are not allowed: the key at line $lineNo is empty or" +
                        " has an empty key part."
            )
        }

        keys.forEachIndexed { i, value ->
            if (i > 0) {
                emitter.emitKeyDot()
            }

            when {
                value.startsWith('"') && value.endsWith('"') ->
                    emitter.emitQuotedKey(value.substring(1 until value.lastIndex))
                value.startsWith('\'') && value.endsWith('\'') ->
                    emitter.emitQuotedKey(value.substring(1 until value.lastIndex), isLiteral = true)
                else -> emitter.emitBareKey(value)
            }
        }
    }
}
