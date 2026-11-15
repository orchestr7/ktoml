package com.akuleshov7.ktoml.tree.nodes.pairs.values

import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.annotations.InternalKtomlApi
import com.akuleshov7.ktoml.exceptions.IllegalTypeException
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.parsers.isValidTomlIntegerLiteral
import com.akuleshov7.ktoml.utils.BIN_RADIX
import com.akuleshov7.ktoml.utils.HEX_RADIX
import com.akuleshov7.ktoml.utils.OCT_RADIX
import com.akuleshov7.ktoml.writers.IntegerRepresentation
import com.akuleshov7.ktoml.writers.IntegerRepresentation.*
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * Toml AST Node for a representation of Arbitrary 64-bit signed integers: key = 1
 * @property content
 * @property representation The representation of the integer.
 */
@InternalKtomlApi
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

    private companion object {
        private val prefixRegex = "(?<=0[box])".toRegex()

        private fun String.parse(lineNo: Int): Pair<Long, IntegerRepresentation> {
            if (!isValidTomlIntegerLiteral()) {
                throw NumberFormatException("Invalid TOML integer literal <$this>")
            }

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
                value.first().parseDecimal(lineNo)
            }
        }

        private fun String.parseDecimal(lineNo: Int): Pair<Long, IntegerRepresentation> = try {
            toLong() to DECIMAL
        } catch (e: NumberFormatException) {
            if (startsWith("-")) {
                throw IllegalTypeException("Integer <$this> is outside the signed Long range.", lineNo)
            }
            throw e
        }
    }
}
