package org.openpmca.framework;

/** Test-only bridge; never include this class in the SDK JAR or application DEX. */
public final class RuntimeInspectionTestAccess {

    private RuntimeInspectionTestAccess() {
    }

    public static RuntimeSnapshot inspect(ClassLoader loader) {
        return PmcaRuntime.inspect(loader);
    }
}
