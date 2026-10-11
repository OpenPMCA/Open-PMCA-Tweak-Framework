package org.openpmca.framework;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Entry point for runtime and device capability inspection on Sony PMCA and host platforms.
 */
public final class PmcaRuntime {

    private PmcaRuntime() {
    }

    /**
     * Inspects the current runtime environment using the default class loader.
     *
     * @return an immutable snapshot of observed runtime properties and capability probes
     */
    public static RuntimeSnapshot inspect() {
        return inspect(PmcaRuntime.class.getClassLoader());
    }

    /**
     * Package-private test seam allowing inspection against isolated class loaders.
     */
    static RuntimeSnapshot inspect(ClassLoader loader) {
        if (loader == null) {
            loader = PmcaRuntime.class.getClassLoader();
        }

        try {
            return performInspection(loader);
        } catch (Exception e) {
            return new RuntimeSnapshot(
                RuntimeKind.DETECTION_FAILURE,
                Probe.<Boolean>unknown("READ_FAILED"),
                Probe.<Boolean>unknown("READ_FAILED"),
                Probe.<Boolean>unknown("READ_FAILED"),
                Probe.<String>unknown("READ_FAILED"),
                Probe.<String>unknown("READ_FAILED"),
                Probe.<String>unknown("READ_FAILED"),
                Probe.<Integer>unknown("READ_FAILED")
            );
        } catch (LinkageError e) {
            return new RuntimeSnapshot(
                RuntimeKind.DETECTION_FAILURE,
                Probe.<Boolean>unknown("READ_FAILED"),
                Probe.<Boolean>unknown("READ_FAILED"),
                Probe.<Boolean>unknown("READ_FAILED"),
                Probe.<String>unknown("READ_FAILED"),
                Probe.<String>unknown("READ_FAILED"),
                Probe.<String>unknown("READ_FAILED"),
                Probe.<Integer>unknown("READ_FAILED")
            );
        }
    }

    private static RuntimeSnapshot performInspection(ClassLoader loader) {
        Class<?> scalarClass = null;
        Probe<Boolean> scalarProvider;

        try {
            scalarClass = Class.forName("com.sony.scalar.sysutil.ScalarProperties", false, loader);
            scalarProvider = Probe.available(Boolean.TRUE, "CLASS_PRESENT");
        } catch (ClassNotFoundException e) {
            scalarProvider = Probe.unavailable("CLASS_ABSENT");
        } catch (LinkageError e) {
            scalarProvider = Probe.unknown("READ_FAILED");
        } catch (SecurityException e) {
            scalarProvider = Probe.unknown("READ_FAILED");
        }

        if (scalarClass != null) {
            return inspectSonyScalar(scalarClass, scalarProvider, loader);
        } else if (scalarProvider.getAvailability() == Availability.UNKNOWN) {
            // Blocked or inconclusive vendor class detection
            return new RuntimeSnapshot(
                RuntimeKind.DETECTION_FAILURE,
                scalarProvider,
                Probe.<Boolean>unknown("READ_FAILED"),
                Probe.<Boolean>unknown("READ_FAILED"),
                Probe.<String>unknown("READ_FAILED"),
                Probe.<String>unknown("READ_FAILED"),
                Probe.<String>unknown("READ_FAILED"),
                Probe.<Integer>unknown("READ_FAILED")
            );
        } else {
            return inspectNonScalar(scalarProvider, loader);
        }
    }

