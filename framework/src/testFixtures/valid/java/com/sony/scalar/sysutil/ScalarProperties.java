package com.sony.scalar.sysutil;

public class ScalarProperties {
    public static int serialCallCount = 0;

    public static String getString(String key) {
        if ("model.name".equals(key)) return "ILCE-7RM2";
        if ("version.platform".equals(key)) return "2.00";
        if (key != null && key.toLowerCase().contains("serial")) {
            serialCallCount++;
        }
        return null;
    }

    public static String getFirmwareVersion() {
        return "3.10";
    }
}
