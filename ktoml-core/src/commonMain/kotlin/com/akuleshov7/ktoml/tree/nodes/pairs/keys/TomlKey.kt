package com.akuleshov7.ktoml.tree.nodes.pairs.keys

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.TomlWritingException
import com.akuleshov7.ktoml.parsers.splitKeyToTokens
import com.akuleshov7.ktoml.parsers.trimQuotes
import com.akuleshov7.ktoml.parsers.trimSingleQuotes
import com.akuleshov7.ktoml.writers.TomlEmitter
import com.akuleshov7.ktoml.writers.TomlStringEmitter

/**
 * Class that represents a toml key-value pair.
 * Key has TomlKey type, Value has TomlValue type
 *
 * @property keyParts The parts of the key, separated by dots.
 */
public class TomlKey internal constructor(
    internal val keyParts: List<String>
) {
    /**
     * Whether the key has multiple dot-separated parts.
     */
    internal val isDotted: Boolean = keyParts.size > 1

    @Deprecated(
        message = "rawContent is deprecated; use toString for a lazily formatted version. Will be removed in future releases.",
        replaceWith = ReplaceWith("toString()")
    )
    @Suppress("CUSTOM_GETTERS_SETTERS")
    public val rawContent: String get() = toString()

    @Deprecated(
        message = "content was replaced with last. Will be removed in future releases.",
        replaceWith = ReplaceWith("last()")
    )
    @Suppress("CUSTOM_GETTERS_SETTERS")
    public val content: String get() = last()

    /**
     * @param rawContent
     * @param lineNo
     */
    public constructor(
        rawContent: String,
        lineNo: Int
    ) : this(rawContent.splitKeyToTokens(lineNo))

    /**
     * Gets the last key part, with all whitespace and quotes trimmed, i.e. `c` in
     * `a.b.' c '`
     */
    public fun last(): String = keyParts.last()
        .trimQuotes()
        .trimSingleQuotes()
        .trim()

    @Deprecated(
        message = "TomlConfig is deprecated. Will be removed in next releases.",
        replaceWith = ReplaceWith("write(emitter)")
    )
    public fun write(emitter: TomlEmitter, config: TomlConfig): Unit = write(emitter)

    public fun write(emitter: TomlEmitter) {
        val keys = keyParts

        if (keys.isEmpty() || keys.any(String::isEmpty)) {
            throw TomlWritingException(
                "Empty keys are not allowed: key <$keys> is empty or has an empty key part."
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

    override fun toString(): String = buildString {
        val emitter = TomlStringEmitter(this, TomlOutputConfig())

        write(emitter)
    }

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is TomlKey -> false
        keyParts != other.keyParts -> false
        else -> true
    }

    override fun hashCode(): Int = keyParts.hashCode()
}
