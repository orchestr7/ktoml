package com.akuleshov7.ktoml.test.file

import com.akuleshov7.ktoml.deserializeFile
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class TomlFileParserTest {
    @Serializable
    data class TestClass(
        val title: String,
        val owner: Owner,
        val database: Database
    )

    @Serializable
    data class Owner(
        val name: String,
        val dob: String,
        val mytest: MyTest
    )

    @Serializable
    data class Database(
        val server: String
    )

    @Serializable
    data class MyTest(
        val myserver: String
    )


    @ExperimentalSerializationApi
    @Test
    fun readParseAndDecodeFile() {
        val test = TestClass(
            "TOML Example",
            Owner(
                "Tom Preston-Werner",
                "1979-05-27T07:32:00-08:00",
                MyTest("test")
            ),
            Database(
                "192.168.1.1"
            )
        )
        assertEquals(test, deserializeFile("src/commonTest/resources/simple_example.toml"))
    }
}