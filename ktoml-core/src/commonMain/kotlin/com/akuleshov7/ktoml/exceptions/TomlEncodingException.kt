@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE", "MISSING_KDOC_TOP_LEVEL")

package com.akuleshov7.ktoml.exceptions

import kotlinx.serialization.SerializationException

public sealed class TomlEncodingException(message: String) : SerializationException(message)

internal class TomlWritingException(message: String) : TomlEncodingException(message)
