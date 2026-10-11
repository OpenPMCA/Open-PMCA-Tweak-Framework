# Read-only PMCA SDK Implementation Plan

> **For agentic workers:** Use the existing core/build and sample/verification peer sessions chosen by the user. Apply `superpowers:test-driven-development` and `superpowers:verification-before-completion` within each lane. This coordinator owns design, research, and review. No new implementer session, commit, push, or camera operation is required.

**Goal:** Deliver a reusable, host-tested read-only PMCA runtime-information JAR and minimal Android sample source without claiming an APK or camera test that was not performed.

**Architecture:** A reflection-only Java library isolates optional Sony Scalar and Android classes from plain-host execution. Immutable probes report observed symbol/data availability separately from hardware support. The sample consumes that public API and does not allocate camera resources or duplicate desktop installation logic.

**Tech Stack:** Java 6-era idioms, JDK 17 host compiler with Java 7 class output, cached Gradle 9 Java plugin, dependency-free host test runner, legacy Android API 10 sample source and manifest.

**Spec:** `docs/design/2026-10-11-read-only-sdk-contract.md`

## Continuation assignments (2026-10-11)

The user resumed implementation with the two existing active sessions. Preserve the already delivered, uncommitted SDK/sample sources; do not recreate them.

| Session | Responsibility | Completion evidence |
| --- | --- | --- |
| `open-pmca-tweak-framework-19` | Core/build lane, missing runtime regressions, minimal SDK contract fixes | Fresh offline host tests, JAR inspection and frozen public API |
| `open-pmca-tweak-framework-6a` | Sample/verification lane, formatter/runtime coverage and host runner | Tests against the stable core JAR, different-working-directory execution and manifest/source validation |
| `open-pmca-tweak-framework-14` | Coordinator, cross-lane integration, independent review and developer documentation | Fresh integrated tests, independent artifact/manifest checks, accurately bounded Android-signature evidence |

The core peer owns root Gradle runs until it reports a stable JAR. The sample peer does not edit SDK/build files or run root Gradle. The coordinator runs final integration only after both peers report their lanes stable. Completion results are recorded in `docs/research/2026-10-11-build-verification.md`; prior test-first ordering is not inferred from already existing source.

Both lanes delivered stable source. Final independent review found one reader-isolation defect, which the core owner fixed with four-path RED/GREEN regressions. The coordinator reran all 17 core scenarios and eight formatter groups, the original failure reproducers, different-working-directory/error controls, API/JAR/manifest checks and supplementary legacy compilation. The host-tested slice is complete; APK/signing/hardware remain NOT RUN. Initial test-first-ordering checkboxes below remain historical, unverified process evidence rather than an instruction to delete and recreate existing code; this continuation's contract and reflection fixes have separately recorded RED/GREEN evidence.

## Global Constraints

- Canonical root is `/Users/phucdang/Documents/Open-PMCA-Tweak-Framework` and origin is `https://github.com/OpenPMCA/Open-PMCA-Tweak-Framework.git`.
- All SDK types are in `org.openpmca.framework`. Sample package is `org.openpmca.sample`.
- Runtime enum names are `SONY_SCALAR`, `GENERIC_ANDROID`, `PLAIN_HOST`, `DETECTION_FAILURE`.
- Availability enum names are `AVAILABLE`, `UNAVAILABLE`, `UNKNOWN`.
- Public getters and exact Sony reflection signatures come from the spec. Do not rename them independently in either lane.
- SDK inspection has no serial path, filesystem/network operations, persistent settings changes, native code, camera allocation, process-kill, or arbitrary-script support.
- Use only the verified Sony property keys `model.name` and `version.platform`, plus zero-argument `getFirmwareVersion()`.
- Core does not require a Sony stub module, Android JAR, JCenter, JitPack, external test library, or Android Gradle Plugin.
- Do not use Java 7-only standard-library APIs merely because host `--release 7` succeeds. Host compilation is not Android API-signature or DEX verification.
- Sample manifest uses minimum/target SDK `10`, version name `0.1.0`, version code `1`, one launcher activity, zero permissions, zero receivers.
- APK validation requires binary Android resources/manifest, DEX, alignment before signing, valid v1 RSA signature, and explicit signature verification for minimum API 10. Missing tools must be reported as blockers.
- SDK JAR excludes all test doubles and any `com/sony/` or `android/` implementation classes.
- No commits, pushes, global tool installation, desktop repository edits, release publication, or camera installation/execution.

## Review Focus

- A present vendor class with a missing reader must not crash or be reported as complete feature support.
- A native getter failure or empty value must produce `UNKNOWN`, while absent symbols produce `UNAVAILABLE`.
- Generic Android OS/build data must never be labeled as Sony camera firmware or a physical-camera test.
- Test doubles and host-only fixtures must never enter the SDK JAR or eventual application DEX.
- Sample input and metadata must remain zero-permission/receiver, support MENU exit only, and avoid serial data even when a test double could supply it.

## File Ownership

