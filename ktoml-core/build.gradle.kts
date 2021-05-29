import com.akuleshov7.buildutils.configurePublishing
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {

    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }

    linuxX64()
    mingwX64()
    macosX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.squareup.okio:okio-multiplatform:${Versions.OKIO}")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${Versions.SERIALIZATION}")
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
            dependsOn(commonTest)
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter-engine:5.0.0")
            }
        }
    }
}

configurePublishing()

tasks.withType<KotlinJvmTest> {
    useJUnitPlatform()
}
