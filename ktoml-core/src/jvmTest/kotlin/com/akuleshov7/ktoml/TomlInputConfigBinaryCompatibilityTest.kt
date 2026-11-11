package com.akuleshov7.ktoml

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals

class TomlInputConfigBinaryCompatibilityTest {
    private val javaConsumer = loadConsumer("UpstreamJavaConfigConsumer")
    private val kotlinConsumer = loadConsumer("UpstreamKotlinConfigConsumerKt")

    @Test
    fun upstreamJavaConstructorLinks() {
        assertEquals(
            TomlInputConfig(true, false, true, false, true, false),
            invoke(javaConsumer, "construct"),
        )
    }

    @Test
    fun upstreamJavaCopyLinksAndPreservesTablePolicy() {
        assertEquals(
            TomlInputConfig(false, true, false, true, false, true, allowTableRedefinition = false),
            invoke(javaConsumer, "copy", TomlInputConfig.compliant()),
        )
    }

    @Test
    fun upstreamKotlinDefaultConstructorLinks() {
        assertEquals(
            TomlInputConfig(ignoreUnknownNames = true, allowEmptyValues = false),
            invoke(kotlinConsumer, "constructWithDefaults"),
        )
    }

    @Test
    fun upstreamKotlinDefaultCopyLinksAndPreservesTablePolicy() {
        val config = TomlInputConfig.compliant()
        assertEquals(config.copy(ignoreUnknownNames = true), invoke(kotlinConsumer, "copyWithDefaults", config))
    }

    @Test
    fun upstreamKotlinUnchangedCopyPreservesAllOptions() {
        val config = TomlInputConfig.compliant()
        assertEquals(config, invoke(kotlinConsumer, "copyUnchanged", config))
    }

    private fun invoke(consumer: Class<*>, name: String, vararg configs: TomlInputConfig): TomlInputConfig {
        val parameterTypes = configs.map { TomlInputConfig::class.java }.toTypedArray()
        return consumer.getMethod(name, *parameterTypes).invoke(null, *configs) as TomlInputConfig
    }

    private fun loadConsumer(simpleName: String): Class<*> {
        val encoded = requireNotNull(javaClass.getResourceAsStream("/binary-compatibility/$simpleName.class.base64")) {
            "Missing upstream-compiled compatibility fixture: $simpleName"
        }.bufferedReader().use { it.readText() }
        val bytes = Base64.getMimeDecoder().decode(encoded)
        return ConsumerClassLoader().define("com.akuleshov7.ktoml.binarycompatibility.fixtures.$simpleName", bytes)
    }

    private class ConsumerClassLoader : ClassLoader(TomlInputConfig::class.java.classLoader) {
        fun define(name: String, bytes: ByteArray): Class<*> = defineClass(name, bytes, 0, bytes.size)
    }
}
