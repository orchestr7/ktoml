package com.akuleshov7.ktoml.parsers

/**
 * @param emptyValuesAllowed - control to allow/prohibit the following: a = # comment
 * @property emptyValuesAllowed
 */
data class ParserConf(val emptyValuesAllowed: Boolean = true)
