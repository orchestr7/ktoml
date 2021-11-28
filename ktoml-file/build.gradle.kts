import com.akuleshov7.buildutils.configurePublishing
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    explicitApi()

    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }

    val os = getCurrentOperatingSystem()

    val target = listOf(when {
        os.isWindows -> mingwX64()
        os.isLinux -> linuxX64()
        os.isMacOsX -> macosX64()
        else -> throw GradleException("Unknown operating system $os")
    })

    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
        }

         val nativeMain by creating {
            dependencies {
                implementation("com.squareup.okio:okio:${Versions.OKIO}")
                implementation("org.jetbrains.kotlin:kotlin-stdlib:${Versions.KOTLIN}")
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("com.squareup.okio:okio:${Versions.OKIO}")
                implementation("org.jetbrains.kotlin:kotlin-stdlib:${Versions.KOTLIN}")
            }
        }

        val commonMain by getting {
            dependencies {
                implementation("com.squareup.okio:okio:${Versions.OKIO}")
                implementation("org.jetbrains.kotlin:kotlin-stdlib:${Versions.KOTLIN}")
                implementation(project(":ktoml-core"))
            }
        }

        target.forEach {
            getByName("${it.name}Main").dependsOn(nativeMain)
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

        all {
            languageSettings.enableLanguageFeature("InlineClasses")
        }
    }
}

configurePublishing()

tasks.withType<KotlinJvmTest> {
    useJUnitPlatform()
}
