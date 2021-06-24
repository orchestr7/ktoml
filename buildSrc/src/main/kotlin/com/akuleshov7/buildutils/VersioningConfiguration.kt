/**
 * Version configuration file.
 */

package com.akuleshov7.buildutils

import org.ajoberstar.reckon.gradle.ReckonExtension
import org.ajoberstar.reckon.gradle.ReckonPlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

@Suppress("MISSING_KDOC_ON_FUNCTION", "MISSING_KDOC_TOP_LEVEL")
fun Project.configureVersioning() {
    apply<ReckonPlugin>()

    configure<ReckonExtension> {
        scopeFromProp()
        stageFromProp("alpha", "rc", "final")  // version string will be based on last commit; when checking out a tag, that tag will be used
    }
}
