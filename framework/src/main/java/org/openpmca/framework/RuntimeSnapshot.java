package org.openpmca.framework;

/**
 * An immutable point-in-time snapshot of observed PMCA runtime environment information.
 */
public final class RuntimeSnapshot {
    private final RuntimeKind runtimeKind;
    private final Probe<Boolean> scalarProvider;
    private final Probe<Boolean> scalarStringReader;
    private final Probe<Boolean> scalarFirmwareReader;
    private final Probe<String> model;
    private final Probe<String> cameraFirmware;
    private final Probe<String> sonyPlatformVersion;
    private final Probe<Integer> androidApiLevel;

    RuntimeSnapshot(
        RuntimeKind runtimeKind,
        Probe<Boolean> scalarProvider,
        Probe<Boolean> scalarStringReader,
        Probe<Boolean> scalarFirmwareReader,
        Probe<String> model,
        Probe<String> cameraFirmware,
        Probe<String> sonyPlatformVersion,
        Probe<Integer> androidApiLevel
    ) {
        if (runtimeKind == null) {
            throw new IllegalArgumentException("runtimeKind must not be null");
        }
        this.runtimeKind = runtimeKind;
        this.scalarProvider = (scalarProvider != null) ? scalarProvider : Probe.<Boolean>unavailable("UNKNOWN");
        this.scalarStringReader = (scalarStringReader != null) ? scalarStringReader : Probe.<Boolean>unavailable("UNKNOWN");
        this.scalarFirmwareReader = (scalarFirmwareReader != null) ? scalarFirmwareReader : Probe.<Boolean>unavailable("UNKNOWN");
        this.model = (model != null) ? model : Probe.<String>unavailable("UNKNOWN");
        this.cameraFirmware = (cameraFirmware != null) ? cameraFirmware : Probe.<String>unavailable("UNKNOWN");
        this.sonyPlatformVersion = (sonyPlatformVersion != null) ? sonyPlatformVersion : Probe.<String>unavailable("UNKNOWN");
        this.androidApiLevel = (androidApiLevel != null) ? androidApiLevel : Probe.<Integer>unavailable("UNKNOWN");
    }

    public RuntimeKind getRuntimeKind() {
        return runtimeKind;
    }

    public Probe<Boolean> getScalarProvider() {
        return scalarProvider;
    }

    public Probe<Boolean> getScalarStringReader() {
        return scalarStringReader;
    }

    public Probe<Boolean> getScalarFirmwareReader() {
        return scalarFirmwareReader;
    }

    public Probe<String> getModel() {
        return model;
    }

    public Probe<String> getCameraFirmware() {
        return cameraFirmware;
    }

    public Probe<String> getSonyPlatformVersion() {
        return sonyPlatformVersion;
    }

    public Probe<Integer> getAndroidApiLevel() {
        return androidApiLevel;
    }

    @Override
    public String toString() {
        return "RuntimeSnapshot{" +
            "kind=" + runtimeKind +
            ", scalarProvider=" + scalarProvider +
            ", scalarStringReader=" + scalarStringReader +
            ", scalarFirmwareReader=" + scalarFirmwareReader +
            ", model=" + model +
            ", cameraFirmware=" + cameraFirmware +
            ", sonyPlatformVersion=" + sonyPlatformVersion +
            ", androidApiLevel=" + androidApiLevel +
            '}';
    }
}
