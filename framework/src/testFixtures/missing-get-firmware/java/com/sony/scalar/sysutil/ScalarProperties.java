package com.sony.scalar.sysutil;

public final class ScalarProperties {
    public static int serialCallCount = 0;

    public static String getString(String key) {
        if ("model.name".equals(key)) {
            return "ILCE-7RM2";
        }
        if ("version.platform".equals(key)) {
            return "2.00";
        }
        return null;
    }
}
