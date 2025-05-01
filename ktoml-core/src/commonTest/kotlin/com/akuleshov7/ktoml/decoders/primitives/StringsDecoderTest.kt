package com.akuleshov7.ktoml.decoders.primitives

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StringDecoderTest {
    @Serializable
    data class Literals(
        val winpath: String?,
        val winpath2: String,
        val quoted: String,
        val regex: String,
    )

    @Test
    fun positiveScenario() {
        var test = """
                # What you see is what you get.
                winpath  = 'C:\Users\nodejs\templates'
                winpath2 = '\\ServerX\admin${'$'}\system32\'
                quoted   = 'Tom "Dubs" Preston-Werner'
                regex    = '<\i\c*\s*>'
            """

        var decoded = Toml.decodeFromString<Literals>(test)
        assertEquals(
            Literals(
                "C:\\Users\\nodejs\\templates",
                "\\\\ServerX\\admin${'$'}\\system32\\",
                "Tom \"Dubs\" Preston-Werner",
                "<\\i\\c*\\s*>"
            ),
            decoded
        )

        test = """
            winpath  = '\t'
            winpath2 = '\n'
            quoted   = '\r'
            regex    = '\f'
        """

        decoded = Toml.decodeFromString<Literals>(test)
        assertEquals(
            Literals(
                "\\t",
                "\\n",
                "\\r",
            "\\f"
            ),
            decoded
        )

        test = """
            winpath  = "\t"
            winpath2 = "\n"
            quoted   = "\r"
            regex    = "\f"
        """

        decoded = Toml.decodeFromString<Literals>(test)
        assertEquals(
            Literals(
                "\t",
                "\n",
                "\r",
                "\u000C"
            ),
            decoded
        )

        test = """
            winpath  = "\u0048"
            winpath2 = "\u0065"
            quoted   = "\u006C"
            regex    = "\u006F"
        """

        decoded = Toml.decodeFromString<Literals>(test)
        assertEquals(
            Literals(
                "H",
                "e",
                "l",
                "o"
            ),
            decoded
        )

        test = """
            winpath  = "\u0048\u0065\u006C\u006F"
            winpath2 = "My\u0048\u0065\u006C\u006FWorld"
            quoted   = "\u0048\u0065\u006C\u006F World"
            regex    = "My\u0048\u0065\u006CWorld"
        """

        decoded = Toml.decodeFromString<Literals>(test)
        assertEquals(
            Literals(
                "Helo",
                "MyHeloWorld",
                "Helo World",
                "MyHelWorld"
            ),
            decoded
        )

        test = """
            winpath  = '\u0048\u0065\u006C\u006F'
            winpath2 = 'My\u0048\u0065\u006C\u006FWorld'
            quoted   = '\u0048\u0065\u006C\u006F World'
            regex    = 'My\u0048\u0065\u006CWorld'
        """

        decoded = Toml.decodeFromString<Literals>(test)
        assertEquals(
            Literals(
                "\\u0048\\u0065\\u006C\\u006F",
                "My\\u0048\\u0065\\u006C\\u006FWorld",
                "\\u0048\\u0065\\u006C\\u006F World",
                "My\\u0048\\u0065\\u006CWorld"
            ),
            decoded
        )
    }

    @Test
    fun emptyStringTest() {
        var test = """
                winpath  = 
                winpath2 = ''
                quoted   = ""
                regex    = ''
            """

        val decoded = Toml.decodeFromString<Literals>(test)
        assertEquals(
            Literals(
                null,
                "",
                "",
                ""
            ),
            decoded
        )

        test = """
                winpath  = 
        """.trimIndent()

        assertFailsWith<ParseException> {
            Toml(TomlInputConfig(allowEmptyValues = false)).decodeFromString<Literals>(test)
        }
    }
}
