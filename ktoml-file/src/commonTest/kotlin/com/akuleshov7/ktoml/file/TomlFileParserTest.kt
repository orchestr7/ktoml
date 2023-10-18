package com.akuleshov7.ktoml.file

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.parsers.TomlParser
import com.akuleshov7.ktoml.source.useLines
import com.akuleshov7.ktoml.tree.nodes.TableType
import com.akuleshov7.ktoml.tree.nodes.TomlTable
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.Test

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
        val server: String,
    )

    @Serializable
    data class MyTest(
        val myserver: String,
        val myotherserver: String
    )

    @Test
    fun readParseAndDecodeFile() {
        val parsedResult =
            TomlFileReader().decodeFromFile<TestClass>(serializer(), "src/commonTest/resources/simple_example.toml")

        parsedResult shouldBe TestClass(
            "TOML \"Example\"",
            Owner(
                "Tom Preston-Werner",
                "1979-05-27T07:32:00-08:00",
                MyTest("test", "this is my \\ special \" [ value \" / ")
            ),
            Database(
                "192.168.1.1"
            )
        )
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
    @ExperimentalSerializationApi
    fun testTableDiscovery() {
        val file = "src/commonTest/resources/complex_toml_tables.toml"
        // ==== reading from file
        val parsedMyTableTest = TomlFileReader.decodeFromFile<MyTableTest>(serializer(), file)
        parsedMyTableTest shouldBe MyTableTest(
            A(Ab(InnerTest("Undefined")), InnerTest("Undefined")),
            D(InnerTest("Undefined"))
        )

        // ==== checking how table discovery works
        val parsedResult = getFileSource(file).useLines {
            TomlParser(TomlInputConfig()).parseStringsToTomlTree(it, TomlInputConfig())
        }
        val tableNames = parsedResult.getRealTomlTables().map { it.fullTableKey.toString() }
        tableNames shouldBe listOf("a", "a.b.c", "a.d", "d", "d.a")
    }

    @Serializable
    data class RegressionTest(val a: Long?, val b: Long, val c: Long, val d: Long?)

    @ExperimentalSerializationApi
    @Test
    fun regressionCastTest() {
        val file = "src/commonTest/resources/class_cast_regression.toml"
        val parsedResult = TomlFileReader.decodeFromFile<RegressionTest>(serializer(), file)
        parsedResult shouldBe RegressionTest(null, 1, 2, null)
    }

    @ExperimentalSerializationApi
    @Test
    fun regressionPartialTest() {
        val file = "src/commonTest/resources/class_cast_regression.toml"
        val parsedResult = TomlFileReader.decodeFromFile<RegressionTest>(serializer(), file)
        parsedResult shouldBe RegressionTest(null, 1, 2, null)
    }


    @Serializable
    data class TestRegression(
        val list1: List<Double>,
        val general: GeneralConfig,
        val list2: List<Long>,
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
        val parsedGeneralConfig = TomlFileReader.partiallyDecodeFromFile<GeneralConfig>(serializer(), file, "general")
        parsedGeneralConfig shouldBe GeneralConfig(
            execCmd = "echo hello world",
            tags = listOf("Tag", "Other tag"),
            description = "My description",
            suiteName = "// DocsCheck",
            excludedTests = null,
            includedTests = null,
            ignoreSaveComments = null
        )

        val parsedTestRegression = TomlFileReader.decodeFromFile<TestRegression>(serializer(), file)
        parsedTestRegression shouldBe TestRegression(
            list1 = listOf(1.0, 2.0),
            general = GeneralConfig(
                execCmd = "echo hello world",
                tags = listOf("Tag", "Other tag"),
                description = "My description",
                suiteName = "// DocsCheck",
                excludedTests = null,
                includedTests = null,
                ignoreSaveComments = null
            ),
            list2 = listOf(1, 3, 5),
            warn = WarnConfig(list = listOf("12a", "12f")),
            list3 = listOf("mystr", "2", "3")
        )
    }

    @Serializable
    data class Table1(val a: Long, val b: Long)

    @Test
    fun testPartialFileDecoding() {
        val file = "src/commonTest/resources/partial_decoder.toml"
        val parsedResult = TomlFileReader.partiallyDecodeFromFile<Table1>(serializer(), file, "table1")
        parsedResult shouldBe Table1(1, 2)
    }

    @Test
    fun readTopLevelTables() {
        val file = "src/commonTest/resources/simple_example.toml"
        val tableNames = getFileSource(file).useLines { lines ->
            TomlParser(TomlInputConfig())
                .parseStringsToTomlTree(lines, TomlInputConfig())
                .children
                .filterIsInstance<TomlTable>()
                .filter { it.type == TableType.PRIMITIVE && !it.isSynthetic }
                .map { it.fullTableKey.toString() }
        }

        tableNames shouldBe listOf("owner", "database")
    }

    @Test
    fun invalidFile() {
        val file = "src/commonTest/resources/simple_example.wrongext"
        shouldThrow<IllegalStateException> {
            getFileSource(file)
        }
    }
}
