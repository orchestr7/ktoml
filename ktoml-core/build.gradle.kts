import com.akuleshov7.buildutils.configureSigning
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

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

    js(IR) {
        browser()
        nodejs()
    }

    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
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
        }

        val commonMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:${Versions.SERIALIZATION}")
                api("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
                implementation("org.jetbrains.kotlin:kotlin-stdlib:${Versions.KOTLIN}")
            }
        }

        val commonTest by getting {
            dependencies {
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
                implementation("org.junit.jupiter:junit-jupiter-engine:5.12.1")
            }
        }
        all {
            languageSettings.enableLanguageFeature("InlineClasses")
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
