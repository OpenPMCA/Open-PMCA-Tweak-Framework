package com.sony.scalar.sysutil;

public class ScalarProperties {
    public static String getString(String key) {
        throw new NoClassDefFoundError("simulated missing native scalar library");
    }

    public static String getFirmwareVersion() {
        return "1.00";
    }
}
