package com.akuleshov7.ktoml.compliance

import com.akuleshov7.ktoml.parsers.parseKeyName
import com.akuleshov7.ktoml.tree.nodes.*
import com.akuleshov7.ktoml.tree.nodes.pairs.keys.TomlKey
import com.akuleshov7.ktoml.tree.nodes.pairs.values.*
import kotlinx.serialization.json.*

/**
 * Converts a ktoml AST (rooted at [TomlFile]) into the toml-test "tagged JSON" format.
 *
 * Spec: https://github.com/toml-lang/toml-test#tagged-json
 *
 * Every TOML value becomes `{"type": "<type>", "value": "<string>"}`.
 * Tables become plain JSON objects. Arrays become JSON arrays.
 */
object TomlTestConverter {

    fun toJson(root: TomlFile): JsonObject = childrenToJson(root.children)

    private fun tableToJson(table: TomlTable): JsonElement = when (table.type) {
        TableType.PRIMITIVE -> childrenToJson(table.children)
        TableType.ARRAY -> {
            val elements = table.children
                .filterIsInstance<TomlArrayOfTablesElement>()
                .map { childrenToJson(it.children) }
            JsonArray(elements)
        }
    }

    // -- value dispatch --

    private fun valueToJson(value: TomlValue): JsonElement = when (value) {
        is TomlBasicString -> tagged("string", value.content as String)
        is TomlLiteralString -> tagged("string", value.content as String)
        is TomlLong -> tagged("integer", (value.content as Long).toString())
        is TomlUnsignedLong -> tagged("integer", (value.content as ULong).toString())
        is TomlDouble -> tagged("float", formatFloat(value.content as Double))
        is TomlBoolean -> tagged("bool", (value.content as Boolean).toString())
        is TomlDateTime -> datetimeToJson(value.content)
        is TomlNull -> JsonNull
        is TomlArray -> arrayToJson(value)
    }

    // -- arrays --

    @Suppress("UNCHECKED_CAST")
    private fun arrayToJson(arr: TomlArray): JsonArray {
        val items = (arr.content as List<Any>).map { element ->
            when (element) {
                is TomlInlineTable -> inlineTableToJson(element)
                is TomlValue -> valueToJson(element)
                is TomlArray -> arrayToJson(element)
                else -> error("Unknown array element type: ${element::class.simpleName}")
            }
        }
        return JsonArray(items)
    }

    /**
     * Renders an inline table, expanding any dotted keys (`{ a.b.c = 1 }` -> `{"a":{"b":{"c": ...}}}`).
     *
     * An inline array of tables (`[{..}, {..}]`, modelled by ktoml as a single inline table whose
     * pairs are [TomlArrayOfTablesElement]s) is rendered as a JSON array instead of an object.
     */
    private fun inlineTableToJson(table: TomlInlineTable): JsonElement {
        if (table.tomlKeyValues.any { it is TomlArrayOfTablesElement }) {
            val elements = table.tomlKeyValues
                .filterIsInstance<TomlArrayOfTablesElement>()
                .map { childrenToJson(it.children) }
            return JsonArray(elements)
        }
        val root = linkedMapOf<String, Any>()
        table.tomlKeyValues.forEach { putChild(root, it) }
        return mapToJson(root)
    }

    // -- datetime --

    // ktoml keeps date-times as raw (normalized) text, so the tagged value is just that text.
    // Offset values are compared semantically by the suite (Instant-based), so a `.5` vs `.500`
    // fractional difference is tolerated; local values are already canonical (seconds padded, `T`
    // separator) and match the expected JSON verbatim.
    private fun datetimeToJson(content: Any): JsonElement = when (content) {
        is TomlOffsetDateTime -> tagged("datetime", content.raw)
        is TomlLocalDateTime -> tagged("datetime-local", content.raw)
        is TomlLocalDate -> tagged("date-local", content.raw)
        is TomlLocalTime -> tagged("time-local", content.raw)
        else -> error("Unknown datetime type: ${content::class.simpleName}")
    }

    // -- helpers --

    private fun childrenToJson(children: List<TomlNode>): JsonObject {
        val root = linkedMapOf<String, Any>()
        children.forEach { putChild(root, it) }
        return mapToJson(root)
    }

    /**
     * Adds a single AST child into [root], expanding dotted keys into nested maps so that
     * e.g. `dot.dot.dot = 1` becomes nested objects rather than a single `"dot.dot.dot"` key.
     */
    private fun putChild(root: MutableMap<String, Any>, child: TomlNode) {
        when (child) {
            is TomlStubEmptyNode -> {} // skip
            is TomlTable -> nestedPut(root, listOf(child.name), tableToJson(child))
            is TomlKeyValuePrimitive -> nestedPut(root, child.keyPartNames(), valueToJson(child.value))
            is TomlKeyValueArray -> nestedPut(root, child.keyPartNames(), valueToJson(child.value))
            is TomlInlineTable ->
                nestedPut(root, child.key?.keyPartNames() ?: listOf(child.name), inlineTableToJson(child))
            is TomlArrayOfTablesElement ->
                // Shouldn't normally appear as direct child — handled via tableToJson
                child.children.forEach { putChild(root, it) }
            else -> error("Unhandled child type: ${child::class.simpleName}")
        }
    }

    private fun TomlKeyValue.keyPartNames(): List<String> = key.keyPartNames()

    // Unquote and resolve escapes per fragment, the same way TomlKey.last() does for the last part.
    private fun TomlKey.keyPartNames(): List<String> = keyParts.map { it.parseKeyName(lineNo = 0) }

    /**
     * Inserts [value] at the dotted [path] inside [root], creating/merging intermediate maps.
     */
    @Suppress("UNCHECKED_CAST")
    private fun nestedPut(root: MutableMap<String, Any>, path: List<String>, value: JsonElement) {
        var current = root
        path.dropLast(1).forEach { part ->
            current = current.getOrPut(part) { linkedMapOf<String, Any>() } as MutableMap<String, Any>
        }
        current[path.last()] = value
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapToJson(map: Map<String, Any>): JsonObject = buildJsonObject {
        map.forEach { (key, value) ->
            when (value) {
                is JsonElement -> put(key, value)
                is Map<*, *> -> put(key, mapToJson(value as Map<String, Any>))
                else -> error("Unexpected value in nested map: ${value::class.simpleName}")
            }
        }
    }

    private fun tagged(type: String, value: String): JsonObject = buildJsonObject {
        put("type", type)
        put("value", value)
    }

    private fun formatFloat(d: Double): String = when {
        d.isNaN() -> "nan"
        d.isInfinite() -> if (d > 0) "inf" else "-inf"
        // Negative zero: Double.toBits() distinguishes +0.0 from -0.0
        d == 0.0 -> if (d.toBits() == (-0.0).toBits()) "-0" else "0"
        else -> {
            // BigDecimal.toString() uses scientific notation for large/small exponents,
            // plain notation otherwise — matching toml-test expectations.
            d.toBigDecimal().stripTrailingZeros().toString()
                .lowercase()
                // Pad single-digit exponents: e+6 → e+06, e-4 → e-04
                .replace(Regex("e([+-])(\\d)$")) { m ->
                    "e${m.groupValues[1]}0${m.groupValues[2]}"
                }
        }
    }
}
