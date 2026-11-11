package com.akuleshov7.ktoml

import com.akuleshov7.ktoml.annotations.ExperimentalKtomlApi
import com.akuleshov7.ktoml.annotations.InternalKtomlApi
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder

/**
 * A TOML [Decoder] that can expose the already-parsed element at its current position.
 *
 * Custom serializers may cast their decoder to this interface, inspect [decodeTomlElement], and
 * delegate typed decoding back to [toml]. This mirrors the `JsonDecoder.decodeJsonElement()` pattern
 * without exposing ktoml's internal parser tree.
 *
 * This interface is intended for use, not third-party implementation.
 */
@ExperimentalKtomlApi
@SubclassOptInRequired(InternalKtomlApi::class)
public interface TomlDecoder : Decoder, CompositeDecoder {
    /** The format instance driving the current decoding operation. */
    public val toml: Toml

    /**
     * Returns the complete TOML element at the decoder's current position.
     *
     * Call this before invoking [beginStructure] or any other `decode*` method for the value handled
     * by the custom serializer. Calling it after partially consuming that value has unspecified behavior.
     *
     * @return already-parsed element at the current decoder position
     */
    public fun decodeTomlElement(): TomlElement
}
