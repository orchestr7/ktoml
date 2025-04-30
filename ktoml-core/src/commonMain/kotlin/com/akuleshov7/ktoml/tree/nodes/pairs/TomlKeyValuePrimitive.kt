package com.akuleshov7.ktoml.tree.nodes

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.tree.nodes.pairs.keys.TomlKey
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlValue
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * class for parsing and storing simple single value types in AST
 *
 * @param comments
 * @param inlineComment
 * @property lineNo
 * @property key
 * @property value
 */
public class TomlKeyValuePrimitive(
    override var key: TomlKey,
    override val value: TomlValue,
    override val lineNo: Int,
    comments: List<String>,
    inlineComment: String
) : TomlNode(
    lineNo,
    comments,
    inlineComment
), TomlKeyValue {
    override val name: String = key.last()

    // adaptor for a string pair of key-value
    public constructor(
        keyValuePair: Pair<String, String>,
        lineNo: Int,
        comments: List<String> = emptyList(),
        inlineComment: String = "",
        config: TomlInputConfig = TomlInputConfig()
    ) : this(
        TomlKey(keyValuePair.first, lineNo),
        keyValuePair.second.parseValue(lineNo, config),
        lineNo,
        comments,
        inlineComment
    )

    public override fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig
    ): Unit = super<TomlKeyValue>.write(emitter, config)
}