    private static RuntimeSnapshot inspectSonyScalar(Class<?> scalarClass, Probe<Boolean> scalarProvider, ClassLoader loader) {
        Method getStringMethod = null;
        Probe<Boolean> scalarStringReader;
        try {
            getStringMethod = scalarClass.getMethod("getString", new Class<?>[] { String.class });
            scalarStringReader = Probe.available(Boolean.TRUE, "METHOD_PRESENT");
        } catch (NoSuchMethodException e) {
            scalarStringReader = Probe.unavailable("METHOD_ABSENT");
        } catch (LinkageError e) {
            scalarStringReader = Probe.unknown("READ_FAILED");
        } catch (SecurityException e) {
            scalarStringReader = Probe.unknown("READ_FAILED");
        }

        Method getFirmwareMethod = null;
        Probe<Boolean> scalarFirmwareReader;
        try {
            getFirmwareMethod = scalarClass.getMethod("getFirmwareVersion", new Class<?>[0]);
            scalarFirmwareReader = Probe.available(Boolean.TRUE, "METHOD_PRESENT");
        } catch (NoSuchMethodException e) {
            scalarFirmwareReader = Probe.unavailable("METHOD_ABSENT");
        } catch (LinkageError e) {
            scalarFirmwareReader = Probe.unknown("READ_FAILED");
        } catch (SecurityException e) {
            scalarFirmwareReader = Probe.unknown("READ_FAILED");
        }

        Probe<String> model;
        if (getStringMethod == null) {
            model = (scalarStringReader.getAvailability() == Availability.UNKNOWN)
                ? Probe.<String>unknown("READ_FAILED")
                : Probe.<String>unavailable("METHOD_ABSENT");
        } else {
            model = readStringProperty(getStringMethod, "model.name");
        }

        Probe<String> sonyPlatformVersion;
        if (getStringMethod == null) {
            sonyPlatformVersion = (scalarStringReader.getAvailability() == Availability.UNKNOWN)
                ? Probe.<String>unknown("READ_FAILED")
                : Probe.<String>unavailable("METHOD_ABSENT");
        } else {
            sonyPlatformVersion = readStringProperty(getStringMethod, "version.platform");
        }

        Probe<String> cameraFirmware;
        if (getFirmwareMethod == null) {
            cameraFirmware = (scalarFirmwareReader.getAvailability() == Availability.UNKNOWN)
                ? Probe.<String>unknown("READ_FAILED")
                : Probe.<String>unavailable("METHOD_ABSENT");
        } else {
            cameraFirmware = readFirmwareVersion(getFirmwareMethod);
        }

        Probe<Integer> androidApiLevel = readAndroidSdkLevel(loader, true);

        return new RuntimeSnapshot(
            RuntimeKind.SONY_SCALAR,
            scalarProvider,
            scalarStringReader,
            scalarFirmwareReader,
            model,
            cameraFirmware,
            sonyPlatformVersion,
            androidApiLevel
        );
    }

    private static RuntimeSnapshot inspectNonScalar(Probe<Boolean> scalarProvider, ClassLoader loader) {
        Probe<Boolean> scalarStringReader = Probe.unavailable("CLASS_ABSENT");
        Probe<Boolean> scalarFirmwareReader = Probe.unavailable("CLASS_ABSENT");
        Probe<String> cameraFirmware = Probe.unavailable("NOT_APPLICABLE");
        Probe<String> sonyPlatformVersion = Probe.unavailable("NOT_APPLICABLE");

        Class<?> buildClass = null;
        boolean buildDetectionBlocked = false;
        try {
            buildClass = Class.forName("android.os.Build", false, loader);
        } catch (ClassNotFoundException e) {
            buildClass = null;
        } catch (LinkageError e) {
            buildClass = null;
            buildDetectionBlocked = true;
        } catch (SecurityException e) {
            buildClass = null;
            buildDetectionBlocked = true;
        }

        if (buildDetectionBlocked) {
            return new RuntimeSnapshot(
                RuntimeKind.DETECTION_FAILURE,
                scalarProvider,
                scalarStringReader,
                scalarFirmwareReader,
                Probe.<String>unknown("READ_FAILED"),
                cameraFirmware,
                sonyPlatformVersion,
                Probe.<Integer>unknown("READ_FAILED")
            );
        }

        RuntimeKind runtimeKind;
        Probe<String> model;
        Probe<Integer> androidApiLevel;

        if (buildClass != null) {
            runtimeKind = RuntimeKind.GENERIC_ANDROID;
            model = readAndroidModel(buildClass);
            androidApiLevel = readAndroidSdkLevel(loader, false);
        } else {
            runtimeKind = RuntimeKind.PLAIN_HOST;
            model = Probe.unavailable("NOT_APPLICABLE");
            androidApiLevel = Probe.unavailable("NOT_APPLICABLE");
        }

        return new RuntimeSnapshot(
            runtimeKind,
            scalarProvider,
            scalarStringReader,
            scalarFirmwareReader,
            model,
            cameraFirmware,
            sonyPlatformVersion,
            androidApiLevel
        );
    }

    private static Probe<String> readStringProperty(Method getStringMethod, String key) {
        if (!Modifier.isStatic(getStringMethod.getModifiers())) {
            return Probe.unknown("READ_FAILED");
        }
        try {
            Object val = getStringMethod.invoke(null, new Object[] { key });
            if (val == null) {
                return Probe.unknown("NO_VALUE");
            }
            if (val instanceof String) {
                String s = ((String) val).trim();
                if (s.length() == 0) {
                    return Probe.unknown("NO_VALUE");
                }
                return Probe.available(s, "VALUE_READ");
            }
            return Probe.unknown("READ_FAILED");
        } catch (InvocationTargetException e) {
            return Probe.unknown("READ_FAILED");
        } catch (IllegalAccessException e) {
            return Probe.unknown("READ_FAILED");
        } catch (IllegalArgumentException e) {
            return Probe.unknown("READ_FAILED");
        } catch (NullPointerException e) {
            return Probe.unknown("READ_FAILED");
        } catch (ExceptionInInitializerError e) {
            return Probe.unknown("READ_FAILED");
        } catch (LinkageError e) {
            return Probe.unknown("READ_FAILED");
        } catch (SecurityException e) {
            return Probe.unknown("READ_FAILED");
        }
    }

