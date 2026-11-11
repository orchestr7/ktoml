package com.akuleshov7.ktoml.tree.nodes.pairs.values

import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.parsers.isValidTomlFloatLiteral
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * Toml AST Node for a representation of float types: key = 1.01.
 * Toml specification requires floating point numbers to be IEEE 754 binary64 values,
 * so it should be Kotlin Double (64 bits)
 * @property content
 */
public class TomlDouble
internal constructor(
    override var content: Any
) : TomlValue() {
    public constructor(content: String, lineNo: Int) : this(content.parse())

    public constructor(content: Double, lineNo: Int) : this(content)

    override fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig
    ) {
        emitter.emitValue(content as Double)
    }

    private companion object {
        /**
         * Parses a TOML float literal into a [Double].
         *
         * Floats must have a decimal integer part without leading zeroes and either a fractional
         * part with digits on both sides of `.` or an exponent part. Underscores are allowed only
         * between digits in the integer, fractional, or exponent part.
         */
        private fun String.parse(): Double {
            if (!isValidTomlFloatLiteral()) {
                throw NumberFormatException("Invalid TOML float literal <$this>")
            }
            return replace("_", "").toDouble()
        }
    }
}
