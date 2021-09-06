package decoder

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.ExperimentalSerializationApi

class ReadMeExampleTest {
    @Serializable
    data class MyClass(val someBooleanProperty: Boolean, val table1: Table1, val table2: Table2)

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
        val inlineTable: InlineTable,
        val otherNumber: Double
    )

    @Serializable
    data class InlineTable(
        val name: String,
        @SerialName("configurationList")
        val overriddenName: List<String?>
    )

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun readmeExampleTest() {
        val test =
            """
            |someBooleanProperty = true
            |
            |[table1]
            |property1 = null
            |property2 = 6
            | 
            |[table2]
            |someNumber = 5
            |   [table2."akuleshov7.com"]
            |       name = 'this is a "literal" string'
            |       # empty lists are also supported
            |       configurationList = ["a",  "b",  "c", null]
            |
            |# such redeclaration of table2
            |# is prohibited in toml specification;
            |# but ktoml is allowing it in non-strict mode: 
            |[table2]       
            |otherNumber = 5.56
            |       
            """.trimMargin()

        val decoded = Toml.decodeFromString<MyClass>(test)

        assertEquals(
            MyClass(
                someBooleanProperty = true,
                table1 = Table1(property1 = null, property2 = 6),
                table2 = Table2(
                    someNumber = 5,
                    inlineTable = InlineTable(name = "this is a \"literal\" string", overriddenName = listOf("a", "b", "c", null)),
                    otherNumber = 5.56
                )
            ),
            decoded
        )
    }
}

