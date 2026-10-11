package com.sony.scalar.sysutil;

public class ScalarProperties {
    public static Object getString(String key) {
        return Integer.valueOf(999); // wrong type, expected String!
    }

    public static Object getFirmwareVersion() {
        return Boolean.TRUE; // wrong type, expected String!
    }
}
