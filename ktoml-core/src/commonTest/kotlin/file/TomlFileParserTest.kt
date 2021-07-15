package com.akuleshov7.ktoml.test.file

import com.akuleshov7.ktoml.KtomlConf
import com.akuleshov7.ktoml.deserializeTomlFile
import com.akuleshov7.ktoml.exceptions.NonNullableValueException
import com.akuleshov7.ktoml.parsers.TomlParser
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import okio.ExperimentalFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

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
        assertEquals(test, "src/commonTest/resources/simple_example.toml".deserializeTomlFile())
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

    @Test
    @ExperimentalFileSystem
    @ExperimentalSerializationApi
    fun testTableDiscovery() {
        val file = "src/commonTest/resources/complex_toml_tables.toml"
        // ==== reading from file
        val test = MyTableTest(A(Ab(InnerTest("Undefined")), InnerTest("Undefined")), D(InnerTest("Undefined")))
        assertEquals(test, file.deserializeTomlFile())
        // ==== checking how table discovery works
        val parsedResult = TomlParser(KtomlConf()).readAndParseFile(file)
        assertEquals(listOf("a", "a.b.c", "a.d", "d", "d.a"), parsedResult.getRealTomlTables().map { it.fullTableName })
    }

    @Serializable
    data class RegressionTest(val a: Int?, val b: Int, val c: Int, val d: Int?)

    @ExperimentalSerializationApi
    @Test
    fun regressionCast1Test() {
        assertFailsWith<NonNullableValueException> {
            val file = "src/commonTest/resources/class_cast_regression1.toml"
            file.deserializeTomlFile<RegressionTest>()
        }
    }

    @ExperimentalSerializationApi
    @Test
    fun regressionCast2Test() {
        val file = "src/commonTest/resources/class_cast_regression2.toml"
        val parsedResult = file.deserializeTomlFile<RegressionTest>()
        assertEquals(RegressionTest(null, 1, 2, null), parsedResult)
    }

    @ExperimentalSerializationApi
    @Test
    fun regressionPartialTest() {
        val file = "src/commonTest/resources/class_cast_regression2.toml"
        val parsedResult = file.deserializeTomlFile<RegressionTest>()
        assertEquals(RegressionTest(null, 1, 2, null), parsedResult)
    }


    @Serializable
    data class TestRegression(
        val list1: List<Double>,
        val general: GeneralConfig,
        val list2: List<Int>,
        val warn: WarnConfig,
        val list3: List<String>
    )

    @Serializable
    data class GeneralConfig(
        val execCmd: String? = null,
        val tags: List<String>? = null,
        val description: String? = null,
        val suiteName: String? = null,
        val excludedTests: List<String>? = null,
        val includedTests: List<String>? = null,
        val ignoreSaveComments: Boolean? = null
    )

    @Serializable
    data class WarnConfig(
        val list: List<String>
    )

    @ExperimentalSerializationApi
    @Test
    fun regressionInvalidIndex() {
        val file = "src/commonTest/resources/partial_parser_regression.toml"
        assertEquals(
            GeneralConfig(
                execCmd = "echo hello world",
                tags = listOf("Tag", "Other tag"),
                description = "My description",
                suiteName = "DocsCheck",
                excludedTests = null,
                includedTests = null,
                ignoreSaveComments = null
            ),
            file.deserializeTomlFile<GeneralConfig>("general")
        )
        assertEquals(
            TestRegression(
                list1 = listOf(1.0, 2.0),
                general = GeneralConfig(
                    execCmd = "echo hello world",
                    tags = listOf("Tag", "Other tag"),
                    description = "My description",
                    suiteName = "DocsCheck",
                    excludedTests = null,
                    includedTests = null,
                    ignoreSaveComments = null
                ),
                list2 = listOf(1, 3, 5),
                warn = WarnConfig(list = listOf("12a", "12f")),
                list3 = listOf("mystr", "2", "3")
            ),
            file.deserializeTomlFile<TestRegression>()
        )
    }
}
