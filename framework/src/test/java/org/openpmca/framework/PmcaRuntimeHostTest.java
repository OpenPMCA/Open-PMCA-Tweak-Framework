package org.openpmca.framework;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class PmcaRuntimeHostTest {

    public static void main(String[] args) {
        System.out.println("Starting PmcaRuntimeHostTest suite (17 scenarios)...");

        testPlainHost();
        testValidSonyScalar();
        testNoMethodsOnScalarProperties();
        testThrowingGetterInScalar();
        testEmptyValuesOnScalar();
        testGenericAndroidFallback();
        testNoSerialPathVerification();
        testCoexistScalarEmptyNeverFallbacksToBuildModel();
        testWrongReflectedMetadataTypes();
        testStaticInitOrLinkageFailureInGetter();
        testApiVisibility();
        testIndividuallyMissingReaders();
        testReaderIndependence();
        testGenericAndroidMissingSdkField();
        testBlockedProviderLookup();
        testMethodLookupLinkageError();
        testInstanceMembersNonStaticDegradation();

        System.out.println("All 17 PmcaRuntimeHostTest scenarios PASSED successfully.");
    }

    private static ClassLoader createFixtureLoader(String fixtureName) {
        String baseDir = System.getProperty("openpmca.test.fixtures.baseDir");
        if (baseDir == null) {
            throw new AssertionError("System property openpmca.test.fixtures.baseDir is not set");
        }
        File fixtureDir = new File(baseDir, "fixtures_" + fixtureName);
        if (!fixtureDir.exists()) {
            throw new AssertionError("Fixture directory does not exist: " + fixtureDir.getAbsolutePath());
        }
        try {
            return new URLClassLoader(
                new URL[] { fixtureDir.toURI().toURL() },
                PmcaRuntimeHostTest.class.getClassLoader()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to create fixture classloader for " + fixtureName, e);
        }
    }

    private static void testPlainHost() {
        System.out.println("-> Test 1: Plain Host JVM (default inspect())");
        RuntimeSnapshot snapshot = PmcaRuntime.inspect();

        assertNotNull(snapshot, "Snapshot must not be null");
        assertEquals(RuntimeKind.PLAIN_HOST, snapshot.getRuntimeKind(), "Kind must be PLAIN_HOST");

        assertEquals(Availability.UNAVAILABLE, snapshot.getScalarProvider().getAvailability(), "ScalarProvider availability");
        assertEquals("CLASS_ABSENT", snapshot.getScalarProvider().getReason(), "ScalarProvider reason");

        assertEquals(Availability.UNAVAILABLE, snapshot.getScalarStringReader().getAvailability(), "ScalarStringReader availability");
        assertEquals("CLASS_ABSENT", snapshot.getScalarStringReader().getReason(), "ScalarStringReader reason");

        assertEquals(Availability.UNAVAILABLE, snapshot.getScalarFirmwareReader().getAvailability(), "ScalarFirmwareReader availability");
        assertEquals("CLASS_ABSENT", snapshot.getScalarFirmwareReader().getReason(), "ScalarFirmwareReader reason");

        assertEquals(Availability.UNAVAILABLE, snapshot.getModel().getAvailability(), "Model availability");
        assertEquals("NOT_APPLICABLE", snapshot.getModel().getReason(), "Model reason");
        assertNull(snapshot.getModel().getValue(), "Model value");

        assertEquals(Availability.UNAVAILABLE, snapshot.getCameraFirmware().getAvailability(), "CameraFirmware availability");
        assertEquals("NOT_APPLICABLE", snapshot.getCameraFirmware().getReason(), "CameraFirmware reason");
        assertNull(snapshot.getCameraFirmware().getValue(), "CameraFirmware value");

        assertEquals(Availability.UNAVAILABLE, snapshot.getSonyPlatformVersion().getAvailability(), "Platform availability");
        assertEquals("NOT_APPLICABLE", snapshot.getSonyPlatformVersion().getReason(), "Platform reason");
        assertNull(snapshot.getSonyPlatformVersion().getValue(), "Platform value");

        assertEquals(Availability.UNAVAILABLE, snapshot.getAndroidApiLevel().getAvailability(), "AndroidApiLevel availability");
        assertEquals("NOT_APPLICABLE", snapshot.getAndroidApiLevel().getReason(), "AndroidApiLevel reason");
        assertNull(snapshot.getAndroidApiLevel().getValue(), "AndroidApiLevel value");
    }

    private static void testValidSonyScalar() {
        System.out.println("-> Test 2: Valid Sony Scalar Runtime");
        ClassLoader loader = createFixtureLoader("valid");
        RuntimeSnapshot snapshot = PmcaRuntime.inspect(loader);

        assertEquals(RuntimeKind.SONY_SCALAR, snapshot.getRuntimeKind(), "Kind must be SONY_SCALAR");

        assertEquals(Availability.AVAILABLE, snapshot.getScalarProvider().getAvailability(), "ScalarProvider availability");
        assertEquals("CLASS_PRESENT", snapshot.getScalarProvider().getReason(), "ScalarProvider reason");
        assertEquals(Boolean.TRUE, snapshot.getScalarProvider().getValue(), "ScalarProvider value");

        assertEquals(Availability.AVAILABLE, snapshot.getScalarStringReader().getAvailability(), "StringReader availability");
        assertEquals("METHOD_PRESENT", snapshot.getScalarStringReader().getReason(), "StringReader reason");
        assertEquals(Boolean.TRUE, snapshot.getScalarStringReader().getValue(), "StringReader value");

        assertEquals(Availability.AVAILABLE, snapshot.getScalarFirmwareReader().getAvailability(), "FirmwareReader availability");
        assertEquals("METHOD_PRESENT", snapshot.getScalarFirmwareReader().getReason(), "FirmwareReader reason");
        assertEquals(Boolean.TRUE, snapshot.getScalarFirmwareReader().getValue(), "FirmwareReader value");

        assertEquals(Availability.AVAILABLE, snapshot.getModel().getAvailability(), "Model availability");
        assertEquals("VALUE_READ", snapshot.getModel().getReason(), "Model reason");
        assertEquals("ILCE-7RM2", snapshot.getModel().getValue(), "Model value");

        assertEquals(Availability.AVAILABLE, snapshot.getSonyPlatformVersion().getAvailability(), "Platform availability");
        assertEquals("VALUE_READ", snapshot.getSonyPlatformVersion().getReason(), "Platform reason");
        assertEquals("2.00", snapshot.getSonyPlatformVersion().getValue(), "Platform value");

        assertEquals(Availability.AVAILABLE, snapshot.getCameraFirmware().getAvailability(), "Firmware availability");
        assertEquals("VALUE_READ", snapshot.getCameraFirmware().getReason(), "Firmware reason");
        assertEquals("3.10", snapshot.getCameraFirmware().getValue(), "Firmware value");

        assertEquals(Availability.AVAILABLE, snapshot.getAndroidApiLevel().getAvailability(), "AndroidApiLevel availability");
        assertEquals("VALUE_READ", snapshot.getAndroidApiLevel().getReason(), "AndroidApiLevel reason");
        assertEquals(Integer.valueOf(10), snapshot.getAndroidApiLevel().getValue(), "AndroidApiLevel value");
    }

    private static void testNoMethodsOnScalarProperties() {
        System.out.println("-> Test 3: ScalarProperties Present but Readers Missing");
        ClassLoader loader = createFixtureLoader("no_methods");
        RuntimeSnapshot snapshot = PmcaRuntime.inspect(loader);

        assertEquals(RuntimeKind.SONY_SCALAR, snapshot.getRuntimeKind(), "Kind must be SONY_SCALAR");
        assertEquals(Availability.AVAILABLE, snapshot.getScalarProvider().getAvailability(), "Provider is present");
        assertEquals("CLASS_PRESENT", snapshot.getScalarProvider().getReason(), "Provider reason");

        assertEquals(Availability.UNAVAILABLE, snapshot.getScalarStringReader().getAvailability(), "StringReader availability");
        assertEquals("METHOD_ABSENT", snapshot.getScalarStringReader().getReason(), "StringReader reason");

        assertEquals(Availability.UNAVAILABLE, snapshot.getScalarFirmwareReader().getAvailability(), "FirmwareReader availability");
        assertEquals("METHOD_ABSENT", snapshot.getScalarFirmwareReader().getReason(), "FirmwareReader reason");

        assertEquals(Availability.UNAVAILABLE, snapshot.getModel().getAvailability(), "Model availability");
        assertEquals("METHOD_ABSENT", snapshot.getModel().getReason(), "Model reason");

        assertEquals(Availability.UNAVAILABLE, snapshot.getSonyPlatformVersion().getAvailability(), "Platform availability");
        assertEquals("METHOD_ABSENT", snapshot.getSonyPlatformVersion().getReason(), "Platform reason");

        assertEquals(Availability.UNAVAILABLE, snapshot.getCameraFirmware().getAvailability(), "Firmware availability");
        assertEquals("METHOD_ABSENT", snapshot.getCameraFirmware().getReason(), "Firmware reason");
    }

    private static void testThrowingGetterInScalar() {
        System.out.println("-> Test 4: Getter Exists but Throws Exception During Invoke");
        ClassLoader loader = createFixtureLoader("throwing");
        RuntimeSnapshot snapshot = PmcaRuntime.inspect(loader);

        assertEquals(RuntimeKind.SONY_SCALAR, snapshot.getRuntimeKind(), "Kind must be SONY_SCALAR");

        assertEquals(Availability.AVAILABLE, snapshot.getScalarFirmwareReader().getAvailability(), "FirmwareReader method probe");
        assertEquals("METHOD_PRESENT", snapshot.getScalarFirmwareReader().getReason(), "FirmwareReader reason");

        assertEquals(Availability.UNKNOWN, snapshot.getCameraFirmware().getAvailability(), "CameraFirmware availability");
        assertEquals("READ_FAILED", snapshot.getCameraFirmware().getReason(), "CameraFirmware reason");
        assertNull(snapshot.getCameraFirmware().getValue(), "CameraFirmware value must be null on failure");

        assertEquals(Availability.AVAILABLE, snapshot.getModel().getAvailability(), "Model availability");
        assertEquals("VALUE_READ", snapshot.getModel().getReason(), "Model reason");
        assertEquals("ILCE-6300", snapshot.getModel().getValue(), "Model value");

        assertEquals(Availability.AVAILABLE, snapshot.getSonyPlatformVersion().getAvailability(), "Platform availability");
        assertEquals("VALUE_READ", snapshot.getSonyPlatformVersion().getReason(), "Platform reason");
        assertEquals("2.10", snapshot.getSonyPlatformVersion().getValue(), "Platform value");
    }

    private static void testEmptyValuesOnScalar() {
        System.out.println("-> Test 5: Empty / Whitespace / Null Scalar Values");
        ClassLoader loader = createFixtureLoader("empty");
        RuntimeSnapshot snapshot = PmcaRuntime.inspect(loader);

        assertEquals(RuntimeKind.SONY_SCALAR, snapshot.getRuntimeKind(), "Kind must be SONY_SCALAR");

        assertEquals(Availability.UNKNOWN, snapshot.getModel().getAvailability(), "Model availability on whitespace");
        assertEquals("NO_VALUE", snapshot.getModel().getReason(), "Model reason on whitespace");
        assertNull(snapshot.getModel().getValue(), "Model value");

        assertEquals(Availability.UNKNOWN, snapshot.getSonyPlatformVersion().getAvailability(), "Platform availability on null");
        assertEquals("NO_VALUE", snapshot.getSonyPlatformVersion().getReason(), "Platform reason on null");
        assertNull(snapshot.getSonyPlatformVersion().getValue(), "Platform value");

        assertEquals(Availability.UNKNOWN, snapshot.getCameraFirmware().getAvailability(), "Firmware availability on empty");
        assertEquals("NO_VALUE", snapshot.getCameraFirmware().getReason(), "Firmware reason on empty");
        assertNull(snapshot.getCameraFirmware().getValue(), "Firmware value");
    }

    private static void testGenericAndroidFallback() {
        System.out.println("-> Test 6: Generic Android Fallback (Scalar absent, Build present)");
        ClassLoader loader = createFixtureLoader("generic_android");
        RuntimeSnapshot snapshot = PmcaRuntime.inspect(loader);

        assertEquals(RuntimeKind.GENERIC_ANDROID, snapshot.getRuntimeKind(), "Kind must be GENERIC_ANDROID");

        assertEquals(Availability.UNAVAILABLE, snapshot.getScalarProvider().getAvailability(), "ScalarProvider unavailable");
        assertEquals("CLASS_ABSENT", snapshot.getScalarProvider().getReason(), "ScalarProvider reason");

        assertEquals(Availability.AVAILABLE, snapshot.getModel().getAvailability(), "Model availability");
        assertEquals("VALUE_READ", snapshot.getModel().getReason(), "Model reason");
        assertEquals("Pixel 6", snapshot.getModel().getValue(), "Model value from Build.MODEL");

        assertEquals(Availability.AVAILABLE, snapshot.getAndroidApiLevel().getAvailability(), "AndroidApiLevel availability");
        assertEquals("VALUE_READ", snapshot.getAndroidApiLevel().getReason(), "AndroidApiLevel reason");
        assertEquals(Integer.valueOf(31), snapshot.getAndroidApiLevel().getValue(), "AndroidApiLevel value");

        assertEquals(Availability.UNAVAILABLE, snapshot.getCameraFirmware().getAvailability(), "Firmware availability");
        assertEquals("NOT_APPLICABLE", snapshot.getCameraFirmware().getReason(), "Firmware reason");
        assertNull(snapshot.getCameraFirmware().getValue(), "Firmware value must be null");

        assertEquals(Availability.UNAVAILABLE, snapshot.getSonyPlatformVersion().getAvailability(), "Platform availability");
        assertEquals("NOT_APPLICABLE", snapshot.getSonyPlatformVersion().getReason(), "Platform reason");
        assertNull(snapshot.getSonyPlatformVersion().getValue(), "Platform value must be null");
    }

    private static void testNoSerialPathVerification() {
        System.out.println("-> Test 7: Verify No Serial Number Path in SDK API or Probing");
        for (Method m : RuntimeSnapshot.class.getMethods()) {
            if (m.getName().toLowerCase().contains("serial")) {
                throw new AssertionError("RuntimeSnapshot must not expose any serial method: " + m.getName());
            }
        }
        for (Method m : PmcaRuntime.class.getMethods()) {
            if (m.getName().toLowerCase().contains("serial")) {
                throw new AssertionError("PmcaRuntime must not expose any serial method: " + m.getName());
            }
        }

        try {
            ClassLoader loader = createFixtureLoader("valid");
            Class<?> scalarClass = loader.loadClass("com.sony.scalar.sysutil.ScalarProperties");
            int serialCalls = scalarClass.getField("serialCallCount").getInt(null);
            assertEquals(Integer.valueOf(0), Integer.valueOf(serialCalls), "serialCallCount on ScalarProperties fixture must be 0");
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify serialCallCount", e);
        }
    }

    private static void testCoexistScalarEmptyNeverFallbacksToBuildModel() {
        System.out.println("-> Test 8: Scalar & Build Coexist, Empty Scalar Model Never Fallbacks to Build.MODEL");
        ClassLoader loader = createFixtureLoader("coexist_scalar_empty");
        RuntimeSnapshot snapshot = PmcaRuntime.inspect(loader);

        assertEquals(RuntimeKind.SONY_SCALAR, snapshot.getRuntimeKind(), "Kind must be SONY_SCALAR");
        assertEquals(Availability.AVAILABLE, snapshot.getScalarProvider().getAvailability(), "ScalarProvider available");
        assertEquals("CLASS_PRESENT", snapshot.getScalarProvider().getReason(), "ScalarProvider reason");

        assertEquals(Availability.UNKNOWN, snapshot.getModel().getAvailability(), "Model availability");
        assertEquals("NO_VALUE", snapshot.getModel().getReason(), "Model reason must be NO_VALUE");
        assertNull(snapshot.getModel().getValue(), "Model value must be null");

        assertEquals(Availability.AVAILABLE, snapshot.getAndroidApiLevel().getAvailability(), "AndroidApiLevel available");
        assertEquals("VALUE_READ", snapshot.getAndroidApiLevel().getReason(), "AndroidApiLevel reason");
        assertEquals(Integer.valueOf(10), snapshot.getAndroidApiLevel().getValue(), "AndroidApiLevel value");
    }

    private static void testWrongReflectedMetadataTypes() {
        System.out.println("-> Test 9: Reflected Getters Return Wrong Metadata Types");
        ClassLoader loader = createFixtureLoader("wrong_types");
        RuntimeSnapshot snapshot = PmcaRuntime.inspect(loader);

        assertEquals(RuntimeKind.SONY_SCALAR, snapshot.getRuntimeKind(), "Kind must be SONY_SCALAR");

        assertEquals(Availability.UNKNOWN, snapshot.getModel().getAvailability(), "Model availability on wrong type");
        assertEquals("READ_FAILED", snapshot.getModel().getReason(), "Model reason on wrong type");
        assertNull(snapshot.getModel().getValue(), "Model value must be null");

        assertEquals(Availability.UNKNOWN, snapshot.getCameraFirmware().getAvailability(), "Firmware availability on wrong type");
        assertEquals("READ_FAILED", snapshot.getCameraFirmware().getReason(), "Firmware reason on wrong type");
        assertNull(snapshot.getCameraFirmware().getValue(), "Firmware value must be null");

        assertEquals(Availability.UNKNOWN, snapshot.getAndroidApiLevel().getAvailability(), "AndroidApiLevel availability on wrong type");
        assertEquals("READ_FAILED", snapshot.getAndroidApiLevel().getReason(), "AndroidApiLevel reason on wrong type");
        assertNull(snapshot.getAndroidApiLevel().getValue(), "AndroidApiLevel value must be null");
    }

    private static void testStaticInitOrLinkageFailureInGetter() {
        System.out.println("-> Test 10: LinkageError in Getter Does Not Crash and Leaves Other Readers Active");
        ClassLoader loader = createFixtureLoader("static_init_failure");
        RuntimeSnapshot snapshot = PmcaRuntime.inspect(loader);

        assertEquals(RuntimeKind.SONY_SCALAR, snapshot.getRuntimeKind(), "Kind must be SONY_SCALAR");
        assertEquals(Availability.AVAILABLE, snapshot.getScalarProvider().getAvailability(), "ScalarProvider available");
        assertEquals("CLASS_PRESENT", snapshot.getScalarProvider().getReason(), "ScalarProvider reason");

        assertEquals(Availability.AVAILABLE, snapshot.getScalarStringReader().getAvailability(), "StringReader method available");
        assertEquals("METHOD_PRESENT", snapshot.getScalarStringReader().getReason(), "StringReader reason");

        assertEquals(Availability.UNKNOWN, snapshot.getModel().getAvailability(), "Model availability");
        assertEquals("READ_FAILED", snapshot.getModel().getReason(), "Model reason");
        assertNull(snapshot.getModel().getValue(), "Model value must be null");

        assertEquals(Availability.AVAILABLE, snapshot.getScalarFirmwareReader().getAvailability(), "FirmwareReader available");
        assertEquals("METHOD_PRESENT", snapshot.getScalarFirmwareReader().getReason(), "FirmwareReader reason");
        assertEquals(Availability.AVAILABLE, snapshot.getCameraFirmware().getAvailability(), "CameraFirmware available");
        assertEquals("VALUE_READ", snapshot.getCameraFirmware().getReason(), "CameraFirmware reason");
        assertEquals("1.00", snapshot.getCameraFirmware().getValue(), "CameraFirmware value");
    }

    private static void testApiVisibility() {
        System.out.println("-> Test 11: API Visibility Restrictions (Factories and constructors must be package-private)");

        // 1. Probe constructors must not be public
        for (Constructor<?> c : Probe.class.getDeclaredConstructors()) {
            if (Modifier.isPublic(c.getModifiers())) {
                throw new AssertionError("Probe constructor must not be public: " + c);
            }
        }

        // 2. Probe static factories must not be public
        for (Method m : Probe.class.getDeclaredMethods()) {
            if (m.getName().equals("available") || m.getName().equals("unavailable") || m.getName().equals("unknown")) {
                if (Modifier.isPublic(m.getModifiers())) {
                    throw new AssertionError("Probe factory '" + m.getName() + "' must not be public");
                }
            }
        }

        // 3. Probe public methods must strictly match frozen spec getters + Object methods
        Set<String> allowedProbeMethods = new HashSet<String>(Arrays.asList(
            "getAvailability", "getValue", "getReason", "equals", "hashCode", "toString"
        ));
        for (Method m : Probe.class.getMethods()) {
            if (m.getDeclaringClass() == Probe.class && !allowedProbeMethods.contains(m.getName())) {
                throw new AssertionError("Unauthorized public method on Probe: " + m.getName());
            }
        }

        // 4. RuntimeSnapshot constructors must not be public
        for (Constructor<?> c : RuntimeSnapshot.class.getDeclaredConstructors()) {
            if (Modifier.isPublic(c.getModifiers())) {
                throw new AssertionError("RuntimeSnapshot constructor must not be public: " + c);
            }
        }

        // 5. PmcaRuntime constructors must not be public
        for (Constructor<?> c : PmcaRuntime.class.getDeclaredConstructors()) {
            if (Modifier.isPublic(c.getModifiers())) {
                throw new AssertionError("PmcaRuntime constructor must not be public: " + c);
            }
        }

        // 6. PmcaRuntime must only expose public static inspect() (no args)
        for (Method m : PmcaRuntime.class.getMethods()) {
            if (m.getDeclaringClass() == PmcaRuntime.class) {
                if (!m.getName().equals("inspect") || m.getParameterTypes().length != 0) {
                    throw new AssertionError("PmcaRuntime has unexpected public method: " + m);
                }
            }
        }

        // 7. Test seam inspect(ClassLoader) must not be public
        try {
            Method seam = PmcaRuntime.class.getDeclaredMethod("inspect", ClassLoader.class);
            if (Modifier.isPublic(seam.getModifiers())) {
                throw new AssertionError("Test seam inspect(ClassLoader) must be package-private");
            }
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Test seam inspect(ClassLoader) must exist");
        }
    }

    private static void testIndividuallyMissingReaders() {
        System.out.println("-> Test 12: Individually Missing Readers (getString missing OR getFirmwareVersion missing)");

        // 12a: getString missing, getFirmwareVersion present
        ClassLoader loaderNoString = createFixtureLoader("missing_get_string");
        RuntimeSnapshot snapshotNoString = PmcaRuntime.inspect(loaderNoString);

        assertEquals(RuntimeKind.SONY_SCALAR, snapshotNoString.getRuntimeKind(), "Kind must be SONY_SCALAR");
        assertEquals(Availability.AVAILABLE, snapshotNoString.getScalarProvider().getAvailability(), "Provider available");
        assertEquals("CLASS_PRESENT", snapshotNoString.getScalarProvider().getReason(), "Provider reason");

        assertEquals(Availability.UNAVAILABLE, snapshotNoString.getScalarStringReader().getAvailability(), "StringReader unavailable");
        assertEquals("METHOD_ABSENT", snapshotNoString.getScalarStringReader().getReason(), "StringReader reason");

        assertEquals(Availability.UNAVAILABLE, snapshotNoString.getModel().getAvailability(), "Model unavailable");
        assertEquals("METHOD_ABSENT", snapshotNoString.getModel().getReason(), "Model reason");

        assertEquals(Availability.UNAVAILABLE, snapshotNoString.getSonyPlatformVersion().getAvailability(), "Platform unavailable");
        assertEquals("METHOD_ABSENT", snapshotNoString.getSonyPlatformVersion().getReason(), "Platform reason");

        assertEquals(Availability.AVAILABLE, snapshotNoString.getScalarFirmwareReader().getAvailability(), "FirmwareReader available");
        assertEquals("METHOD_PRESENT", snapshotNoString.getScalarFirmwareReader().getReason(), "FirmwareReader reason");

        assertEquals(Availability.AVAILABLE, snapshotNoString.getCameraFirmware().getAvailability(), "Firmware available");
        assertEquals("VALUE_READ", snapshotNoString.getCameraFirmware().getReason(), "Firmware reason");
        assertEquals("3.10", snapshotNoString.getCameraFirmware().getValue(), "Firmware value");

        // 12b: getFirmwareVersion missing, getString present
        ClassLoader loaderNoFw = createFixtureLoader("missing_get_firmware");
        RuntimeSnapshot snapshotNoFw = PmcaRuntime.inspect(loaderNoFw);

        assertEquals(RuntimeKind.SONY_SCALAR, snapshotNoFw.getRuntimeKind(), "Kind must be SONY_SCALAR");
        assertEquals(Availability.AVAILABLE, snapshotNoFw.getScalarProvider().getAvailability(), "Provider available");
        assertEquals("CLASS_PRESENT", snapshotNoFw.getScalarProvider().getReason(), "Provider reason");

        assertEquals(Availability.AVAILABLE, snapshotNoFw.getScalarStringReader().getAvailability(), "StringReader available");
        assertEquals("METHOD_PRESENT", snapshotNoFw.getScalarStringReader().getReason(), "StringReader reason");

        assertEquals(Availability.AVAILABLE, snapshotNoFw.getModel().getAvailability(), "Model available");
        assertEquals("VALUE_READ", snapshotNoFw.getModel().getReason(), "Model reason");
        assertEquals("ILCE-7RM2", snapshotNoFw.getModel().getValue(), "Model value");

        assertEquals(Availability.AVAILABLE, snapshotNoFw.getSonyPlatformVersion().getAvailability(), "Platform available");
        assertEquals("VALUE_READ", snapshotNoFw.getSonyPlatformVersion().getReason(), "Platform reason");
        assertEquals("2.00", snapshotNoFw.getSonyPlatformVersion().getValue(), "Platform value");

        assertEquals(Availability.UNAVAILABLE, snapshotNoFw.getScalarFirmwareReader().getAvailability(), "FirmwareReader unavailable");
        assertEquals("METHOD_ABSENT", snapshotNoFw.getScalarFirmwareReader().getReason(), "FirmwareReader reason");

        assertEquals(Availability.UNAVAILABLE, snapshotNoFw.getCameraFirmware().getAvailability(), "Firmware unavailable");
        assertEquals("METHOD_ABSENT", snapshotNoFw.getCameraFirmware().getReason(), "Firmware reason");
        assertNull(snapshotNoFw.getCameraFirmware().getValue(), "Firmware value must be null");
    }

    private static void testReaderIndependence() {
        System.out.println("-> Test 13: Reader Independence (model.name fails, version.platform and firmware succeed)");
        ClassLoader loader = createFixtureLoader("model_failure");
        RuntimeSnapshot snapshot = PmcaRuntime.inspect(loader);

        assertEquals(RuntimeKind.SONY_SCALAR, snapshot.getRuntimeKind(), "Kind must be SONY_SCALAR");

        // Model failed during read -> UNKNOWN READ_FAILED
        assertEquals(Availability.UNKNOWN, snapshot.getModel().getAvailability(), "Model availability on failure");
        assertEquals("READ_FAILED", snapshot.getModel().getReason(), "Model reason on failure");
        assertNull(snapshot.getModel().getValue(), "Model value");

        // Platform succeeded independently -> AVAILABLE VALUE_READ
        assertEquals(Availability.AVAILABLE, snapshot.getSonyPlatformVersion().getAvailability(), "Platform availability");
        assertEquals("VALUE_READ", snapshot.getSonyPlatformVersion().getReason(), "Platform reason");
        assertEquals("2.00", snapshot.getSonyPlatformVersion().getValue(), "Platform value");

        // Firmware succeeded independently -> AVAILABLE VALUE_READ
        assertEquals(Availability.AVAILABLE, snapshot.getCameraFirmware().getAvailability(), "Firmware availability");
        assertEquals("VALUE_READ", snapshot.getCameraFirmware().getReason(), "Firmware reason");
        assertEquals("3.10", snapshot.getCameraFirmware().getValue(), "Firmware value");
    }

    private static void testGenericAndroidMissingSdkField() {
        System.out.println("-> Test 14: Generic Android with Missing SDK_INT field");
        ClassLoader loader = createFixtureLoader("missing_sdk_int");
        RuntimeSnapshot snapshot = PmcaRuntime.inspect(loader);

        assertEquals(RuntimeKind.GENERIC_ANDROID, snapshot.getRuntimeKind(), "Kind must be GENERIC_ANDROID");

        assertEquals(Availability.AVAILABLE, snapshot.getModel().getAvailability(), "Model availability");
        assertEquals("VALUE_READ", snapshot.getModel().getReason(), "Model reason");
        assertEquals("Pixel 6", snapshot.getModel().getValue(), "Model value");

        assertEquals(Availability.UNAVAILABLE, snapshot.getAndroidApiLevel().getAvailability(), "AndroidApiLevel availability");
        assertEquals("CLASS_ABSENT", snapshot.getAndroidApiLevel().getReason(), "AndroidApiLevel reason");
        assertNull(snapshot.getAndroidApiLevel().getValue(), "AndroidApiLevel value");

        assertEquals(Availability.UNAVAILABLE, snapshot.getCameraFirmware().getAvailability(), "Firmware availability");
        assertEquals("NOT_APPLICABLE", snapshot.getCameraFirmware().getReason(), "Firmware reason");

        assertEquals(Availability.UNAVAILABLE, snapshot.getSonyPlatformVersion().getAvailability(), "Platform availability");
        assertEquals("NOT_APPLICABLE", snapshot.getSonyPlatformVersion().getReason(), "Platform reason");
    }

    private static void testBlockedProviderLookup() {
        System.out.println("-> Test 15: Blocked Provider Lookup (SecurityException on Class.forName)");
        ClassLoader blockedLoader = new ClassLoader(PmcaRuntimeHostTest.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if ("com.sony.scalar.sysutil.ScalarProperties".equals(name)) {
                    throw new SecurityException("Access to vendor class blocked");
                }
                return super.loadClass(name, resolve);
            }
        };

        RuntimeSnapshot snapshot = PmcaRuntime.inspect(blockedLoader);

        assertEquals(RuntimeKind.DETECTION_FAILURE, snapshot.getRuntimeKind(), "Blocked lookup must yield DETECTION_FAILURE");

        assertEquals(Availability.UNKNOWN, snapshot.getScalarProvider().getAvailability(), "ScalarProvider availability");
        assertEquals("READ_FAILED", snapshot.getScalarProvider().getReason(), "ScalarProvider reason");

        assertEquals(Availability.UNKNOWN, snapshot.getScalarStringReader().getAvailability(), "StringReader availability");
        assertEquals("READ_FAILED", snapshot.getScalarStringReader().getReason(), "StringReader reason");

        assertEquals(Availability.UNKNOWN, snapshot.getScalarFirmwareReader().getAvailability(), "FirmwareReader availability");
        assertEquals("READ_FAILED", snapshot.getScalarFirmwareReader().getReason(), "FirmwareReader reason");

        assertEquals(Availability.UNKNOWN, snapshot.getModel().getAvailability(), "Model availability");
        assertEquals("READ_FAILED", snapshot.getModel().getReason(), "Model reason");

        assertEquals(Availability.UNKNOWN, snapshot.getCameraFirmware().getAvailability(), "Firmware availability");
        assertEquals("READ_FAILED", snapshot.getCameraFirmware().getReason(), "Firmware reason");

        assertEquals(Availability.UNKNOWN, snapshot.getSonyPlatformVersion().getAvailability(), "Platform availability");
        assertEquals("READ_FAILED", snapshot.getSonyPlatformVersion().getReason(), "Platform reason");

        assertEquals(Availability.UNKNOWN, snapshot.getAndroidApiLevel().getAvailability(), "AndroidApiLevel availability");
        assertEquals("READ_FAILED", snapshot.getAndroidApiLevel().getReason(), "AndroidApiLevel reason");
    }

    private static void testMethodLookupLinkageError() {
        System.out.println("-> Test 16: Method Lookup LinkageError (NoClassDefFoundError during getMethod)");
        String baseDir = System.getProperty("openpmca.test.fixtures.baseDir");
        File fixtureDir = new File(baseDir, "fixtures_method_linkage_failure");
        try {
            URL url = fixtureDir.toURI().toURL();
            URLClassLoader loader = new URLClassLoader(new URL[] { url }, null) {
                @Override
                protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                    if ("com.sony.scalar.sysutil.MissingType".equals(name)) {
                        throw new ClassNotFoundException(name);
                    }
                    return super.loadClass(name, resolve);
                }
            };

            RuntimeSnapshot snapshot = PmcaRuntime.inspect(loader);

            // Provider must remain SONY_SCALAR and AVAILABLE (Class.forName succeeded)
            assertEquals(RuntimeKind.SONY_SCALAR, snapshot.getRuntimeKind(), "Kind must be SONY_SCALAR");
            assertEquals(Availability.AVAILABLE, snapshot.getScalarProvider().getAvailability(), "ScalarProvider available");
            assertEquals("CLASS_PRESENT", snapshot.getScalarProvider().getReason(), "ScalarProvider reason");

            // Affected reader where getMethod hit NoClassDefFoundError must degrade gracefully to UNKNOWN READ_FAILED
            assertEquals(Availability.UNKNOWN, snapshot.getScalarStringReader().getAvailability(), "StringReader availability");
            assertEquals("READ_FAILED", snapshot.getScalarStringReader().getReason(), "StringReader reason");

            // Dependent properties also degrade to UNKNOWN READ_FAILED
            assertEquals(Availability.UNKNOWN, snapshot.getModel().getAvailability(), "Model availability");
            assertEquals("READ_FAILED", snapshot.getModel().getReason(), "Model reason");

            assertEquals(Availability.UNKNOWN, snapshot.getSonyPlatformVersion().getAvailability(), "Platform availability");
            assertEquals("READ_FAILED", snapshot.getSonyPlatformVersion().getReason(), "Platform reason");

            assertEquals(Availability.UNKNOWN, snapshot.getScalarFirmwareReader().getAvailability(), "FirmwareReader availability");
            assertEquals("READ_FAILED", snapshot.getScalarFirmwareReader().getReason(), "FirmwareReader reason");

            assertEquals(Availability.UNKNOWN, snapshot.getCameraFirmware().getAvailability(), "Firmware availability");
            assertEquals("READ_FAILED", snapshot.getCameraFirmware().getReason(), "Firmware reason");

            // Android SDK lookup remains independent and succeeded
            assertEquals(Availability.AVAILABLE, snapshot.getAndroidApiLevel().getAvailability(), "AndroidApiLevel available");
            assertEquals("VALUE_READ", snapshot.getAndroidApiLevel().getReason(), "AndroidApiLevel reason");
            assertEquals(Integer.valueOf(10), snapshot.getAndroidApiLevel().getValue(), "AndroidApiLevel value");

            loader.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed in testMethodLookupLinkageError", e);
        }
    }

    private static void testInstanceMembersNonStaticDegradation() {
        System.out.println("-> Test 17: Non-Static Instance Members Degradation");

        // 17a: instance getString, static getFirmwareVersion
        ClassLoader loaderInstString = createFixtureLoader("instance_string");
        RuntimeSnapshot snapInstString = PmcaRuntime.inspect(loaderInstString);
        assertEquals(RuntimeKind.SONY_SCALAR, snapInstString.getRuntimeKind(), "17a: Kind must be SONY_SCALAR");
        assertEquals(Availability.AVAILABLE, snapInstString.getScalarProvider().getAvailability(), "17a: Provider available");
        assertEquals("CLASS_PRESENT", snapInstString.getScalarProvider().getReason(), "17a: Provider reason");
        assertEquals(Availability.AVAILABLE, snapInstString.getScalarStringReader().getAvailability(), "17a: StringReader available");
        assertEquals("METHOD_PRESENT", snapInstString.getScalarStringReader().getReason(), "17a: StringReader reason");
        assertEquals(Availability.UNKNOWN, snapInstString.getModel().getAvailability(), "17a: Model unknown on instance method");
        assertEquals("READ_FAILED", snapInstString.getModel().getReason(), "17a: Model reason on instance method");
        assertEquals(Availability.UNKNOWN, snapInstString.getSonyPlatformVersion().getAvailability(), "17a: Platform unknown on instance method");
        assertEquals("READ_FAILED", snapInstString.getSonyPlatformVersion().getReason(), "17a: Platform reason on instance method");
        assertEquals(Availability.AVAILABLE, snapInstString.getScalarFirmwareReader().getAvailability(), "17a: FirmwareReader available");
        assertEquals(Availability.AVAILABLE, snapInstString.getCameraFirmware().getAvailability(), "17a: Firmware available");
        assertEquals("3.10", snapInstString.getCameraFirmware().getValue(), "17a: Firmware value");
        assertEquals(Availability.AVAILABLE, snapInstString.getAndroidApiLevel().getAvailability(), "17a: AndroidApiLevel available");
        assertEquals(Integer.valueOf(10), snapInstString.getAndroidApiLevel().getValue(), "17a: AndroidApiLevel value");

        // 17b: static getString, instance getFirmwareVersion
        ClassLoader loaderInstFw = createFixtureLoader("instance_firmware");
        RuntimeSnapshot snapInstFw = PmcaRuntime.inspect(loaderInstFw);
        assertEquals(RuntimeKind.SONY_SCALAR, snapInstFw.getRuntimeKind(), "17b: Kind must be SONY_SCALAR");
        assertEquals(Availability.AVAILABLE, snapInstFw.getScalarProvider().getAvailability(), "17b: Provider available");
        assertEquals("CLASS_PRESENT", snapInstFw.getScalarProvider().getReason(), "17b: Provider reason");
        assertEquals(Availability.AVAILABLE, snapInstFw.getScalarStringReader().getAvailability(), "17b: StringReader available");
        assertEquals(Availability.AVAILABLE, snapInstFw.getModel().getAvailability(), "17b: Model available");
        assertEquals("ILCE-7", snapInstFw.getModel().getValue(), "17b: Model value");
        assertEquals(Availability.AVAILABLE, snapInstFw.getSonyPlatformVersion().getAvailability(), "17b: Platform available");
        assertEquals("2.00", snapInstFw.getSonyPlatformVersion().getValue(), "17b: Platform value");
        assertEquals(Availability.AVAILABLE, snapInstFw.getScalarFirmwareReader().getAvailability(), "17b: FirmwareReader available");
        assertEquals("METHOD_PRESENT", snapInstFw.getScalarFirmwareReader().getReason(), "17b: FirmwareReader reason");
        assertEquals(Availability.UNKNOWN, snapInstFw.getCameraFirmware().getAvailability(), "17b: Firmware unknown on instance method");
        assertEquals("READ_FAILED", snapInstFw.getCameraFirmware().getReason(), "17b: Firmware reason on instance method");
        assertEquals(Availability.AVAILABLE, snapInstFw.getAndroidApiLevel().getAvailability(), "17b: AndroidApiLevel available");
        assertEquals(Integer.valueOf(10), snapInstFw.getAndroidApiLevel().getValue(), "17b: AndroidApiLevel value");

        // 17c: instance Build.MODEL, static Build$VERSION.SDK_INT
        ClassLoader loaderInstModel = createFixtureLoader("instance_model");
        RuntimeSnapshot snapInstModel = PmcaRuntime.inspect(loaderInstModel);
        assertEquals(RuntimeKind.GENERIC_ANDROID, snapInstModel.getRuntimeKind(), "17c: Kind must be GENERIC_ANDROID");
        assertEquals(Availability.UNKNOWN, snapInstModel.getModel().getAvailability(), "17c: Model unknown on instance field");
        assertEquals("READ_FAILED", snapInstModel.getModel().getReason(), "17c: Model reason on instance field");
        assertEquals(Availability.AVAILABLE, snapInstModel.getAndroidApiLevel().getAvailability(), "17c: AndroidApiLevel available");
        assertEquals(Integer.valueOf(31), snapInstModel.getAndroidApiLevel().getValue(), "17c: AndroidApiLevel value");
        assertEquals(Availability.UNAVAILABLE, snapInstModel.getCameraFirmware().getAvailability(), "17c: Firmware unavailable");
        assertEquals("NOT_APPLICABLE", snapInstModel.getCameraFirmware().getReason(), "17c: Firmware reason");

        // 17d: static Build.MODEL, instance Build$VERSION.SDK_INT
        ClassLoader loaderInstSdk = createFixtureLoader("instance_sdk");
        RuntimeSnapshot snapInstSdk = PmcaRuntime.inspect(loaderInstSdk);
        assertEquals(RuntimeKind.GENERIC_ANDROID, snapInstSdk.getRuntimeKind(), "17d: Kind must be GENERIC_ANDROID");
        assertEquals(Availability.AVAILABLE, snapInstSdk.getModel().getAvailability(), "17d: Model available");
        assertEquals("Pixel 6", snapInstSdk.getModel().getValue(), "17d: Model value");
        assertEquals(Availability.UNKNOWN, snapInstSdk.getAndroidApiLevel().getAvailability(), "17d: AndroidApiLevel unknown on instance field");
        assertEquals("READ_FAILED", snapInstSdk.getAndroidApiLevel().getReason(), "17d: AndroidApiLevel reason on instance field");
        assertEquals(Availability.UNAVAILABLE, snapInstSdk.getCameraFirmware().getAvailability(), "17d: Firmware unavailable");
        assertEquals("NOT_APPLICABLE", snapInstSdk.getCameraFirmware().getReason(), "17d: Firmware reason");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError(message + " - expected: <" + expected + ">, but was: <" + actual + ">");
    }

    private static void assertNull(Object actual, String message) {
        if (actual != null) {
            throw new AssertionError(message + " - expected null, but was: <" + actual + ">");
        }
    }

    private static void assertNotNull(Object actual, String message) {
        if (actual == null) {
            throw new AssertionError(message + " - expected non-null");
        }
    }
}
