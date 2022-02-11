package com.akuleshov7.ktoml.tree

import com.akuleshov7.ktoml.TomlConfig

/**
 * Class for parsing and storing Array of Tables in AST.
 * @property lineNo
 * @property key
 * @property value
 * @property name
 */
public class TomlKeyValueArray(
    override var key: TomlKey,
    override val value: TomlValue,
    override val lineNo: Int,
    override val name: String,
    config: TomlConfig = TomlConfig()
) : TomlNode(
    key,
    value,
    lineNo,
    config
), TomlKeyValue {
    // adaptor for a string pair of key-value
    public constructor(
        keyValuePair: Pair<String, String>,
        lineNo: Int,
        config: TomlConfig = TomlConfig()
    ) : this(
        TomlKey(keyValuePair.first, lineNo),
        keyValuePair.second.parseList(lineNo, config),
        lineNo,
        TomlKey(keyValuePair.first, lineNo).content
    )
}
