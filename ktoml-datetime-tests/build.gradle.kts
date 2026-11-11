import org.jetbrains.kotlin.gradle.dsl.JvmTarget
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
