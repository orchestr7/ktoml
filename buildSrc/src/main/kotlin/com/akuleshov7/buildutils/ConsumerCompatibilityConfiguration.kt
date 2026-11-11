/**
 * Verifies that a Kotlin compiler older than the library's build compiler can consume ktoml.
 */

package com.akuleshov7.buildutils

import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.register

// Change this only when a serialization runtime upgrade requires a newer consumer compiler.
private const val CONSUMER_KOTLIN_VERSION = "2.3.0"

/**
 * Compiles and executes a JVM consumer with an independently resolved compiler and serialization
 * plugin. The consumer uses this build's real core jar and transitive runtime dependencies.
 */
fun Project.configureJvmConsumerCompatibility() {
    val compiler = configurations.create("consumerKotlinCompiler") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }
    val plugin = configurations.create("consumerSerializationPlugin") {
        isCanBeConsumed = false
        isCanBeResolved = true
        isTransitive = false
    }
    val runtime = configurations.create("consumerKtomlRuntime") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }
    dependencies.add(compiler.name, "org.jetbrains.kotlin:kotlin-compiler-embeddable:$CONSUMER_KOTLIN_VERSION")
    dependencies.add(compiler.name, "org.jetbrains:annotations:13.0")
    dependencies.add(plugin.name, "org.jetbrains.kotlin:kotlin-serialization-compiler-plugin-embeddable:$CONSUMER_KOTLIN_VERSION")
    dependencies.add(runtime.name, dependencies.project(mapOf("path" to ":ktoml-core", "configuration" to "jvmRuntimeElements")))

    val source = layout.projectDirectory.file("compatibility/jvm/Consumer.kt")
    val classes = layout.buildDirectory.dir("compatibility/jvm/classes")
    val compileConsumer = tasks.register<JavaExec>("compileJvmCompatibilityConsumer") {
        group = "verification"
        description = "Compiles a real JVM consumer with Kotlin $CONSUMER_KOTLIN_VERSION."
        mainClass.set("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
        classpath = compiler
        maxHeapSize = "512m"
        inputs.file(source)
        inputs.files(runtime, plugin)
        outputs.dir(classes)
        doFirst {
            setArgs(listOf(
                "-version", "-no-stdlib", "-no-reflect", "-jvm-target", "1.8",
                "-Xplugin=${plugin.singleFile.absolutePath}",
                "-classpath", runtime.asPath,
                "-d", classes.get().asFile.absolutePath,
                source.asFile.absolutePath,
            ))
        }
    }
    val verifyConsumer = tasks.register<JavaExec>("checkJvmConsumerCompatibility") {
        group = "verification"
        description = "Runs the consumer compiled with Kotlin $CONSUMER_KOTLIN_VERSION."
        dependsOn(compileConsumer)
        mainClass.set("ConsumerKt")
        classpath(classes, runtime)
        maxHeapSize = "256m"
    }
    tasks.named("check") {
        dependsOn(verifyConsumer)
    }
}
