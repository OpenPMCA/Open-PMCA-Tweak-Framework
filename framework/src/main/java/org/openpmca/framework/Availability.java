package org.openpmca.framework;

/**
 * Tri-state availability status for runtime capabilities, providers, and individual data probes.
 */
public enum Availability {
    /**
     * The feature, provider, or reader method is present and operating normally.
     */
    AVAILABLE,

    /**
     * The symbol, class, or method is known to be absent on the host platform.
     */
    UNAVAILABLE,

    /**
     * The capability or provider exists, but attempting to read data failed, was blocked, or produced no value.
     */
    UNKNOWN
}
