/**
 * Opt-in marker for ktoml APIs that are not yet stabilized.
 */

package com.akuleshov7.ktoml.annotations

/**
 * Marks a ktoml declaration as **experimental**: while ktoml is pre-1.0 — and especially for APIs that
 * are specific to this fork and not yet adopted upstream — its signature or behavior may change, or it
 * may be removed, in a future release without notice.
 *
 * To use such an API, opt in explicitly with `@OptIn(ExperimentalKtomlApi::class)` on the using
 * declaration, or propagate the marker by annotating your own declaration with `@ExperimentalKtomlApi`.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This ktoml API is experimental: it may change or be removed in a future release. " +
            "Opt in with @OptIn(ExperimentalKtomlApi::class) to acknowledge this.",
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS,
    AnnotationTarget.CONSTRUCTOR,
)
public annotation class ExperimentalKtomlApi
