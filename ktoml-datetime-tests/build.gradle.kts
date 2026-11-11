import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

/*
 * Test-only module (NOT published). It verifies the opt-in path: when a consumer adds kotlinx-datetime,
 * ktoml-core deserializes/serializes `kotlinx.datetime.LocalDate`/`LocalDateTime`/`LocalTime` via the
 * serializers kotlinx-datetime provides. ktoml-core itself does not depend on kotlinx-datetime; the
 * dependency lives here, so these tests run with it on the classpath while ktoml-core's own tests run
 * without it.
 */

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

    js {
        nodejs()
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
    wasmJs {
        nodejs()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }
    applyDefaultHierarchyTemplate()

    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
            languageSettings.optIn("com.akuleshov7.ktoml.annotations.ExperimentalKtomlApi")
            languageSettings.optIn("com.akuleshov7.ktoml.annotations.InternalKtomlApi")
        }

        val commonTest by getting {
            dependencies {
                implementation(project(":ktoml-core"))
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1-0.6.x-compat")
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
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
            }
        }
    }
}

tasks.withType<KotlinJvmTest> {
    useJUnitPlatform()
}

// Match core's test policy: run JS on Node; keep Wasm targets available without enabling their runners.
tasks.withType<KotlinJsTest> {
    if (name.startsWith("wasm")) {
        enabled = false
    }
}
