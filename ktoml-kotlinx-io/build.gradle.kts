import com.akuleshov7.buildutils.configureSigning
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
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

    applyDefaultHierarchyTemplate()

    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
            languageSettings.optIn("com.akuleshov7.ktoml.annotations.ExperimentalKtomlApi")
            languageSettings.optIn("com.akuleshov7.ktoml.annotations.InternalKtomlApi")
        }

        val commonMain by getting {
            dependencies {
                api(project(":ktoml-core"))
                api("org.jetbrains.kotlinx:kotlinx-io-core:${Versions.KOTLINX_IO}")
                implementation("org.jetbrains.kotlin:kotlin-stdlib:${Versions.KOTLIN}")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
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

configureSigning()

tasks.withType<KotlinJvmTest> {
    useJUnitPlatform()
}
