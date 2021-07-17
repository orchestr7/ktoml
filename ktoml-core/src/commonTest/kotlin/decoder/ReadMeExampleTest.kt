package decoder

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadMeExampleTest {
    @Serializable
    data class MyClass(val someBooleanProperty: Boolean, val table1: Table1, val table2: Table2)

    @Serializable
    data class Table1(val property1: Long, val property2: Long)

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
        val overriddenName: List<String>
    )

    @Test
    fun readmeExampleTest() {
        val test =
            """
            |someBooleanProperty = true
            |
            |[table1]
            |property1 = 5
            |property2 = 6
            | 
            |[table2]
            |someNumber = 5
            |   [table2."akuleshov7.com"]
            |       name = "my name"
            |       configurationList = ["a",  "b",  "c"]
            |
            |# such redeclaration of table2
            |# is prohibited in toml specification;
            |# but ktoml is allowing it in non-strict mode: 
            |[table2]       
            |otherNumber = 5.56
            |       
            """.trimMargin()

        assertEquals(
            MyClass(
                someBooleanProperty = true,
                table1 = Table1(property1 = 5, property2 = 6),
                table2 = Table2(
                    someNumber = 5,
                    inlineTable = InlineTable(name = "my name", overriddenName = listOf("a", "b", "c")),
                    otherNumber = 5.56
                )
            ),
            Toml.decodeFromString(test)
        )
    }
}

