package com.akuleshov7.ktoml.tree.nodes.pairs.values

import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.utils.BIN_RADIX
import com.akuleshov7.ktoml.utils.HEX_RADIX
import com.akuleshov7.ktoml.utils.OCT_RADIX
import com.akuleshov7.ktoml.writers.IntegerRepresentation
import com.akuleshov7.ktoml.writers.IntegerRepresentation.*
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * Toml AST Node for a representation of Arbitrary 64-bit unsigned integers: key = 9_223_372_036_854_775_808
 * This node contains integer in range: [2^63, 2^64-1]. Integers less than 2^63 are treated as TomlLong
 * @property content
 * @property representation The representation of the integer.
 */
public class TomlUnsignedLong internal constructor(
    override var content: Any,
    public var representation: IntegerRepresentation = DECIMAL
) : TomlValue() {
    public constructor(content: String, lineNo: Int) : this(content.parse(lineNo))

    private constructor(pair: Pair<ULong, IntegerRepresentation>) : this(pair.first, pair.second)

    override fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig
    ) {
        emitter.emitValue(content as ULong, representation)
    }

    private companion object {
        private val prefixRegex = "(?<=0[box])".toRegex()

        private fun String.parse(lineNo: Int): Pair<ULong, IntegerRepresentation> {
            val value = replace("_", "").split(prefixRegex, limit = 2)

            return if (value.size == 2) {
                val (prefix, digits) = value

                when (prefix) {
                    "0b" -> digits.toULong(BIN_RADIX) to BINARY
                    "0o" -> digits.toULong(OCT_RADIX) to OCTAL
                    "0x" -> digits.toULong(HEX_RADIX) to HEX
                    else -> throw ParseException(
                        "Invalid radix prefix for ULong number <$this> $prefix: expected \"0b\", \"0o\", or \"0x\".",
                        lineNo
                    )
                }
            } else {
                value.first().toULong() to DECIMAL
            }
        }
    }
}
