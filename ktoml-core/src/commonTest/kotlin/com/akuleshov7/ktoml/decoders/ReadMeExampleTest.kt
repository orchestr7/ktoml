package com.akuleshov7.ktoml.decoders

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlinx.serialization.ExperimentalSerializationApi

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
        // nullable property, from toml input you can pass "null"/"nil"/"empty" value (no quotes needed) to this field
        val property1: Long?,
        // please note, that according to the specification of toml integer values should be represented with Long,
        // but we allow to use Int/Short/etc. Just be careful with overflow
        val property2: Byte,
        // no need to pass this value in the input as it has the default value and so it is NOT REQUIRED
        val property3: Short = 5
    )

    @Serializable
    data class Table2(
        val someNumber: Long,
        @SerialName("akuleshov7.com")
        val inlineTable: NestedTable,
        val otherNumber: Double,
        // Char in a manner of Java/Kotlin is not supported in TOML, because single quotes are used for literal strings.
        // However, ktoml supports reading Char from both single-char string and from it's integer code
        val charFromString: Char,
        val charFromInteger: Char
    )

    @Serializable
    data class NestedTable(
        val name: String,
        @SerialName("configurationList")
        val overriddenName: List<String?>
    )

    @Serializable
    data class GradlePlugin(val id: String, val version: Version)

    @Serializable
    data class Version(val ref: String)

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun readmeExampleTest() {
        """
            someBooleanProperty = true
            # inline tables in gradle 'libs.versions.toml' notation
            gradle-libs-like-property = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
            
            [table1]
              # null is prohibited by the TOML spec, but allowed in ktoml for nullable types
              # so for 'property1' null value is ok. Use: property1 = null  
              property1 = 100 
              property2 = 6
             
            [table2]
              someNumber = 5
               [table2."akuleshov7.com"]
                 name = '''
                     this is a "literal" multiline
                     string
                 '''
                 # empty lists are also supported
                 configurationList = ["a",  "b",  "c"]
            
            # such redeclaration of table2
            # is prohibited in toml specification;
            # but ktoml is allowing it in non-strict mode: 
            [table2]
              otherNumber = 5.56
              # use single quotes
              charFromString = 'a'
              charFromInteger = 123
        """.shouldDecodeInto(
            MyClass(
                someBooleanProperty = true,
                table1 = Table1(property1 = 100, property2 = 6),
                table2 = Table2(
                    someNumber = 5,
                    inlineTable = NestedTable(
                        name = "                     this is a \"literal\" multiline\n" +
                                "                     string\n",
                        overriddenName = listOf("a", "b", "c")
                    ),
                    otherNumber = 5.56,
                    charFromString = 'a',
                    charFromInteger = '{'
                ),

                kotlinJvm = GradlePlugin("org.jetbrains.kotlin.jvm", Version("kotlin"))
            )
        )
    }
}
