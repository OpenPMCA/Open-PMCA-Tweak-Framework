package org.openpmca.sample;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import org.openpmca.framework.Availability;
import org.openpmca.framework.PmcaRuntime;
import org.openpmca.framework.Probe;
import org.openpmca.framework.RuntimeInspectionTestAccess;

public final class DiagnosticsFormatterHostTest {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new AssertionError("Expected the isolated fixture directory");
        }
        File fixtures = new File(args[0]);
        System.out.println("Running DiagnosticsFormatterHostTest...");

        testFormatterHelperAvailabilityStates();
        testPlainHostSnapshot();
        testSonyScalarSnapshot(fixtures);
        testGenericAndroidSnapshot(fixtures);
        testDetectionFailureSnapshot();
        testMissingValues(fixtures);
        testNullProbeHandling();
        testNullSnapshotHandling();

        System.out.println("ALL DIAGNOSTICS FORMATTER HOST TESTS PASSED.");
    }

    private static void testFormatterHelperAvailabilityStates() {
        System.out.println("Test: Formatter probe helper across availability states");

        assertEquals("Model: ILCE-6000 [AVAILABLE] (VALUE_READ)",
            DiagnosticsFormatter.formatProbe("Model", Availability.AVAILABLE, "ILCE-6000", "VALUE_READ"));
        assertEquals("Scalar Provider: true [AVAILABLE] (CLASS_PRESENT)",
            DiagnosticsFormatter.formatProbe("Scalar Provider", Availability.AVAILABLE, Boolean.TRUE, "CLASS_PRESENT"));
        assertEquals("Android API Level: 16 [AVAILABLE] (VALUE_READ)",
            DiagnosticsFormatter.formatProbe("Android API Level", Availability.AVAILABLE, Integer.valueOf(16), "VALUE_READ"));
        assertEquals("Camera Firmware: Unknown [UNAVAILABLE] (CLASS_ABSENT)",
            DiagnosticsFormatter.formatProbe("Camera Firmware", Availability.UNAVAILABLE, null, "CLASS_ABSENT"));
        assertEquals("Sony Platform Version: Unknown [UNKNOWN] (READ_FAILED)",
            DiagnosticsFormatter.formatProbe("Sony Platform Version", Availability.UNKNOWN, null, "READ_FAILED"));
        assertEquals("Model: Unknown [UNKNOWN] (NO_VALUE)",
            DiagnosticsFormatter.formatProbe("Model", Availability.UNKNOWN, null, "NO_VALUE"));
    }

    private static void testPlainHostSnapshot() {
        System.out.println("Test: Default inspect() on plain host");
        String text = DiagnosticsFormatter.format(PmcaRuntime.inspect());

        assertContains(text, "Runtime Kind: PLAIN_HOST\n");
        assertContains(text, "Scalar Provider: Unknown [UNAVAILABLE] (CLASS_ABSENT)\n");
        assertContains(text, "Model: Unknown [UNAVAILABLE] (NOT_APPLICABLE)\n");
        assertContains(text, "Camera Firmware: Unknown [UNAVAILABLE] (NOT_APPLICABLE)\n");
        assertContains(text, "Sony Platform Version: Unknown [UNAVAILABLE] (NOT_APPLICABLE)\n");
        assertContains(text, "Android API Level: Unknown [UNAVAILABLE] (NOT_APPLICABLE)\n");
        assertUnverified(text);
    }

    private static void testSonyScalarSnapshot(File fixtures) throws Exception {
        System.out.println("Test: Sony Scalar fixture renders all snapshot getters without hardware claims");
        String text = DiagnosticsFormatter.format(RuntimeInspectionTestAccess.inspect(
            fixtureLoader(fixtures, true)));

        assertContains(text, "Runtime Kind: SONY_SCALAR\n");
        assertContains(text, "Scalar Provider: true [AVAILABLE] (CLASS_PRESENT)\n");
        assertContains(text, "Scalar String Reader: true [AVAILABLE] (METHOD_PRESENT)\n");
        assertContains(text, "Scalar Firmware Reader: true [AVAILABLE] (METHOD_PRESENT)\n");
        assertContains(text, "Model: Fixture Sony Model [AVAILABLE] (VALUE_READ)\n");
        assertContains(text, "Camera Firmware: 3.20 [AVAILABLE] (VALUE_READ)\n");
        assertContains(text, "Sony Platform Version: 2.0 [AVAILABLE] (VALUE_READ)\n");
        assertContains(text, "Android API Level: 16 [AVAILABLE] (VALUE_READ)\n");
        assertUnverified(text);
    }

    private static void testGenericAndroidSnapshot(File fixtures) throws Exception {
        System.out.println("Test: Generic Android fixture never becomes Sony camera firmware");
        String text = DiagnosticsFormatter.format(RuntimeInspectionTestAccess.inspect(
            fixtureLoader(fixtures, false)));

        assertContains(text, "Runtime Kind: GENERIC_ANDROID\n");
        assertContains(text, "Scalar Provider: Unknown [UNAVAILABLE] (CLASS_ABSENT)\n");
        assertContains(text, "Model: Generic Android Fixture [AVAILABLE] (VALUE_READ)\n");
        assertContains(text, "Android API Level: 16 [AVAILABLE] (VALUE_READ)\n");
        assertContains(text, "Camera Firmware: Unknown [UNAVAILABLE] (NOT_APPLICABLE)\n");
        assertContains(text, "Sony Platform Version: Unknown [UNAVAILABLE] (NOT_APPLICABLE)\n");
        assertNotContains(text, "TEST_OS_RELEASE_NOT_CAMERA_FIRMWARE");
        assertUnverified(text);
    }

    private static void testDetectionFailureSnapshot() {
        System.out.println("Test: Blocked provider detection renders UNKNOWN without leaking its exception");
        ClassLoader blocked = new ClassLoader(DiagnosticsFormatterHostTest.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if ("com.sony.scalar.sysutil.ScalarProperties".equals(name)) {
                    throw new SecurityException("PRIVATE_FIXTURE_EXCEPTION");
                }
                return super.loadClass(name, resolve);
            }
        };
        String text = DiagnosticsFormatter.format(RuntimeInspectionTestAccess.inspect(blocked));

        assertContains(text, "Runtime Kind: DETECTION_FAILURE\n");
        assertContains(text, "Scalar Provider: Unknown [UNKNOWN] (READ_FAILED)\n");
        assertNotContains(text, "PRIVATE_FIXTURE_EXCEPTION");
        assertUnverified(text);
    }

    private static void testMissingValues(File fixtures) throws Exception {
        System.out.println("Test: Empty, whitespace, and null field values render Unknown with NO_VALUE");
        ClassLoader loader = fixtureLoader(fixtures, true);
        loader.loadClass("com.sony.scalar.sysutil.ScalarProperties")
            .getField("noValues").setBoolean(null, true);
        String text = DiagnosticsFormatter.format(RuntimeInspectionTestAccess.inspect(loader));

        assertContains(text, "Runtime Kind: SONY_SCALAR\n");
        assertContains(text, "Scalar String Reader: true [AVAILABLE] (METHOD_PRESENT)\n");
        assertContains(text, "Scalar Firmware Reader: true [AVAILABLE] (METHOD_PRESENT)\n");
        assertContains(text, "Model: Unknown [UNKNOWN] (NO_VALUE)\n");
        assertContains(text, "Camera Firmware: Unknown [UNKNOWN] (NO_VALUE)\n");
        assertContains(text, "Sony Platform Version: Unknown [UNKNOWN] (NO_VALUE)\n");
        assertContains(text, "Android API Level: 16 [AVAILABLE] (VALUE_READ)\n");
        assertUnverified(text);
    }

    private static ClassLoader fixtureLoader(File fixtures, boolean scalar) throws Exception {
        URL android = new File(fixtures, "android").toURI().toURL();
        URL[] urls = scalar
            ? new URL[] { new File(fixtures, "scalar").toURI().toURL(), android }
            : new URL[] { android };
        return new URLClassLoader(urls, DiagnosticsFormatterHostTest.class.getClassLoader());
    }

    private static void testNullProbeHandling() {
        System.out.println("Test: Null probe handling");
        assertEquals("Null Probe: Unknown [UNAVAILABLE] (NULL_PROBE)",
            DiagnosticsFormatter.formatProbe("Null Probe", (Probe<?>) null));
    }

    private static void testNullSnapshotHandling() {
        System.out.println("Test: Null snapshot handling");
        try {
            DiagnosticsFormatter.format(null);
            throw new AssertionError("Expected IllegalArgumentException for null snapshot");
        } catch (IllegalArgumentException expected) {
            // Success
        }
    }

    private static void assertUnverified(String text) {
        assertContains(text, "Hardware verification: NOT RUN\n");
        assertNotContains(text, "Hardware verified");
        assertNotContains(text, "HARDWARE_VERIFIED");
        assertNotContains(text, "serial");
        assertNotContains(text, "Serial");
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected: " + expected + "\nActual: " + actual);
        }
    }

    private static void assertContains(String text, String expected) {
        if (text.indexOf(expected) < 0) {
            throw new AssertionError("Missing: " + expected + "\nOutput:\n" + text);
        }
    }

    private static void assertNotContains(String text, String unexpected) {
        if (text.indexOf(unexpected) >= 0) {
            throw new AssertionError("Unexpected: " + unexpected + "\nOutput:\n" + text);
        }
    }
}
