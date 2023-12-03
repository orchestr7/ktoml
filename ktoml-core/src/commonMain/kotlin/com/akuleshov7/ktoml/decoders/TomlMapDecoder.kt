import com.akuleshov7.ktoml.decoders.TomlAbstractDecoder
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValue
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValuePrimitive
import com.akuleshov7.ktoml.tree.nodes.TomlTable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * @property rootNode
 */
@ExperimentalSerializationApi
public class TomlMapDecoder(
    private val rootNode: TomlTable,
) : TomlAbstractDecoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule()
    override fun decodeValue(): Any = rootNode.children.map {
        when(it) {
            is TomlKeyValue -> it.key to it.value
            else -> throw Exception()
        }
    }
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = 0

    override fun decodeKeyValue(): TomlKeyValue = throw NotImplementedError("")
}
