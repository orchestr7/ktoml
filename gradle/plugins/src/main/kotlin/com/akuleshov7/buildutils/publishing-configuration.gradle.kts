package com.akuleshov7.buildutils

import io.github.gradlenexus.publishplugin.NexusPublishPlugin
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.extra

plugins {
    `maven-publish`
    signing
}

run {
    // If present, set properties from env variables. If any are absent, release will fail.
    System.getenv("SONATYPE_USER")?.let {
        extra.set("sonatypeUsername", it)
    }
    System.getenv("SONATYPE_PASSWORD")?.let {
        extra.set("sonatypePassword", it)
    }
    System.getenv("PGP_SEC")?.let {
        extra.set("signingKey", it)
    }
    System.getenv("PGP_PASSWORD")?.let {
        extra.set("signingPassword", it)
    }

    if (project.path == rootProject.path) {
        apply<NexusPublishPlugin>()
        if (hasProperty("sonatypeUsername")) {
            configureNexusPublishing()
        }
    }
}

run {
    configurePublications()
}
