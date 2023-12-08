package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.annotations.TomlComments
import io.kotest.matchers.should
import kotlinx.serialization.Serializable
import kotlin.test.Ignore
import kotlin.test.Test

class CommentEncoderTest {
    @Test
    @Ignore
    fun commentsTest() {
        @Serializable
        data class Table(
            @TomlComments("Comment", inline = "Comment")
            val value: Boolean = true
        )

        @Serializable
        data class File(
            @TomlComments("Comment", inline = "Comment")
            val a: Long = 0,
            val b: String = "",
            @TomlComments("Comment", inline = "Comment")
            val table: Table = Table()
        )

        File() should encodeInto(
            """
                # Comment
                a = 0 # Comment
                b = ""
                
                # Comment
                [table] # Comment
                    # Comment
                    value = true # Comment
            """.trimIndent()
        )
    }
}
