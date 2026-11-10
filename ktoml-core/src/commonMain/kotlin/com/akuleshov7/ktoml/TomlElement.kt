/**
 * Public read-only handles for ktoml's parsed representation.
 */

package com.akuleshov7.ktoml

import com.akuleshov7.ktoml.annotations.ExperimentalKtomlApi
import com.akuleshov7.ktoml.tree.nodes.TableType
import com.akuleshov7.ktoml.tree.nodes.TomlArrayOfTablesElement
import com.akuleshov7.ktoml.tree.nodes.TomlFile
import com.akuleshov7.ktoml.tree.nodes.TomlInlineTable
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValue
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValueArray
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValuePrimitive
import com.akuleshov7.ktoml.tree.nodes.TomlNode
import com.akuleshov7.ktoml.tree.nodes.TomlTable
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlBasicString
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlBoolean
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlDateTime
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlDouble
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlLiteralString
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlLong
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlNull
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlUnsignedLong

/**
 * A read-only handle to an already-parsed TOML document or sub-tree.
 *
 * [TomlElement] is the TOML counterpart of `JsonElement` for custom serializers, but deliberately
 * exposes a smaller surface while this API is being evaluated. It hides ktoml's mutable, parent-linked
 * parser tree and lets callers inspect only the top-level shape, retain a sub-tree, and decode it later
 * with [Toml.decodeFromTomlElement].
 *
 * @property node backing internal parser node
 */
@ExperimentalKtomlApi
public class TomlElement internal constructor(internal val node: TomlNode) {
    /** The TOML value category represented by this element. */
    @Suppress("CUSTOM_GETTERS_SETTERS")
    public val kind: TomlElementKind
        get() = when (val value = node) {
            is TomlKeyValueArray -> TomlElementKind.ARRAY
            is TomlKeyValuePrimitive -> when (value.value) {
                is TomlBasicString, is TomlLiteralString -> TomlElementKind.STRING
                is TomlLong, is TomlUnsignedLong -> TomlElementKind.INTEGER
                is TomlDouble -> TomlElementKind.FLOAT
                is TomlBoolean -> TomlElementKind.BOOLEAN
                is TomlDateTime -> TomlElementKind.DATE_TIME
                is TomlNull -> TomlElementKind.NULL
                else -> TomlElementKind.TABLE
            }
            is TomlTable -> if (value.type == TableType.ARRAY) TomlElementKind.ARRAY else TomlElementKind.TABLE
            else -> TomlElementKind.TABLE
        }

    /**
     * Returns the names of key-value pairs and child tables directly contained in this element.
     *
     * @return names in document order
     */
    public fun keys(): Set<String> = node.children
        .filter { it is TomlKeyValue || it is TomlTable }
        .mapTo(linkedSetOf()) { it.name }

    /**
     * Returns the child tables directly contained in this element, keyed by their local names.
     *
     * @return child table handles in document order
     */
    public fun tables(): Map<String, TomlElement> = node.children
        .filterIsInstance<TomlTable>()
        .associateTo(linkedMapOf()) { it.name to TomlElement(it) }

    /**
     * Adapts this element to the file-shaped root expected by the existing internal decoders without
     * re-parsing or changing parent links in the retained parser tree.
     *
     * @return file-shaped decoder input
     */
    internal fun asFileNode(): TomlFile = when (val value = node) {
        is TomlFile -> value
        is TomlKeyValuePrimitive -> TomlFile().also { root ->
            root.appendChild(
                TomlKeyValuePrimitive(
                    value.key,
                    value.value,
                    value.lineNo,
                    value.comments,
                    value.inlineComment,
                )
            )
        }
        is TomlKeyValueArray -> TomlFile().also { root ->
            root.appendChild(
                TomlKeyValueArray(
                    value.key,
                    value.value,
                    value.lineNo,
                    value.comments,
                    value.inlineComment,
                )
            )
        }
        is TomlTable,
        is TomlArrayOfTablesElement,
        is TomlInlineTable -> TomlFile().also { it.children.addAll(value.children) }
        else -> TomlFile().also { it.children.addAll(value.children) }
    }

    internal fun isArrayOfTables(): Boolean = node is TomlTable && node.type == TableType.ARRAY
}

/** The minimal set of TOML value categories needed by format-aware custom serializers. */
@ExperimentalKtomlApi
public enum class TomlElementKind {
    ARRAY,
    BOOLEAN,
    DATE_TIME,
    FLOAT,
    INTEGER,
    NULL,
    STRING,
    TABLE,
    ;
}
