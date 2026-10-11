package com.sony.scalar.sysutil;

public class ScalarProperties {
    public static String getString(String key) {
        if ("model.name".equals(key)) return "   "; // whitespace/empty
        if ("version.platform".equals(key)) return null; // null
        return null;
    }

    public static String getFirmwareVersion() {
        return "";
    }
}
