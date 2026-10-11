package org.openpmca.sample;

import org.openpmca.framework.Availability;
import org.openpmca.framework.Probe;
import org.openpmca.framework.RuntimeSnapshot;

public final class DiagnosticsFormatter {

    private DiagnosticsFormatter() {
    }

    public static String format(RuntimeSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("Snapshot must not be null");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== Open-PMCA Diagnostics ===\n");
        sb.append("Runtime Kind: ").append(snapshot.getRuntimeKind()).append("\n");
        sb.append("Hardware verification: NOT RUN\n\n");

        sb.append("--- Provider Probes ---\n");
        sb.append(formatProbe("Scalar Provider", snapshot.getScalarProvider())).append("\n");
        sb.append(formatProbe("Scalar String Reader", snapshot.getScalarStringReader())).append("\n");
        sb.append(formatProbe("Scalar Firmware Reader", snapshot.getScalarFirmwareReader())).append("\n\n");

        sb.append("--- Device & Platform Properties ---\n");
        sb.append(formatProbe("Model", snapshot.getModel())).append("\n");
        sb.append(formatProbe("Camera Firmware", snapshot.getCameraFirmware())).append("\n");
        sb.append(formatProbe("Sony Platform Version", snapshot.getSonyPlatformVersion())).append("\n");
        sb.append(formatProbe("Android API Level", snapshot.getAndroidApiLevel())).append("\n");

        return sb.toString();
    }

    static String formatProbe(String label, Probe<?> probe) {
        if (probe == null) {
            return label + ": Unknown [UNAVAILABLE] (NULL_PROBE)";
        }
        return formatProbe(label, probe.getAvailability(), probe.getValue(), probe.getReason());
    }

    static String formatProbe(String label, Availability availability, Object value, String reason) {
        StringBuilder sb = new StringBuilder();
        sb.append(label).append(": ");
        if (value != null) {
            sb.append(value);
        } else {
            sb.append("Unknown");
        }

        sb.append(" [").append(availability != null ? availability : Availability.UNKNOWN).append("]");

        if (reason != null && reason.length() > 0) {
            sb.append(" (").append(reason).append(")");
        }

        return sb.toString();
    }
}
