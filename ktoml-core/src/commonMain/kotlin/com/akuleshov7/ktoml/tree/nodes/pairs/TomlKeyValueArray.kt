package com.akuleshov7.ktoml.tree.nodes

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.tree.nodes.pairs.keys.TomlKey
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlValue
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * Class for parsing and storing Array of Tables in AST.
 * @property lineNo
 * @property key
 * @property value
 */
public class TomlKeyValueArray(
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
        keyValuePair.second.parseList(lineNo, config),
        lineNo,
        comments,
        inlineComment
    )

    @Deprecated(
        message = "TomlConfig is deprecated; use TomlInputConfig instead. Will be removed in next releases."
    )
    public constructor(
        key: TomlKey,
        value: TomlValue,
        lineNo: Int,
        comments: List<String> = emptyList(),
        inlineComment: String = "",
        name: String,
        config: TomlConfig
    ) : this(
        key,
        value,
        lineNo,
        comments,
        inlineComment
    )

    @Deprecated(
        message = "TomlConfig is deprecated; use TomlInputConfig instead. Will be removed in next releases."
    )
    public constructor(
        keyValuePair: Pair<String, String>,
        lineNo: Int,
        comments: List<String> = emptyList(),
        inlineComment: String = "",
        config: TomlConfig
    ) : this(
        keyValuePair,
        lineNo,
        comments,
        inlineComment,
        config.input
    )

    public override fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig,
        multiline: Boolean
    ): Unit = super<TomlKeyValue>.write(emitter, config, multiline)
}
