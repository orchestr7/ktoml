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
        scopeFromProp()
        if (isSnapshot) {
            // we should build snapshots only for snapshot publishing, so it requires explicit parameter
            snapshotFromProp()
        } else {
            stageFromProp("alpha", "rc", "final")
        }
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
    if (isSnapshot) {
        val grgit = project.findProperty("grgit") as Grgit  // grgit property is added by reckon plugin
        // A terrible hack to remove all pre-release tags. Because in semver `0.1.0-SNAPSHOT` < `0.1.0-alpha`, in snapshot mode
        // we remove tags like `0.1.0-alpha`, and then reckoned version will still be `0.1.0-SNAPSHOT` and it will be compliant.
        val preReleaseTagNames = grgit.tag.list()
            .sortedByDescending { it.commit.dateTime }
            .takeWhile {
                // take latest tags that are pre-release
                !it.name.matches(Regex("""^v\d+\.\d+\.\d+$"""))
            }
            .map { it.name }
        grgit.tag.remove { this.names = preReleaseTagNames }
    }
}
