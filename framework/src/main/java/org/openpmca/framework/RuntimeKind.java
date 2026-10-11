package org.openpmca.framework;

/**
 * Observed runtime host classification.
 */
public enum RuntimeKind {
    /**
     * Sony Scalar runtime classes were discovered on the runtime classpath.
     * Note: Finding Scalar classes does not by itself guarantee official PMCA device support.
     */
    SONY_SCALAR,

    /**
     * Standard Android runtime classes were detected, but no Sony Scalar provider was found.
     */
    GENERIC_ANDROID,

    /**
     * Standard Java host JVM environment (desktop or server) with neither Scalar nor Android platform classes.
     */
    PLAIN_HOST,

    /**
     * An unexpected fault or fatal linkage issue occurred during runtime inspection.
     */
    DETECTION_FAILURE
}
