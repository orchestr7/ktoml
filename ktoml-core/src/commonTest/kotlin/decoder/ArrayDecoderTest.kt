package decoder

import com.akuleshov7.ktoml.deserialize
import kotlinx.serialization.Serializable
import kotlin.test.Test

@Serializable
data class SimpleValue(val a: Int)

@Serializable
data class SimpleArray(val a: List<Int>)

class SimpleArrayDecoderTest {
    @Test
    fun testSimpleArrayDecoder() {
           val a = deserialize<SimpleArray>(
                "a = [\"1\", \"2\"]"
            )
        println(a)
    }

    @Test
    fun test() {
        deserialize<SimpleValue>(
            "a = 1"
        )
    }
}
