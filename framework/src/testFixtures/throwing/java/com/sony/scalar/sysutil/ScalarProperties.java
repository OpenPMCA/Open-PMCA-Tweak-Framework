package com.sony.scalar.sysutil;

public class ScalarProperties {
    public static String getString(String key) {
        if ("model.name".equals(key)) return "ILCE-6300";
        if ("version.platform".equals(key)) return "2.10";
        return null;
    }

    public static String getFirmwareVersion() {
        throw new RuntimeException("Simulated native UnsatisfiedLinkError");
    }
}
