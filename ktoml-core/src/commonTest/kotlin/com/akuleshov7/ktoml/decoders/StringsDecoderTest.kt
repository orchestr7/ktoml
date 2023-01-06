package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class StringDecoderTest {
    @Serializable
    data class Literals(
        val winpath: String,
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
    }
}
