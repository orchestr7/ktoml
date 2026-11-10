import com.akuleshov7.buildutils.configureSigning
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest
import kotlinx.validation.KotlinApiBuildTask
import kotlinx.validation.KotlinApiCompareTask
import kotlinx.validation.KotlinKlibAbiBuildTask

plugins {
    kotlin("multiplatform")

    kotlin("plugin.serialization")
    id("com.akuleshov7.buildutils.publishing-configuration")
    id("com.saveourtool.diktat")
}

kotlin {
    explicitApi()

    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }

    js {
        browser()
        nodejs()
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

    mingwX64()
    linuxX64()
    linuxArm64()
    macosX64()
    macosArm64()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
        d8()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
            // ktoml's own experimental APIs are opted into internally; external consumers must opt in.
            languageSettings.optIn("com.akuleshov7.ktoml.annotations.ExperimentalKtomlApi")
            languageSettings.optIn("com.akuleshov7.ktoml.annotations.InternalKtomlApi")
        }

        val commonMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:${Versions.SERIALIZATION}")
                // NB: ktoml-core does NOT depend on kotlinx-datetime. Offset date-times use the stdlib
                // `kotlin.time.Instant`; local date/time values are parsed and validated by ktoml itself
                // and kept as raw text. Deserializing them into `kotlinx.datetime.LocalDate`/`LocalDateTime`/
                // `LocalTime` is opt-in: the consumer adds `org.jetbrains.kotlinx:kotlinx-datetime` (0.6.0+)
                // and ktoml bridges to its serializers by serial name. A consumer that never names those
                // types pulls no date library.
                implementation("org.jetbrains.kotlin:kotlin-stdlib:${Versions.KOTLIN}")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                // NB: ktoml-core's own tests intentionally do NOT depend on kotlinx-datetime — they prove
                // the library works with no date library on the classpath (offset date-times via the
                // stdlib `kotlin.time.Instant`, locals as raw text / `String`). Tests for the
                // `kotlinx.datetime.Local*` bridge live in the separate `:ktoml-datetime-tests` module.
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        val wasmJsTest by getting {
            dependencies {
                implementation(kotlin("test-wasm-js"))
            }
        }

        val wasmWasiTest by getting {
            dependencies {
                implementation(kotlin("test-wasm-wasi"))
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter-engine:5.14.1")
                implementation("org.junit.jupiter:junit-jupiter-params:5.14.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.SERIALIZATION}")
            }
        }
    }
}

configureSigning()

tasks.withType<KotlinJvmTest> {
    useJUnitPlatform()
}

// it is too time-consuming to support those tests
tasks.withType<KotlinJsTest> {
    if (this.name.contains("jsBrowserTest") ||
            this.name.contains("wasmJsBrowserTest") ||
            this.name.contains("wasmJsNodeTest") ||
            this.name.contains("wasmJsD8Test") ||
            this.name.contains("wasmWasiBrowserTest") ||
            this.name.contains("wasmWasiNodeTest")
    ) {
        this.enabled = false
    }
}

// This optional audit uses a published jar, independently of the committed fork baseline.
// It is deliberately not attached to check: upstream 0.7.1 has known ABI differences.
val referenceJvmApi by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

dependencies {
    referenceJvmApi(
        providers.gradleProperty("referenceApiCoordinate")
            .getOrElse("com.akuleshov7:ktoml-core-jvm:0.7.1")
    )
}

val referenceJvmApiDump by tasks.registering(KotlinApiBuildTask::class) {
    group = "verification"
    description = "Dumps a published core JVM jar for an independent ABI audit."
    inputJar.set(layout.file(referenceJvmApi.elements.map { it.single().asFile }))
    outputApiFile.set(layout.buildDirectory.file("api/reference/ktoml-core.api"))
    runtimeClasspath.from(tasks.named<KotlinApiBuildTask>("jvmApiBuild").map { it.runtimeClasspath })
}

tasks.register<KotlinApiCompareTask>("referenceJvmApiDiff") {
    group = "verification"
    description = "Reports all API differences against a published jar; known differences fail this audit."
    projectApiFile.set(referenceJvmApiDump.flatMap { it.outputApiFile })
    generatedApiFile.set(tasks.named<KotlinApiBuildTask>("jvmApiBuild").flatMap { it.outputApiFile })
}

val referenceJsApi by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

dependencies {
    referenceJsApi(
        providers.gradleProperty("referenceJsApiCoordinate")
            .getOrElse("com.akuleshov7:ktoml-core-js:0.7.1@klib")
    )
}

@OptIn(kotlinx.validation.ExperimentalBCVApi::class)
val referenceJsApiDump by tasks.registering(KotlinKlibAbiBuildTask::class) {
    group = "verification"
    description = "Dumps a published core JS KLib for an independent ABI audit."
    klibFile.from(referenceJsApi)
    val currentJsApi: TaskProvider<KotlinKlibAbiBuildTask> = tasks.named("jsApiBuild", KotlinKlibAbiBuildTask::class.java)
    target.set(currentJsApi.flatMap { it.target })
    signatureVersion.set(currentJsApi.flatMap { it.signatureVersion })
    runtimeClasspath.from(currentJsApi.map { it.runtimeClasspath })
    outputAbiFile.set(layout.buildDirectory.file("api/reference/ktoml-core-js.klib.api"))
}

tasks.register<KotlinApiCompareTask>("referenceJsApiDiff") {
    group = "verification"
    description = "Reports all KLib API differences against a published JS KLib."
    projectApiFile.set(referenceJsApiDump.flatMap { it.outputAbiFile })
    generatedApiFile.set(tasks.named<KotlinKlibAbiBuildTask>("jsApiBuild").flatMap { it.outputAbiFile })
}
