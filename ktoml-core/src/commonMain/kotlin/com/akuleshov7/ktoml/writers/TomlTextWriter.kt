package com.akuleshov7.ktoml.writers

public interface TomlTextWriter {
    public val indentDepth: Int

    public fun indent(): Int
    public fun dedent(): Int

    public fun emitNewLine()
    public fun emitIndent()
    public fun emitWhitespace(count: Int = 1)

    public fun emitComment(comment: String, endOfLine: Boolean = false)

    public fun emitKey(key: String)
    public fun emitBareKey(key: String)
    public fun emitQuotedKey(key: String, isLiteral: Boolean = false)
    public fun emitKeyDot()

    public fun startTableHeader()
    public fun endTableHeader()

    public fun startTableArrayHeaderStart()
    public fun emitTableArrayHeaderEnd()

    public fun emitValue(string: String, isLiteral: Boolean = false, isMultiline: Boolean = false)
    public fun emitValue(integer: Long, representation: IntegerRepresentation = IntegerRepresentation.Decimal)
    public fun emitValue(float: Double)
    public fun emitValue(boolean: Boolean)
    // Todo: Add the KotlinX DateTime library and add support for date-time primitives
    //  in deserialization for consistency.
    // public fun emitValue(dateTime: OffsetDateTime)
    // public fun emitValue(dateTime: LocalDateTime)
    // public fun emitValue(date: LocalDate)
    // public fun emitValue(time: LocalTime)

    public fun startArray()
    public fun endArray()

    public fun startInlineTable()
    public fun endInlineTable()

    public fun emitElementDelimiter()

    public fun emitPairDelimiter()
}