| Lane | Files |
| --- | --- |
| Core/build peer | Root Gradle files and wrapper, `.gitignore`, `framework/`, `docs/development/build-prerequisites.md` |
| Sample/verification peer | `sample/`, `docs/testing/read-only-camera-smoke-test.md` |
| Coordinator | `README.md`, `docs/research/`, `docs/design/`, this plan, final cross-lane review |

Only the core/build peer changes root build files. The root build includes the framework initially; sample Android compilation is not silently folded into a passing host-only task when Android tools are absent.

### Task 1: Host-tested immutable SDK and reflection adapter

**Owner:** Core/build peer.

**Files:**
- Create: `settings.gradle`, `build.gradle`, Gradle wrapper files, `.gitignore`.
- Create: `framework/build.gradle`.
- Create: `framework/src/main/java/org/openpmca/framework/Availability.java`.
- Create: `framework/src/main/java/org/openpmca/framework/RuntimeKind.java`.
- Create: `framework/src/main/java/org/openpmca/framework/Probe.java`.
- Create: `framework/src/main/java/org/openpmca/framework/RuntimeSnapshot.java`.
- Create: `framework/src/main/java/org/openpmca/framework/PmcaRuntime.java`.
- Create: `framework/src/test/java/org/openpmca/framework/PmcaRuntimeHostTest.java` and isolated fake-runtime fixtures.

**Interfaces:**
- Produces: `PmcaRuntime.inspect()` and all getters in the spec.
- Package-private test seam: `PmcaRuntime.inspect(ClassLoader loader)`.
- Public values are immutable. Package-private constructors/factories may serve tests without adding a public provider registry.

- [ ] **Write failing host tests before production implementation.** Test the public host fallback and then controlled loaders/fixtures for each named condition. Use a dependency-free `main` runner whose failed assertions throw `AssertionError`.

```java
RuntimeSnapshot result = PmcaRuntime.inspect();
if (result.getRuntimeKind() != RuntimeKind.PLAIN_HOST) {
    throw new AssertionError("Plain JVM must not be reported as a Sony camera");
}
if (result.getScalarProvider().getAvailability() != Availability.UNAVAILABLE) {
    throw new AssertionError("Missing Scalar provider must be unavailable");
}
if (result.getCameraFirmware().getValue() != null) {
    throw new AssertionError("Host cannot supply Sony camera firmware");
}
```

Add fixtures and concrete assertions for successful model/platform/firmware reads, missing `getString`, missing `getFirmwareVersion`, blocked lookup, native exception wrapped by reflection, linkage failure, null/empty data, generic Android Build, missing SDK field, and a serial-getter counter that stays zero. Isolate fake classes from the production output and each other with a test class loader. Check that a failure in one reader does not prevent the other fields from being read.

- [ ] **Run the failing tests and record the failure.** They must fail because the production API is missing or incorrect, not because test dependencies are unavailable.

- [x] **Implement only the frozen immutable types and narrow reflection adapter.** Provider lookup should avoid class initialization while checking presence:

```java
Class<?> scalar = Class.forName(
    "com.sony.scalar.sysutil.ScalarProperties", false, loader);
java.lang.reflect.Method stringReader = scalar.getMethod(
    "getString", new Class<?>[] { String.class });
java.lang.reflect.Method firmwareReader = scalar.getMethod(
    "getFirmwareVersion", new Class<?>[0]);
Object model = stringReader.invoke(null, new Object[] { "model.name" });
Object platform = stringReader.invoke(null, new Object[] { "version.platform" });
Object firmware = firmwareReader.invoke(null, new Object[0]);
```

Each lookup/read is independent and guarded. Do not use this sketch as one unguarded block. Catch expected reflection, security, and linkage failures without swallowing every `Throwable`. Fixed reason strings must not leak native exception messages. Known missing symbols are unavailable, read failure or no value is unknown. Android fallback reads only model and SDK API metadata and leaves camera firmware/platform not applicable.

- [x] **Wire reproducible offline host verification.** The framework module uses the Java plugin, JDK 17 toolchain, and `options.release = 7`. Define `:framework:hostTest` as a JavaExec task over test runtime output running the dependency-free test main, and make `check` depend on it. Build a standard JAR. Generate the wrapper using the existing trusted cached Gradle distribution without adding remote production dependencies.

```groovy
plugins { id 'java-library' }
java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }
}
tasks.withType(JavaCompile).configureEach { options.release = 7 }
tasks.register('hostTest', JavaExec) {
    dependsOn testClasses
    classpath = sourceSets.test.runtimeClasspath
    mainClass = 'org.openpmca.framework.PmcaRuntimeHostTest'
}
tasks.named('check') { dependsOn tasks.named('hostTest') }
```

- [x] **Run fresh verification and inspect artifacts.**

```bash
./gradlew --offline :framework:hostTest :framework:jar
jar tf framework/build/libs/*.jar
javap -v -classpath framework/build/libs/*.jar org.openpmca.framework.PmcaRuntime
```

Record passed cases and class-file major version. Require no production JAR entries under `com/sony/` or `android/`, and no test fixture classes. Report Android API signature, DEX, APK, and hardware verification as not performed. Send the sample peer the JAR path and confirmed API signatures. Do not commit.

