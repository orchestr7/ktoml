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
        inputs = files(
            "src/**/*.kt"
        )
        debug = true
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
            inputs = files(
                "$rootDir/buildSrc/src/**/*.kt"
            )
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
