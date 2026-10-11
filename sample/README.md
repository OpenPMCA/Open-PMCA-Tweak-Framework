# Open-PMCA Sample Application: Hello Camera

A minimal, read-only diagnostic sample application demonstrating integration with the Open-PMCA Framework (`org.openpmca.framework`).

---

## 1. Overview & Architectural Boundaries

The Hello Camera sample is designed as a **pure consumer** of the Open-PMCA SDK. It inspects and renders device identity and capability probes without invoking camera-specific hardware operations or modifying any settings.

### Key Architectural Invariants
* **Strictly Read-Only**: Performs zero camera operations (no `CameraEx` shutter or focus actuation), zero storage writes, zero network calls, zero settings modifications, and zero native/shell commands.
* **Zero Serial Collection**: Camera Serial Numbers (`ScalarProperties.PROP_MODEL_SERIAL_CODE` and `android.os.Build.SERIAL`) are **never queried, collected, or displayed** by architectural design.
* **Zero Permissions**: The application declares zero `<uses-permission>` elements in its `AndroidManifest.xml`.
* **Zero Receivers**: The application declares zero `<receiver>` elements. It does not include inherited hacks (such as WebView SQLite process-kill or boot logs).
* **Clean Standard Exit**: Intercepts `KeyEvent.KEYCODE_MENU` to finish the activity and return cleanly to the Sony camera launcher (`Intent.CATEGORY_HOME`).
* **Hardware Verification Separation**: Displays `Hardware verification: NOT RUN` prominently. Finding available classes or methods does not imply hardware verification.

---

## 2. Component Structure

```
sample/
├── README.md                    # This document
├── test-host.sh                 # Dependency-free host-side test runner (JDK 17, --release 7)
└── src/
    ├── main/
    │   ├── AndroidManifest.xml  # Minimal API 10 manifest: 1 activity, 0 permissions, 0 receivers
    │   └── java/org/openpmca/sample/
    │       ├── DiagnosticsFormatter.java  # Pure-Java formatter rendering RuntimeSnapshot to text
    │       └── HelloCameraActivity.java   # Minimal Android Activity (API 10, ScrollView + TextView)
    └── test/
        ├── java/org/openpmca/sample/DiagnosticsFormatterHostTest.java
        ├── java/org/openpmca/framework/RuntimeInspectionTestAccess.java # Test seam only
        └── fixtures/              # Isolated, self-written Android/Scalar test doubles
```

---

## 3. Host-Side Formatter Testing

`DiagnosticsFormatter` contains zero `android.*` dependencies. It can be compiled and verified completely offline on a plain desktop JVM using the built framework JAR:

### Prerequisites
1. JDK 17 installed (provides compiler supporting `--release 7` / Java 7 class output).
2. Built core framework JAR (built via `./gradlew :framework:jar`).

### Running the Test
```bash
./sample/test-host.sh
```

Or with explicit compiler / framework JAR overrides:
```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
FRAMEWORK_JAR=/path/to/framework.jar \
./sample/test-host.sh
```

**Verification Guarantees**:
* Compiles the formatter, tests, and fixtures to Java 7 class output (`--release 7`, class-file version 51).
* Verifies all four runtime kinds: `SONY_SCALAR`, `GENERIC_ANDROID`, `PLAIN_HOST`, and `DETECTION_FAILURE`.
* Verifies `Hardware verification: NOT RUN` is present on all snapshots.
* Verifies that unavailable or missing properties display `Unknown [STATUS] (REASON)`.
* Verifies that generic Android snapshots do not display camera firmware.
* Verifies that no serial number fields or raw exception messages are rendered.
* Verifies that empty, whitespace-only, and null field reads retain `UNKNOWN` / `NO_VALUE`, while discovered readers remain `AVAILABLE`.

Fixtures are compiled only under `sample/build/host-test-classes/fixtures/`, outside the ordinary test classpath. Each fixture is loaded explicitly for its test; the default host inspection therefore remains `PLAIN_HOST`. The test-only bridge reaches the package-private inspection seam without expanding the SDK public API. Never feed any `src/test/` or host-test output into an application DEX or SDK JAR.

The host runner does not compile `HelloCameraActivity`, validate Android platform signatures, generate DEX, assemble/sign an APK, or interact with a camera. Java 7 class output is not Android API 10 runtime compatibility. Even a separate narrow compile against Android API 10 signatures does not replace full Android bootclasspath, resource, DEX, alignment, and signing verification.

---

## 4. Hardware Verification & Manual Smoke Testing

Deploying and verifying this application on physical Sony camera hardware requires:
1. Android API 10 signatures and actual Android build-tools for binary resources/manifest, DEX generation, alignment **before signing**, and a valid v1 JAR **RSA** signature verified explicitly for minimum API 10.
2. Physical camera running Sony PMCA firmware (Android 2.3.7 or 4.1.2).
3. Installation via USB Mass Storage using `Sony-PMCA-RE` (`pmca-console` / `pmca-gui`).

If any required Android packaging/signing tool is absent, the APK build and signature verification remain **`NOT RUN / MISSING LOCAL TOOLCHAIN`**. A command named `dx` is not sufficient evidence that the Android DEX tool is installed; inspect its actual origin. Physical camera installation, launch, MENU/HOME exit, and repeated-launch checks remain separately **`NOT RUN`** until a later authorized manual test.

For the detailed 5-step manual camera smoke test protocol and evidence submission template, see:
* [`docs/testing/read-only-camera-smoke-test.md`](../docs/testing/read-only-camera-smoke-test.md)