### Task 2: Read-only sample source, manifest, and host formatter tests

**Owner:** Sample/verification peer. Can start against the frozen interfaces while Task 1 runs.

**Files:**
- Create: `sample/src/main/AndroidManifest.xml`.
- Create: `sample/src/main/java/org/openpmca/sample/HelloCameraActivity.java`.
- Create: `sample/src/main/java/org/openpmca/sample/DiagnosticsFormatter.java`.
- Create: `sample/src/test/java/org/openpmca/sample/DiagnosticsFormatterHostTest.java`.
- Create: `sample/test-host.sh`, `sample/README.md`.
- Update: `docs/testing/read-only-camera-smoke-test.md` only if needed for the actual sample.

**Interfaces:**
- Consumes: `PmcaRuntime.inspect()`, snapshot getters, probe getters, and enums from the spec.
- Produces: `DiagnosticsFormatter.format(RuntimeSnapshot)` returning human-readable plain text.
- Package-private formatter test helper may accept label, availability, value, and reason directly to test unknown/unavailable rendering without Android dependencies.

- [ ] **Write failing formatter tests first.** Assert missing values display `Unknown` plus their actual status/reason, provider presence is not described as hardware verified, generic Android has no camera firmware value, and host output is explicitly unverified. Test all enum states. The formatter must not query runtime classes itself.

```java
String text = DiagnosticsFormatter.format(PmcaRuntime.inspect());
if (text.indexOf("PLAIN_HOST") < 0) {
    throw new AssertionError("Host kind must be visible");
}
if (text.indexOf("Hardware verification: NOT RUN") < 0) {
    throw new AssertionError("Formatter must not imply hardware testing");
}
```

- [x] **Implement the pure-Java formatter, then the minimal legacy activity.** The activity inspects once per resume or create and renders the snapshot. Do not allocate camera/display resources or background work. Use ordinary TextView/ScrollView widgets and no external UI library.

```java
public final class HelloCameraActivity extends android.app.Activity {
    @Override
    protected void onCreate(android.os.Bundle state) {
        super.onCreate(state);
        android.widget.TextView text = new android.widget.TextView(this);
        text.setText(DiagnosticsFormatter.format(
            org.openpmca.framework.PmcaRuntime.inspect()));
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(text);
        setContentView(scroll);
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_MENU) {
            finish();
            startActivity(new android.content.Intent(android.content.Intent.ACTION_MAIN)
                .addCategory(android.content.Intent.CATEGORY_HOME)
                .setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
```

- [x] **Add the exact minimal manifest.**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="org.openpmca.sample"
    android:versionCode="1" android:versionName="0.1.0">
    <uses-sdk android:minSdkVersion="10" android:targetSdkVersion="10" />
    <application android:label="Hello Camera"
        android:icon="@android:drawable/ic_menu_info_details">
        <activity android:name=".HelloCameraActivity"
            android:theme="@android:style/Theme.Black.NoTitleBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [x] **Make formatter tests executable offline without compiling Android Activity.** `sample/test-host.sh` resolves paths from its own location, uses a configurable JDK 17 compiler, consumes the built framework JAR, compiles only the formatter and host test, runs the dependency-free test main, and exits nonzero on any failure. It must not pretend to assemble an APK or install anything.

- [x] **Verify manifest and source boundaries.** Parse source XML to check exactly one activity, MAIN/LAUNCHER in the same filter, min/target/version metadata, no permission elements and no receiver. Inspect sample source for no serial, direct Scalar, CameraEx, DisplayManager, network, storage, APO, or process-kill calls.

- [x] **Run formatter tests with the actual core JAR.** Report test results separately from Android compilation. Document that Android SDK/resource/DEX/signing tools are missing and APK/hardware verification remains not performed. Include the existing manual test protocol link. Do not create an untested APK-build success claim or an install script. Do not commit.

### Task 3: Cross-lane review and accurate developer entry point

**Owner:** Coordinator.

**Files:**
- Review all new SDK, tests, build configuration, and sample source.
- Update `README.md` with the current repository name, initial capabilities, source usage, exact host test commands, proof boundaries, missing Android tool prerequisites, and links to research/design/manual testing.
- Keep research facts consistent with actual artifacts and peer results.

- [x] **Review frozen API and peer diffs against the contract.** Reject serial reads, bundled fake/vendor classes, camera-hardware operations, mutable support flags, mandatory process-kill/receiver scaffolding, or API naming drift.
- [x] **Run the actual core and formatter tests fresh.** Inspect SDK contents and public signatures. Independently validate the sample manifest and zero-permission/receiver boundary.
- [x] **Run an independent code-review workflow over the actual implementation.** Give the reviewer source, not prior conclusions. Verify each actionable finding before asking the owning peer to fix it. Rerun the affected checks after changes.
- [x] **Report only verified completion.** This milestone is complete when core host tests/JAR inspection and sample formatter/manifest checks pass. An APK build or camera support claim requires additional evidence and must remain explicitly unverified if blocked by tools or hardware access.
