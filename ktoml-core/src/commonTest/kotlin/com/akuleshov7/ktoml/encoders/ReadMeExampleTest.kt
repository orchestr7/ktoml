package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.annotations.TomlInlineTable
import com.akuleshov7.ktoml.annotations.TomlLiteral
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadMeExampleTest {
    @Serializable
    data class MyClass(
        val someBooleanProperty: Boolean,
        val table1: Table1,
        val table2: Table2,
        @SerialName("gradle-libs-like-property")
        val kotlinJvm: GradlePlugin
    )

    @Serializable
    data class Table1(
        // nullable values, from toml you can pass null/nil/empty value to this kind of a field
        val property1: Long?,
        // please note, that according to the specification of toml integer values should be represented with Long
        val property2: Long,
        // no need to pass this value as it has the default value and is NOT REQUIRED
        val property3: Long = 5
    )

    @Serializable
    data class Table2(
        val someNumber: Long,
        @SerialName("akuleshov7.com")
        val inlineTable: NestedTable,
        val otherNumber: Double
    )

    @Serializable
    data class NestedTable(
        @TomlLiteral
        val name: String,
        @SerialName("configurationList")
        val overriddenName: List<String?>
    )

    @Serializable
    @TomlInlineTable
    data class GradlePlugin(val id: String, val version: Version)

    @Serializable
    data class Version(val ref: String)

    @Test
    fun readMeExampleTest() {
        val tomlText = """
            someBooleanProperty = true
            gradle-libs-like-property = { id = "org.jetbrains.kotlin.jvm", version = { ref = "kotlin" } }
            
            [table1]
                property1 = null
                property2 = 6
                property3 = 5
            
            [table2]
                someNumber = 5
                otherNumber = 5.56
            
                [table2.akuleshov7.com]
                    name = 'this is a "literal" string'
                    configurationList = [ "a", "b", "c", null ]
        """.trimIndent()

        val tomlTree = Toml.encodeToString(
            MyClass(
                true,
                Table1(null, 6),
                Table2(
                    5,
                    NestedTable("this is a \"literal\" string", listOf("a", "b", "c", null)),
                    5.56
                ),
                GradlePlugin("org.jetbrains.kotlin.jvm", Version("kotlin"))
            )
        )

        assertEquals(tomlText, tomlTree)
    }
}
