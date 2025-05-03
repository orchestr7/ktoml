package com.akuleshov7.ktoml.tree.nodes.pairs.keys

import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.TomlWritingException
import com.akuleshov7.ktoml.parsers.splitKeyToTokens
import com.akuleshov7.ktoml.parsers.trimAllQuotes
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
    public fun last(): String = keyParts.last().trimAllQuotes().trim()

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