    private static Probe<String> readFirmwareVersion(Method getFirmwareMethod) {
        if (!Modifier.isStatic(getFirmwareMethod.getModifiers())) {
            return Probe.unknown("READ_FAILED");
        }
        try {
            Object val = getFirmwareMethod.invoke(null, new Object[0]);
            if (val == null) {
                return Probe.unknown("NO_VALUE");
            }
            if (val instanceof String) {
                String s = ((String) val).trim();
                if (s.length() == 0) {
                    return Probe.unknown("NO_VALUE");
                }
                return Probe.available(s, "VALUE_READ");
            }
            return Probe.unknown("READ_FAILED");
        } catch (InvocationTargetException e) {
            return Probe.unknown("READ_FAILED");
        } catch (IllegalAccessException e) {
            return Probe.unknown("READ_FAILED");
        } catch (IllegalArgumentException e) {
            return Probe.unknown("READ_FAILED");
        } catch (NullPointerException e) {
            return Probe.unknown("READ_FAILED");
        } catch (ExceptionInInitializerError e) {
            return Probe.unknown("READ_FAILED");
        } catch (LinkageError e) {
            return Probe.unknown("READ_FAILED");
        } catch (SecurityException e) {
            return Probe.unknown("READ_FAILED");
        }
    }

    private static Probe<String> readAndroidModel(Class<?> buildClass) {
        try {
            Field field = buildClass.getField("MODEL");
            if (!Modifier.isStatic(field.getModifiers())) {
                return Probe.unknown("READ_FAILED");
            }
            Object val = field.get(null);
            if (val == null) {
                return Probe.unknown("NO_VALUE");
            }
            if (val instanceof String) {
                String s = ((String) val).trim();
                if (s.length() == 0) {
                    return Probe.unknown("NO_VALUE");
                }
                return Probe.available(s, "VALUE_READ");
            }
            return Probe.unknown("READ_FAILED");
        } catch (NoSuchFieldException e) {
            return Probe.unavailable("FIELD_ABSENT");
        } catch (IllegalAccessException e) {
            return Probe.unknown("READ_FAILED");
        } catch (NullPointerException e) {
            return Probe.unknown("READ_FAILED");
        } catch (ExceptionInInitializerError e) {
            return Probe.unknown("READ_FAILED");
        } catch (LinkageError e) {
            return Probe.unknown("READ_FAILED");
        } catch (SecurityException e) {
            return Probe.unknown("READ_FAILED");
        }
    }

    private static Probe<Integer> readAndroidSdkLevel(ClassLoader loader, boolean isScalarRuntime) {
        try {
            Class<?> versionClass = Class.forName("android.os.Build$VERSION", false, loader);
            Field field = versionClass.getField("SDK_INT");
            if (!Modifier.isStatic(field.getModifiers())) {
                return Probe.unknown("READ_FAILED");
            }
            Object val = field.get(null);
            if (val instanceof Integer) {
                return Probe.available((Integer) val, "VALUE_READ");
            }
            if (val instanceof Number) {
                return Probe.available(Integer.valueOf(((Number) val).intValue()), "VALUE_READ");
            }
            if (val == null) {
                return Probe.unknown("NO_VALUE");
            }
            return Probe.unknown("READ_FAILED");
        } catch (ClassNotFoundException e) {
            return Probe.unavailable(isScalarRuntime ? "CLASS_ABSENT" : "CLASS_ABSENT");
        } catch (NoSuchFieldException e) {
            return Probe.unavailable("FIELD_ABSENT");
        } catch (IllegalAccessException e) {
            return Probe.unknown("READ_FAILED");
        } catch (NullPointerException e) {
            return Probe.unknown("READ_FAILED");
        } catch (ExceptionInInitializerError e) {
            return Probe.unknown("READ_FAILED");
        } catch (LinkageError e) {
            return Probe.unknown("READ_FAILED");
        } catch (SecurityException e) {
            return Probe.unknown("READ_FAILED");
        }
    }
}
