package com.akuleshov7

import kotlinx.serialization.Serializable

@Serializable
data class Test(val a: String)

fun main() {
    println(Ktoml.decodeFromString<Int>("Hello"))
}
