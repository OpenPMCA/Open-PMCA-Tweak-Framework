package com.sony.scalar.sysutil;

public class ScalarProperties {
    public static String getString(String key) {
        if ("model.name".equals(key)) return ""; // empty scalar model
        if ("version.platform".equals(key)) return "2.00";
        return null;
    }

    public static String getFirmwareVersion() {
        return "3.10";
    }
}
