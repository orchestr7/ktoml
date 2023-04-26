package com.akuleshov7.ktoml.file

import com.akuleshov7.ktoml.source.TomlSourceReader
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

class TomlSourceParserTest {
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
    fun readParseAndDecodeSource() {
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
        val buffer = Buffer()
        buffer.writeUtf8(SIMPLE_EXAMPLE)
        assertEquals(
            expected,
            TomlSourceReader.decodeFromSource(serializer(), buffer)
        )
    }

    companion object {
        private val SIMPLE_EXAMPLE = """
            # This is a TOML document.

            title = "TOML \"Example\""

            [owner]
            name = "Tom Preston-Werner"
            dob = "1979-05-27T07:32:00-08:00" # First class dates

            [database]
            server = "192.168.1.1"

            [owner.mytest]
            myserver = "test"
            myotherserver = 'this is my \ special " [ value " / '
        """.trimIndent()
    }
}
