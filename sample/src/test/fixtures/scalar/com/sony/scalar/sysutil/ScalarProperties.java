package com.sony.scalar.sysutil;

public final class ScalarProperties {
    public static boolean noValues;

    public static String getString(String key) {
        if ("model.name".equals(key)) {
            return noValues ? "" : "Fixture Sony Model";
        }
        if ("version.platform".equals(key)) {
            return noValues ? " " : "2.0";
        }
        throw new AssertionError("Unexpected fixture property: " + key);
    }

    public static String getFirmwareVersion() {
        return noValues ? null : "3.20";
    }
}
