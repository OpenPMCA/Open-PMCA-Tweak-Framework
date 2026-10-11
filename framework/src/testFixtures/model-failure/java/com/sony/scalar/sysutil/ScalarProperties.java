package com.sony.scalar.sysutil;

public final class ScalarProperties {
    public static int serialCallCount = 0;

    public static String getString(String key) {
        if ("model.name".equals(key)) {
            throw new RuntimeException("Simulated native read failure for model");
        }
        if ("version.platform".equals(key)) {
            return "2.00";
        }
        return null;
    }

    public static String getFirmwareVersion() {
        return "3.10";
    }
}
