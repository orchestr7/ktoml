/**
 * Opt-in marker for ktoml declarations that are public for technical reasons but not part of the API.
 */

package com.akuleshov7.ktoml.annotations

/**
 * Marks a ktoml declaration as **internal to the library**: it is `public` only for technical reasons
 * (e.g. it is reachable from other modules, or it predates this distinction and cannot change
 * visibility without breaking binary compatibility), but it is **not** intended for use outside ktoml
 * and may change or break at any time, without notice.
 *
 * Unlike flipping a declaration to `internal`, this marker keeps the symbol binary-compatible while
 * still discouraging use: opting in with `@OptIn(InternalKtomlApi::class)` is required to reference it.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is an internal ktoml API that is not intended for use outside the library. " +
            "It may change or break at any time. Opt in with @OptIn(InternalKtomlApi::class) if you " +
            "really need it.",
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS,
    AnnotationTarget.CONSTRUCTOR,
)
public annotation class InternalKtomlApi
