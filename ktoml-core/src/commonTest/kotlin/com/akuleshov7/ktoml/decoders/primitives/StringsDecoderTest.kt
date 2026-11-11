package com.akuleshov7.ktoml.decoders.primitives

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.parsers.TomlParser
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
    fun hexAndEscEscapesInBasicStrings() {
        // \xHH (TOML 1.1 hex escape, equivalent to \u00HH) and \e (ESC, U+001B)
        val test = """
            winpath  = "\xE9"
            winpath2 = "S\xf8rmirb\xe6ren"
            quoted   = "\e There is no escape! \e"
            regex    = "Name\tJos\xE9\nSF."
        """

        val decoded = Toml.decodeFromString<Literals>(test)
        assertEquals(
            Literals(
                "é",
                "Sørmirbæren",
                "\u001B There is no escape! \u001B",
                "Name\tJosé\nSF."
            ),
            decoded
        )
    }

    @Test
    fun hexAndEscEscapesNotAppliedInLiteralStrings() {
        // literal strings must keep \x and \e verbatim
        val test = """
            winpath  = '\xE9'
            winpath2 = '\e'
            quoted   = '\x20 \x09'
            regex    = '\e\xFF'
        """

        val decoded = Toml.decodeFromString<Literals>(test)
        assertEquals(
            Literals(
                "\\xE9",
                "\\e",
                "\\x20 \\x09",
                "\\e\\xFF"
            ),
            decoded
        )
    }

    @Test
    fun nonScalarUnicodeEscapesAreRejectedDuringParse() {
        listOf(
            "bad = \"\\uD800\"",
            "bad = \"\\U00110000\"",
            "bad = \"\"\"\\uD801\"\"\"",
        ).forEach { toml ->
            assertFailsWith<ParseException> {
                TomlParser(TomlInputConfig.compliant()).parseString(toml)
            }
        }
    }

    @Test
    fun ideographicSpaceIsNotTomlWhitespace() {
        val parser = TomlParser(TomlInputConfig.compliant())
        assertFailsWith<ParseException> {
            parser.parseString("\u3000foo = \"bar\"")
        }
        parser.parseString("foo = \"bar\u3000baz\"")
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
