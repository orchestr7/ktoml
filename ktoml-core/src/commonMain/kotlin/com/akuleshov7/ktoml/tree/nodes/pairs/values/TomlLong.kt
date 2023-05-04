package com.akuleshov7.ktoml.tree.nodes.pairs.values

import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.writers.IntegerRepresentation
import com.akuleshov7.ktoml.writers.IntegerRepresentation.*
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * Toml AST Node for a representation of Arbitrary 64-bit signed integers: key = 1
 * @property content
 * @property representation The representation of the integer.
 */
public class TomlLong internal constructor(
    override var content: Any,
    public var representation: IntegerRepresentation = DECIMAL
) : TomlValue() {
    public constructor(content: String, lineNo: Int) : this(content.parse(lineNo))

    private constructor(pair: Pair<Long, IntegerRepresentation>) : this(pair.first, pair.second)

    override fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig
    ) {
        emitter.emitValue(content as Long, representation)
    }

    public companion object {
        private const val BIN_RADIX = 2
        private const val HEX_RADIX = 16
        private const val OCT_RADIX = 8
        private val prefixRegex = "(?<=0[box])".toRegex()

        private fun String.parse(lineNo: Int): Pair<Long, IntegerRepresentation> {
            val value = replace("_", "").split(prefixRegex, limit = 2)

            return if (value.size == 2) {
                val (prefix, digits) = value

                when (prefix) {
                    "0b" -> digits.toLong(BIN_RADIX) to BINARY
                    "0o" -> digits.toLong(OCT_RADIX) to OCTAL
                    "0x" -> digits.toLong(HEX_RADIX) to HEX
                    else -> throw ParseException(
                        "Invalid radix prefix for Long number <$this> $prefix: expected \"0b\", \"0o\", or \"0x\".",
                        lineNo
                    )
                }
            } else {
                value.first().toLong() to DECIMAL
            }
        }
    }
}
