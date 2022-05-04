package com.akuleshov7.ktoml.source

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.parsers.TomlParser
import com.akuleshov7.ktoml.tree.TomlTablePrimitive

import okio.source
import org.junit.jupiter.api.Test

import java.io.InputStream

import kotlin.test.assertEquals
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

class StreamTests {
    @Test
    fun readParseAndDecodeStream() {
        val expected = TestClass(
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
        assertEquals(
            expected,
            Toml().decodeFromStream(getTestDataStream("simple_example.toml"))
        )
    }

    @Test
    @ExperimentalSerializationApi
    fun testTableDiscovery() {
        // ==== reading from stream
        val test = MyTableTest(
            A(Ab(InnerTest("Undefined")), InnerTest("Undefined")),
            D(InnerTest("Undefined"))
        )
        assertEquals(test, Toml().decodeFromStream(getTestDataStream("complex_toml_tables.toml")))
        // ==== checking how table discovery works
        val parsedResult =
                getTestDataStream("complex_toml_tables.toml").source().useLines { lines ->
                    TomlParser(TomlConfig()).parseStringsToTomlTree(lines, TomlConfig())
                }
        assertEquals(
            listOf("a", "a.b.c", "a.d", "d", "d.a"),
            parsedResult.getRealTomlTables().map { it.fullTableName })
    }

    @ExperimentalSerializationApi
    @Test
    fun regressionCast2Test() {
        val parsedResult =
                Toml().decodeFromStream<RegressionTest>(getTestDataStream("class_cast_regression2.toml"))
        assertEquals(RegressionTest(null, 1, 2, null), parsedResult)
    }

    @ExperimentalSerializationApi
    @Test
    fun regressionPartialTest() {
        val parsedResult =
                Toml().decodeFromStream<RegressionTest>(getTestDataStream("class_cast_regression2.toml"))
        assertEquals(RegressionTest(null, 1, 2, null), parsedResult)
    }

    @ExperimentalSerializationApi
    @Test
    fun regressionInvalidIndex() {
        assertEquals(
            GeneralConfig(
                execCmd = "echo hello world",
                tags = listOf("Tag", "Other tag"),
                description = "My description",
                suiteName = "// DocsCheck",
                excludedTests = null,
                includedTests = null,
                ignoreSaveComments = null
            ),
            Toml().partiallyDecodeFromStream(
                getTestDataStream("partial_parser_regression.toml"),
                "general"
            )
        )
        assertEquals(
            TestRegression(
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
            ),
            Toml().decodeFromStream(getTestDataStream("partial_parser_regression.toml"))
        )
    }

    @Test
    fun testPartialFileDecoding() {
        val test = TwoTomlTables(Table1(1, 2), Table2(1, 2, 3))
        assertEquals(
            test.table1,
            Toml().partiallyDecodeFromStream(
                getTestDataStream("partial_decoder.toml"),
                "table1"
            )
        )
    }

    @Test
    fun readTopLevelTables() {
        assertEquals(
            listOf("owner", "database"),
            getTestDataStream("simple_example.toml").source().useLines { lines ->
                TomlParser(TomlConfig())
                    .parseStringsToTomlTree(lines, TomlConfig())
                    .children
                    .filterIsInstance<TomlTablePrimitive>()
                    .filter { !it.isSynthetic }
                    .map { it.fullTableName }
            }
        )
    }

    private fun getTestDataStream(name: String): InputStream = requireNotNull(StreamTests::class.java.getResourceAsStream(name))
    /**
     * @property title
     * @property owner
     * @property database
     */
    @Serializable
    data class TestClass(
        val title: String,
        val owner: Owner,
        val database: Database
    )

    /**
     * @property name
     * @property dob
     * @property mytest
     */
    @Serializable
    data class Owner(
        val name: String,
        val dob: String,
        val mytest: MyTest
    )

    /**
     * @property server
     */
    @Serializable
    data class Database(
        val server: String,
    )

    /**
     * @property myserver
     * @property myotherserver
     */
    @Serializable
    data class MyTest(
        val myserver: String,
        val myotherserver: String
    )

    /**
     * @property a
     * @property d
     */
    // ================
    @Serializable
    data class MyTableTest(
        val a: A,
        val d: D
    )

    /**
     * @property b
     * @property d
     */
    @Serializable
    data class A(val b: Ab, val d: InnerTest)

    /**
     * @property c
     */
    @Serializable
    data class Ab(val c: InnerTest)

    /**
     * @property a
     */
    @Serializable
    data class D(val a: InnerTest)

    /**
     * @property str
     */
    @Serializable
    data class InnerTest(val str: String = "Undefined")

    /**
     * @property a
     * @property b
     * @property c
     * @property d
     */
    @Serializable
    data class RegressionTest(
        val a: Long?,
        val b: Long,
        val c: Long,
        val d: Long?
    )

    /**
     * @property list1
     * @property general
     * @property list2
     * @property warn
     * @property list3
     */
    @Serializable
    data class TestRegression(
        val list1: List<Double>,
        val general: GeneralConfig,
        val list2: List<Long>,
        val warn: WarnConfig,
        val list3: List<String>
    )

    /**
     * @property execCmd
     * @property tags
     * @property description
     * @property suiteName
     * @property excludedTests
     * @property includedTests
     * @property ignoreSaveComments
     */
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

    /**
     * @property list
     */
    @Serializable
    data class WarnConfig(
        val list: List<String>
    )

    /**
     * @property a
     * @property b
     */
    @Serializable
    data class Table1(val a: Long, val b: Long)

    /**
     * @property c
     * @property e
     * @property d
     */
    @Serializable
    data class Table2(
        val c: Long,
        val e: Long,
        val d: Long
    )

    /**
     * @property table1
     * @property table2
     */
    @Serializable
    data class TwoTomlTables(val table1: Table1, val table2: Table2)
}
