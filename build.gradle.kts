import com.akuleshov7.buildutils.*

plugins {
    kotlin("multiplatform") apply false
    kotlin("plugin.serialization") version Versions.KOTLIN apply false
    id("io.kotest.multiplatform") version Versions.KOTEST apply false
    id("com.akuleshov7.buildutils.publishing-configuration")
}

configureVersioning()

allprojects {
    repositories {
        mavenCentral()
    }

    configureDiktat()
    configureDetekt()

    tasks.withType<org.cqfn.diktat.plugin.gradle.DiktatJavaExecTaskBase> {
        jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
    }
}

createDetektTask()
installGitHooks()

