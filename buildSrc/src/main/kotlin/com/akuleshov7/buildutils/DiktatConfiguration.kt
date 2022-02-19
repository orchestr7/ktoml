/**
 * Configuration for diktat static analysis
 */

package com.akuleshov7.buildutils

import org.cqfn.diktat.plugin.gradle.DiktatExtension
import org.cqfn.diktat.plugin.gradle.DiktatGradlePlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

/**
 * Applies diktat gradle plugin and configures diktat for [this] project
 */
fun Project.configureDiktat() {
    apply<DiktatGradlePlugin>()
    configure<DiktatExtension> {
        diktatConfigFile = rootProject.file("diktat-analysis.yml")
        githubActions = findProperty("diktat.githubActions")?.toString()?.toBoolean() ?: false
        inputs {
            include("src/**/*.kt", "*.kts", "src/**/*.kts")
            exclude("$projectDir/build/**", "src/commonTest/**/*.kt")
        }
    }
}

/**
 * Creates unified tasks to run diktat on all projects
 */
fun Project.createDiktatTask() {
    if (this == rootProject) {
        // apply diktat to buildSrc
        apply<DiktatGradlePlugin>()
        configure<DiktatExtension> {
            diktatConfigFile = rootProject.file("diktat-analysis.yml")
            githubActions = findProperty("diktat.githubActions")?.toString()?.toBoolean() ?: false
            inputs {
                include(
                    "$rootDir/buildSrc/src/**/*.kt",
                    "$rootDir/buildSrc/src/**/*.kts",
                    "$rootDir/*.kts",
                    "$rootDir/buildSrc/*.kts"
                )
                exclude("$rootDir/build", "$rootDir/buildSrc/build")
            }
        }
    }
    tasks.register("diktatCheckAll") {
        allprojects {
            this@register.dependsOn(tasks.getByName("diktatCheck"))
        }
    }
    tasks.register("diktatFixAll") {
        allprojects {
            this@register.dependsOn(tasks.getByName("diktatFix"))
        }
    }
}
