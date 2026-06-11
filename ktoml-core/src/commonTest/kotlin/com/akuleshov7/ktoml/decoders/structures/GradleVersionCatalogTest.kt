package com.akuleshov7.ktoml.decoders.structures

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Decoding a Gradle Version Catalog (`libs.versions.toml`) — issue orchestr7/ktoml#336.
 *
 * Exercises the combination that catalog files rely on: maps of objects ([Map] of [VersionDeclaration]
 * / [LibraryDeclaration] / [PluginDeclaration]), a `Map<String, List<String>>` for bundles, nested
 * inline tables as map values (`version = { ... }`), and a dotted `@SerialName("version.ref")` key.
 */
class GradleVersionCatalogTest {
    @Serializable
    data class VersionDeclaration(
        val require: String? = null,
        val strictly: String? = null,
        val prefer: String? = null,
        @SerialName("reject") val rejectList: List<String>? = null,
    )

    @Serializable
    data class LibraryDeclaration(
        val module: String? = null,
        val group: String? = null,
        val name: String? = null,
        @SerialName("version") val versionInline: VersionDeclaration? = null,
        @SerialName("version.ref") val versionRef: String? = null,
    )

    @Serializable
    data class PluginDeclaration(
        val id: String,
        @SerialName("version") val versionInline: VersionDeclaration? = null,
        @SerialName("version.ref") val versionRef: String? = null,
    )

    @Serializable
    data class GradleVersionCatalog(
        val versions: Map<String, VersionDeclaration> = emptyMap(),
        val libraries: Map<String, LibraryDeclaration> = emptyMap(),
        val bundles: Map<String, List<String>> = emptyMap(),
        val plugins: Map<String, PluginDeclaration> = emptyMap(),
    )

    private val toml = Toml(inputConfig = TomlInputConfig(ignoreUnknownNames = true))

    @Test
    fun decodesMapOfVersionObjects() {
        val decoded = toml.decodeFromString<GradleVersionCatalog>(
            """
            [versions]
            platform = { require = "252.23892" }
            kotlin = { strictly = "2.0.0" }
            """.trimIndent(),
        )
        assertEquals("252.23892", decoded.versions["platform"]?.require)
        assertEquals("2.0.0", decoded.versions["kotlin"]?.strictly)
    }

    @Test
    fun decodesDottedVersionRefSerialName() {
        val decoded = toml.decodeFromString<GradleVersionCatalog>(
            """
            [libraries]
            annotations = { group = "org.jetbrains", name = "annotations", "version.ref" = "platform" }
            """.trimIndent(),
        )
        val lib = decoded.libraries["annotations"]
        assertEquals("org.jetbrains", lib?.group)
        assertEquals("annotations", lib?.name)
        // the dotted "version.ref" key binds to the @SerialName("version.ref") field
        assertEquals("platform", lib?.versionRef)
    }

    @Test
    fun decodesFullCatalogWithAllFourSections() {
        val decoded = toml.decodeFromString<GradleVersionCatalog>(
            """
            [versions]
            platform = { require = "252.23892" }
            kotlin = { strictly = "2.0.0" }

            [libraries]
            annotations = { group = "org.jetbrains", name = "annotations", "version.ref" = "platform" }
            stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version = { require = "2.0.0" } }

            [bundles]
            common = ["annotations", "stdlib"]

            [plugins]
            kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", "version.ref" = "kotlin" }
            """.trimIndent(),
        )

        val expected = GradleVersionCatalog(
            versions = mapOf(
                "platform" to VersionDeclaration(require = "252.23892"),
                "kotlin" to VersionDeclaration(strictly = "2.0.0"),
            ),
            libraries = mapOf(
                "annotations" to LibraryDeclaration(
                    group = "org.jetbrains",
                    name = "annotations",
                    versionRef = "platform",
                ),
                "stdlib" to LibraryDeclaration(
                    module = "org.jetbrains.kotlin:kotlin-stdlib",
                    versionInline = VersionDeclaration(require = "2.0.0"),
                ),
            ),
            bundles = mapOf("common" to listOf("annotations", "stdlib")),
            plugins = mapOf(
                "kotlin-jvm" to PluginDeclaration(
                    id = "org.jetbrains.kotlin.jvm",
                    versionRef = "kotlin",
                ),
            ),
        )
        assertEquals(expected, decoded)
    }
}
