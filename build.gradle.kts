import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

plugins {
    kotlin("multiplatform") version "1.4.10"
    kotlin("plugin.serialization") version "1.4.10"

}

group = "com.akuleshov7"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://kotlin.bintray.com/kotlinx/")
}



kotlin {
    jvm()
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val ktomlTarget = when {
        hostOs == "Mac OS X" -> macosX64("ktoml")
        hostOs == "Linux" -> linuxX64("ktoml")
        isMingwX64 -> mingwX64("ktoml")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    ktomlTarget.apply {
        binaries {
            executable {
                entryPoint = "com.akuleshov7.main"
            }
        }
    }

    sourceSets {

        val ktomlMain by getting {
            dependencies {
                implementation("com.squareup.okio:okio-multiplatform:3.0.0-alpha.1")
                implementation( "org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.1")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.1")
            }
        }
        val ktomlTest by getting

        val jvmMain by getting {
            dependsOn(ktomlMain)
        }

        val jvmTest by getting {
            dependsOn(ktomlTest)
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter-engine:5.0.0")
            }
        }
    }
}


tasks.withType<KotlinJvmTest> {
    useJUnitPlatform()
}

