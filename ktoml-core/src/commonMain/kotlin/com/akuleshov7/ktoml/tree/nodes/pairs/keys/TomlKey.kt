package com.akuleshov7.ktoml.tree.nodes.pairs.keys

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.TomlWritingException
import com.akuleshov7.ktoml.parsers.splitKeyToTokens
import com.akuleshov7.ktoml.parsers.trimQuotes
import com.akuleshov7.ktoml.writers.TomlEmitter
import com.akuleshov7.ktoml.writers.TomlStringEmitter

/**
 * Class that represents a toml key-value pair.
 * Key has TomlKey type, Value has TomlValue type
 *
 * @property keyParts The parts of the key, separated by dots.
 * @property lineNo
 */
public class TomlKey internal constructor(
    internal val keyParts: List<String>,
    public val lineNo: Int
) {
    /**
     * Whether the key has multiple dot-separated parts.
     */
    internal val isDotted: Boolean = keyParts.isNotEmpty()

    @Deprecated(
        message = "rawContent is deprecated; use write instead. Will be removed in future releases."
    )
    @Suppress("CUSTOM_GETTERS_SETTERS")
    public val rawContent: String get() = buildString {
        val emitter = TomlStringEmitter(this, TomlOutputConfig())

        write(emitter)
    }

    @Deprecated(
        message = "content was replaced with toString. Will be removed in future releases.",
        replaceWith = ReplaceWith("toString()")
    )
    @Suppress("CUSTOM_GETTERS_SETTERS")
    public val content: String get() = toString()

    /**
     * @param rawContent
     * @param lineNo
     */
    public constructor(
        rawContent: String,
        lineNo: Int
    ) : this(
        rawContent.splitKeyToTokens(lineNo),
        lineNo
    )

    @Deprecated(
        message = "TomlConfig is deprecated. Will be removed in next releases.",
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

    override fun toString(): String = keyParts.last().trimQuotes().trim()
}
