/**
 * Version configuration file.
 */

package com.akuleshov7.buildutils

import org.ajoberstar.grgit.Grgit
import org.ajoberstar.reckon.gradle.ReckonExtension
import org.ajoberstar.reckon.gradle.ReckonPlugin
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

@Suppress("MISSING_KDOC_ON_FUNCTION", "MISSING_KDOC_TOP_LEVEL")
/**
 * Configures how project version is determined.
 *
 * @throws GradleException if there was an attempt to run release build with dirty working tree
 */
fun Project.configureVersioning() {
    apply<ReckonPlugin>()

    val isSnapshot = hasProperty("reckon.stage") && property("reckon.stage") == "snapshot"
    configure<ReckonExtension> {
        setDefaultInferredScope("patch")
        if (isSnapshot) {
            snapshots()
        } else {
            stages("alpha", "rc", "final")
        }
        setScopeCalc(calcScopeFromProp().or(calcScopeFromCommitMessages()))
        setStageCalc(calcStageFromProp())
    }

    // to activate release, provide `-Prelease` or `-Prelease=true`. To deactivate, either omit the property, or set `-Prelease=false`.
    val isRelease = hasProperty("release") && (property("release") as String != "false")
    if (isRelease) {
        val grgit = project.findProperty("grgit") as Grgit  // grgit property is added by reckon plugin
        val status = grgit.repository.jgit.status().call()
        if (!status.isClean) {
            throw GradleException(
                "Release build will be performed with not clean git tree; aborting. " +
                        "Untracked files: ${status.untracked}, uncommitted changes: ${status.uncommittedChanges}"
            )
        }
    }
}
