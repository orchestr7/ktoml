import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

@Serializable
data class Server(val port: Int)

@Serializable
data class Config(val name: String, val values: List<Int>, val server: Server)

fun main() {
    val expected = Config("older compiler", listOf(1, 2, 3), Server(8080))
    val format = Toml(TomlInputConfig.compliant())
    val input = """
        name = "older compiler"
        values = [1, 2, 3]
        server = {
            port = 8080,
        }
    """.trimIndent()
    val parsed = format.decodeFromString<Config>(input)
    check(parsed == expected)
    check(format.decodeFromString<Config>(format.encodeToString(parsed)) == expected)
    println("Kotlin consumer compatibility: generated serializers and TOML round trip passed")
}
