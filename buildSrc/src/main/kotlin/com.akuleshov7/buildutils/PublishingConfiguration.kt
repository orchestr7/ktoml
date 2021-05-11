package com.akuleshov7.buildutils

import org.gradle.api.Project
import io.github.gradlenexus.publishplugin.NexusPublishPlugin
import io.github.gradlenexus.publishplugin.NexusPublishExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin

fun Project.configurePublishing() {
    // If present, set properties from env variables. If any are absent, release will fail.
    System.getenv("OSSRH_USERNAME")?.let {
        extra.set("sonatypeUsername", it)
    }
    System.getenv("OSSRH_PASSWORD")?.let {
        extra.set("sonatypePassword", it)
    }
    System.getenv("PGP_SEC")?.let {
        extra.set("signingKey", it)
    }
    System.getenv("PGP_PASSWORD")?.let {
        extra.set("signingPassword", it)
    }

    if (this == rootProject) {
        apply<NexusPublishPlugin>()
        if (hasProperty("sonatypeUsername")) {
            configureNexusPublishing()
        }
    }

    apply<MavenPublishPlugin>()
    apply<SigningPlugin>()

    configurePublications()

    if (hasProperty("signingKey")) {
        configureSigning()
    }

    // https://kotlinlang.org/docs/mpp-publish-lib.html#avoid-duplicate-publications
    // `configureNexusPublishing` adds sonatype publication tasks inside `afterEvaluate`.
    rootProject.afterEvaluate {
        val publicationsFromMainHost = listOf("jvm", "js", "linuxX64", "kotlinMultiplatform", "metadata")
        configure<PublishingExtension> {
            publications.matching { it.name in publicationsFromMainHost }.all {
                val targetPublication = this@all
                tasks.withType<AbstractPublishToMaven>()
                    .matching { it.publication == targetPublication }
                    .configureEach {
                        onlyIf {
                            // main publishing CI job is executed on Linux host
                            DefaultNativePlatform.getCurrentOperatingSystem().isLinux.apply {
                                if (!this) {
                                    logger.lifecycle("Publication ${(it as AbstractPublishToMaven).publication.name} is skipped on current host")
                                }
                            }
                        }
                    }
                }
        }
    }
}

private fun Project.configurePublications() {
    val dokkaJar = tasks.create<Jar>("dokkaJar") {
        group = "documentation"
        archiveClassifier.set("javadoc")
        from(tasks.findByName("dokkaHtml"))
    }
    configure<PublishingExtension> {
        repositories {
            mavenLocal()
        }
        publications.withType<MavenPublication>().forEach { publication ->
            publication.artifact(dokkaJar)
            publication.pom {
                name.set(project.name)
                description.set(project.description ?: project.name)
                url.set("https://github.com/akuleshov7/ktoml")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/akuleshov7/ktoml/blob/main/LICENSE")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("akuleshov7")
                        name.set("Andrey Kuleshov")
                        email.set("andrewkuleshov7@gmail.com")
                    }
                }
                scm {
                    url.set("https://github.com/akuleshov7/ktoml")
                    connection.set("scm:git:git://github.com/akuleshov7/ktoml.git")
                }
            }
        }
    }
}

private fun Project.configureSigning() {
    configure<SigningExtension> {
        useInMemoryPgpKeys(property("signingKey") as String?, property("signingPassword") as String?)
        logger.lifecycle("The following publications are getting signed: ${extensions.getByType<PublishingExtension>().publications.map { it.name }}")
        sign(*extensions.getByType<PublishingExtension>().publications.toTypedArray())
    }
}

private fun Project.configureNexusPublishing() {
    configure<NexusPublishExtension> {
        repositories {
            sonatype {  //only for users registered in Sonatype after 24 Feb 2021
                nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
                snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
                username.set(property("sonatypeUsername") as String)
                password.set(property("sonatypePassword") as String)
            }
        }
    }
}
