package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.annotations.*
import com.akuleshov7.ktoml.exceptions.InternalEncodingException
import com.akuleshov7.ktoml.writers.IntegerRepresentation

/**
 * @property parent The parent to inherit default values from.
 * @property key The current element's key.
 * @property isMultiline Marks subsequent key-string or array pair elements to
 * be written as multiline.
 * @property isLiteral Marks subsequent key-string pair elements to be written
 * as string literals.
 * @property intRepresentation Changes how subsequent key-integer pair elements
 * are represented.
 * @property isInline Marks subsequent table-like elements as inline. Tables
 * will be written as inline tables.
 * @property comments Comment lines to be prepended before the next element.
 * @property inlineComment A comment to be appended to the end of the next
 * element's line.
 * @property isImplicit Whether the current property is implicitly defined in
 * its child, i.e. the table `[a]` in `[a.b]`.
 */
public data class TomlEncoderAttributes(
    public val parent: TomlEncoderAttributes? = null,
    public var key: String? = null,
    public var isMultiline: Boolean = false,
    public var isLiteral: Boolean = false,
    public var intRepresentation: IntegerRepresentation = IntegerRepresentation.DECIMAL,
    public var isInline: Boolean = false,
    public var comments: List<String> = emptyList(),
    public var inlineComment: String = "",
    public var isImplicit: Boolean = false,
) {
    public fun keyOrThrow(): String = key ?: throw InternalEncodingException("Key not set")

    public fun child(): TomlEncoderAttributes = copy(parent = copy(), isImplicit = false)

    public fun set(annotations: Iterable<Annotation>) {
        annotations.forEach { annotation ->
            when (annotation) {
                is TomlLiteral -> isLiteral = true
                is TomlMultiline -> isMultiline = true
                is TomlInteger -> intRepresentation = annotation.representation
                is TomlComments -> {
                    comments = annotation.lines.asList()
                    inlineComment = annotation.inline
                }
                is TomlInlineTable -> isInline = true
            }
        }
    }

    public fun reset() {
        key = null

        val parent = parent ?: TomlEncoderAttributes()

        isMultiline = parent.isMultiline
        isLiteral = parent.isLiteral
        intRepresentation = parent.intRepresentation
        isInline = parent.isInline
        comments = parent.comments
        inlineComment = parent.inlineComment
        isImplicit = false
    }

    public fun getFullKey(): String {
        val elementKey = keyOrThrow()

        return parent?.let {
            "${it.getFullKey()}.$elementKey"
        } ?: elementKey
    }
}
