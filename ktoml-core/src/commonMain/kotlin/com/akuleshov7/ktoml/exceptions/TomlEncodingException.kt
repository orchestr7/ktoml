@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE", "MISSING_KDOC_TOP_LEVEL")

package com.akuleshov7.ktoml.exceptions

import kotlinx.serialization.SerializationException

/**
 * @param message
 */
public sealed class TomlEncodingException(message: String) : SerializationException(message)

/**
 * @param message
 */
internal class TomlWritingException(message: String) : TomlEncodingException(message)

/**
 * @param message
 */
internal class InternalEncodingException(message: String) : TomlEncodingException(message +
        " It's an internal error - you can do nothing with it, please report it to https://github.com/akuleshov7/ktoml/")

/**
 * @param message
 * @param lineNo
 */
// Todo: This needs a better name
internal class IllegalEncodingTypeException(message: String, lineNo: Int) : TomlEncodingException("Line $lineNo: $message")

/**
 * @param message
 */
internal class UnsupportedEncodingFeatureException(message: String) : TomlEncodingException(message)
