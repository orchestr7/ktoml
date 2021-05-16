package com.akuleshov7.ktoml.test.file

import com.akuleshov7.ktoml.deserializeFile
import com.akuleshov7.ktoml.parsers.TomlParser
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

    // ================
    @Serializable
    data class MyTableTest(
        val a: A,
        val d: D
    )

    @Serializable
    data class A(val b: Ab, val d: InnerTest)

    @Serializable
    data class Ab(val c: InnerTest)

    @Serializable
    data class D(val a: InnerTest)

    @Serializable
    data class InnerTest(val str: String = "Undefined")

    @ExperimentalSerializationApi
    @Test
    fun testTableDiscovery() {
        val file = "src/commonTest/resources/complex_toml_tables.toml"
        // ==== reading from file
        val test = MyTableTest(A(Ab(InnerTest("Undefined")), InnerTest("Undefined")), D(InnerTest("Undefined")))
        assertEquals(test, deserializeFile(file))
        // ==== checking how table discovery works
        val parsedResult = TomlParser(file).readAndParseFile()
        assertEquals(listOf("a", "a.b.c", "a.d", "d", "d.a"), parsedResult.getRealTomlTables().map { it.fullTableName })
    }
}
