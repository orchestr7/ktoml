package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import kotlinx.serialization.Serializable
import kotlin.test.Test

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
        """
            # What you see is what you get.
            winpath  = 'C:\Users\nodejs\templates'
            winpath2 = '\\ServerX\admin${'$'}\system32\'
            quoted   = 'Tom "Dubs" Preston-Werner'
            regex    = '<\i\c*\s*>'
        """.shouldDecodeInto(
            Literals(
                "C:\\Users\\nodejs\\templates",
                "\\\\ServerX\\admin${'$'}\\system32\\",
                "Tom \"Dubs\" Preston-Werner",
                "<\\i\\c*\\s*>"
            )
        )

        """
            winpath  = '\t'
            winpath2 = '\n'
            quoted   = '\r'
            regex    = '\f'
        """.shouldDecodeInto(
            Literals(
                "\\t",
                "\\n",
                "\\r",
            "\\f"
            )
        )

        """
            winpath  = "\t"
            winpath2 = "\n"
            quoted   = "\r"
            regex    = "\f"
        """.shouldDecodeInto(
            Literals(
                "\t",
                "\n",
                "\r",
                "\u000C"
            )
        )

        """
            winpath  = "\u0048"
            winpath2 = "\u0065"
            quoted   = "\u006C"
            regex    = "\u006F"
        """.shouldDecodeInto(
            Literals(
                "H",
                "e",
                "l",
                "o"
            )
        )

        """
            winpath  = "\u0048\u0065\u006C\u006F"
            winpath2 = "My\u0048\u0065\u006C\u006FWorld"
            quoted   = "\u0048\u0065\u006C\u006F World"
            regex    = "My\u0048\u0065\u006CWorld"
        """.shouldDecodeInto(
            Literals(
                "Helo",
                "MyHeloWorld",
                "Helo World",
                "MyHelWorld"
            ),
        )

        """
            winpath  = '\u0048\u0065\u006C\u006F'
            winpath2 = 'My\u0048\u0065\u006C\u006FWorld'
            quoted   = '\u0048\u0065\u006C\u006F World'
            regex    = 'My\u0048\u0065\u006CWorld'
        """.shouldDecodeInto(
            Literals(
                "\\u0048\\u0065\\u006C\\u006F",
                "My\\u0048\\u0065\\u006C\\u006FWorld",
                "\\u0048\\u0065\\u006C\\u006F World",
                "My\\u0048\\u0065\\u006CWorld"
            )
        )
    }

    @Test
    fun emptyStringTest() {
        """
            winpath  = 
            winpath2 = ''
            quoted   = ""
            regex    = ''
        """.shouldDecodeInto(
            Literals(
                null,
                "",
                "",
                ""
            )
        )

        """
            winpath  = 
        """.trimIndent()
            .shouldThrowExceptionWhileDecoding<Literals, ParseException>(
                tomlInstance = Toml(TomlInputConfig(allowEmptyValues = false))
            )
    }
}
