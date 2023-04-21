/**
 * Configuration for diktat static analysis
 */

package com.akuleshov7.buildutils

import org.cqfn.diktat.plugin.gradle.DiktatExtension
import org.cqfn.diktat.plugin.gradle.DiktatGradlePlugin
import org.cqfn.diktat.plugin.gradle.DiktatJavaExecTaskBase
import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

/**
 * Applies diktat gradle plugin and configures diktat for [this] project
 */
/**
 * Applies diktat gradle plugin and configures diktat for [this] project
 */
fun Project.configureDiktat() {
    apply<DiktatGradlePlugin>()
    configure<DiktatExtension> {
        diktatConfigFile = rootProject.file("diktat-analysis.yml")
        githubActions = findProperty("diktat.githubActions")?.toString()?.toBoolean() ?: false
        inputs {
            // using `Project#path` here, because it must be unique in gradle's project hierarchy
            if (path == rootProject.path) {
                include("$rootDir/buildSrc/src/**/*.kt", "$rootDir/*.kts", "$rootDir/buildSrc/**/*.kts")
                exclude("src/test/**/*.kt", "src/commonTest/**/*.kt")  // path matching this pattern will not be checked by diktat
            } else {
                include("src/**/*.kt", "**/*.kts")
                exclude("src/**test/**/*.kt", "src/commonTest/**/*.kt")  // path matching this pattern will not be checked by diktat
            }
        }
    }
    fixDiktatTasks()
}

private fun Project.fixDiktatTasks() {
    tasks.withType<DiktatJavaExecTaskBase>().configureEach {
        // https://github.com/analysis-dev/diktat/issues/1269
        systemProperty("user.home", rootDir.toString())
    }
}
