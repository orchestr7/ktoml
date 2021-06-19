package decoder

import com.akuleshov7.ktoml.deserialize
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable
data class SimpleArray(val a: List<Int>)

@Serializable
data class NestedArray(val a: List<List<Int>>)

class SimpleArrayDecoderTest {
    @Test
    fun testSimpleArrayDecoder() {
        val test = deserialize<SimpleArray>(
            "a = [1, 2,      3]"
        )

        assertEquals(SimpleArray(listOf(1, 2, 3)), test)

        // FixMe: nested array decoding causes issues and is not supported yet
        val testNested = deserialize<NestedArray>(
            "a = [[1, 2],      [3,  4]]"
        )

        assertEquals(NestedArray(listOf(listOf(1, 2), listOf(3, 4))), test)
    }
}
