package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.exceptions.InternalEncodingException
import com.akuleshov7.ktoml.tree.*
import kotlinx.serialization.SerializationStrategy

// Todo: Support "flat keys", i.e. a = { b.c = "..." }

/**
 * Encodes a TOML inline table.
 * @property currentKey The key of the current inline table.
 */
public class TomlInlineTableEncoder(
    override var currentKey: String,
    elementIndex: Int,
    config: TomlInputConfig
) : TomlAbstractEncoder(
    elementIndex,
    config,
    isInlineDefault = true
) {
    internal val keyValues: MutableList<TomlNode> = mutableListOf()

    override fun nextElementIndex() {
        // All inline table elements are on the same line; don't increment.
        elementIndex
    }

    override fun encodeValue(
        value: TomlValue,
        comments: List<String>,
        inlineComment: String
    ) {
        val key = TomlKey(currentKey, elementIndex)

        keyValues += if (value is TomlArray) {
            TomlKeyValueArray(
                key,
                value,
                elementIndex,
                comments,
                inlineComment,
                currentKey,
                config
            )
        } else {
            TomlKeyValuePrimitive(
                key,
                value,
                elementIndex,
                comments,
                inlineComment,
                currentKey,
                config
            )
        }
    }

    override fun encodeTable(value: TomlTable): Nothing {
        throw InternalEncodingException(
            "Non-inline tables are not allowed inside inline tables."
        )
    }

    override fun <T> encodeTableLike(
        serializer: SerializationStrategy<T>,
        value: T,
        isInline: Boolean,
        comments: List<String>,
        inlineComment: String
    ) {
        if (!isInline) {
            throw InternalEncodingException(
                "Non-inline tables are not allowed inside inline tables."
            )
        }

        val enc = TomlInlineTableEncoder(currentKey, elementIndex, config)

        serializer.serialize(enc, value)

        keyValues += TomlInlineTable(
            "",
            elementIndex,
            currentKey,
            enc.keyValues,
            comments,
            inlineComment,
            config
        )
    }
}
