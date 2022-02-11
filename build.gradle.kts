import com.akuleshov7.buildutils.*

plugins {
    kotlin("multiplatform") apply false
    kotlin("plugin.serialization") version Versions.KOTLIN apply false
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
createDiktatTask()
createDetektTask()
installGitHooks()

configurePublishing()
