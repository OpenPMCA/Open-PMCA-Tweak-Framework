package com.sony.scalar.sysutil;

public final class ScalarProperties {
    public static String getString(String key) {
        if ("model.name".equals(key)) {
            return "ILCE-7";
        }
        if ("version.platform".equals(key)) {
            return "2.00";
        }
        return null;
    }

    public String getFirmwareVersion() {
        return "3.10";
    }
}
