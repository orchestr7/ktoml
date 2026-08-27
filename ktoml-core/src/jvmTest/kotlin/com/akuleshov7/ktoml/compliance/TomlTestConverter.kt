package com.akuleshov7.ktoml.compliance

import com.akuleshov7.ktoml.tree.nodes.*
import com.akuleshov7.ktoml.tree.nodes.pairs.values.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.*
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

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
                is TomlValue -> valueToJson(element)
                is TomlArray -> arrayToJson(element)
                else -> error("Unknown array element type: ${element::class.simpleName}")
            }
        }
        return JsonArray(items)
    }

    // -- datetime --

    @OptIn(ExperimentalTime::class)
    private fun datetimeToJson(content: Any): JsonElement = when (content) {
        is Instant -> tagged("datetime", content.toString())
        is TomlOffsetDateTime -> tagged("datetime", content.toRfc3339String())
        is LocalDateTime -> tagged("datetime-local", formatLocalDateTime(content))
        is LocalDate -> tagged("date-local", content.toString())
        is LocalTime -> tagged("time-local", formatLocalTime(content))
        else -> error("Unknown datetime type: ${content::class.simpleName}")
    }

    // LocalTime.toString() drops seconds when they are 0 (e.g. "17:45" instead of "17:45:00").
    // toml-test always expects seconds.
    private fun formatLocalTime(t: LocalTime): String {
        val base = "%02d:%02d:%02d".format(t.hour, t.minute, t.second)
        return if (t.nanosecond != 0) {
            // Strip trailing zeros from fractional seconds
            val frac = "%09d".format(t.nanosecond).trimEnd('0')
            "$base.$frac"
        } else {
            base
        }
    }

    private fun formatLocalDateTime(dt: LocalDateTime): String =
        "${dt.date}T${formatLocalTime(dt.time)}"

    // -- helpers --

    private fun childrenToJson(children: List<TomlNode>): JsonObject = buildJsonObject {
        for (child in children) {
            when (child) {
                is TomlStubEmptyNode -> {} // skip
                is TomlTable -> put(child.name, tableToJson(child))
                is TomlKeyValuePrimitive -> put(child.name, valueToJson(child.value))
                is TomlKeyValueArray -> put(child.name, valueToJson(child.value))
                is TomlInlineTable -> put(child.name, childrenToJson(child.tomlKeyValues))
                is TomlArrayOfTablesElement -> {
                    // Shouldn't normally appear as direct child — handled via tableToJson
                    val obj = childrenToJson(child.children)
                    obj.forEach { (k, v) -> put(k, v) }
                }
                else -> error("Unhandled child type: ${child::class.simpleName}")
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
