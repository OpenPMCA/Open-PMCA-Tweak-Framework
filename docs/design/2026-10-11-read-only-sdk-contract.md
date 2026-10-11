# Proposed read-only SDK contract

Date: 2026-10-11
Status: initial working contract issued by the coordinator for the user-requested development task. This is not a released SDK, proof of hardware compatibility, or a claim that the user reviewed and approved this exact API.

## Goal and explicit scope

Establish a reusable application-development foundation for Sony PlayMemories Camera Apps cameras. The initial vertical slice is a read-only runtime/device-information SDK and a small diagnostic Android application that consumes it.

This first slice is intentionally not a complete photography SDK or a tweak app. Capture, live view, focus control, display switching, persistent configuration writes, arbitrary scripts, USB transport, and firmware operations are outside it. They can become later, separately verified features without changing the distinction between the desktop installer and the camera runtime.

The coordinator owns this contract. Core/build and sample/verification are separate implementation lanes. The Sony read paths below have been directly checked against the pinned upstream source. The two existing peer sessions implement separate lanes under this working contract.

## Artifact and platform boundaries

- SDK: a standard Java JAR while it contains no Android UI resources. Do not introduce an AAR or an Android Gradle Plugin dependency into the core merely for packaging.
- Sample: an Android APK built separately from host-only SDK verification. Its eventual minimum SDK is API 10, following the directly inspected legacy application baseline.
- Runtime API discipline: use Java 6-era language idioms and Android API 10-compatible library calls. The currently available JDK 17 compiler can emit Java 7 class files, which is a host build choice, not a runtime compatibility claim.
- No Kotlin, Compose, native subsystem, plugin loader, arbitrary-script engine, or speculative device-profile database is needed for this slice.
- Do not ship Sony Scalar or Android compile-time stub classes in the SDK JAR or sample DEX. Tests may use isolated, self-written test doubles that never enter production artifacts.
- Reuse upstream MIT-licensed source only with its original notices retained. Do not assign a new project license or publish artifacts as part of this initial research/coordination task.

A Java SE release compiler check is not an Android API-level check. In particular, an Android JAR on the ordinary classpath does not replace the compiler's Java platform signatures. Java 7-only library calls such as `AutoCloseable` or `java.nio.file` must not enter this API 10 slice merely because a host compilation succeeds.

## Proposed public behaviour

Use a small public surface in the package `org.openpmca.framework`:

- A runtime-inspection entry point returns one immutable snapshot.
- The snapshot distinguishes Sony Scalar runtime, generic Android, plain Java host, and detection failure. Finding Scalar classes does not by itself establish that the device is an officially supported PMCA camera.
- Expose model name, camera firmware, Sony platform version, and Android API level only when a matching, verified read path supplies them.
- Missing string fields remain absent and the example displays `Unknown`. Never substitute an Android OS release for Sony camera firmware or invent a device generation from a model-name guess.
- Do not read or expose serial numbers in this first slice. This avoids an unnecessary private-data path rather than attempting to sanitize it after collection.
- Provider presence, read-method presence, and individual field-read results use `AVAILABLE`, `UNAVAILABLE`, or `UNKNOWN`, with short fixed diagnostic reasons.
- Class or method absence is `UNAVAILABLE`. A blocked lookup or failed native read is `UNKNOWN`. A discovered provider/read symbol is not hardware verification.
- Read-only getters that actually succeed may be marked available, but unrelated camera features remain untested. The SDK contains no global `all cameras supported` or `hardware verified` flag.

The public names are frozen for this initial slice. All types are in `org.openpmca.framework`.

```java
public enum Availability { AVAILABLE, UNAVAILABLE, UNKNOWN }
public enum RuntimeKind { SONY_SCALAR, GENERIC_ANDROID, PLAIN_HOST, DETECTION_FAILURE }

public final class Probe<T> {
    public Availability getAvailability();
    public T getValue();
    public String getReason();
}

public final class RuntimeSnapshot {
    public RuntimeKind getRuntimeKind();
    public Probe<Boolean> getScalarProvider();
    public Probe<Boolean> getScalarStringReader();
    public Probe<Boolean> getScalarFirmwareReader();
    public Probe<String> getModel();
    public Probe<String> getCameraFirmware();
    public Probe<String> getSonyPlatformVersion();
    public Probe<Integer> getAndroidApiLevel();
}

public final class PmcaRuntime {
    public static RuntimeSnapshot inspect();
}
```

These declarations describe the contract rather than compilable class bodies. Constructor/factory helpers and class-loader injection remain package-private. Runtime inspection must return an immutable snapshot rather than a public mutable provider/plugin registry.

`Probe` holds a non-null value only for `AVAILABLE`; unavailable and unknown probes hold null. Availability of provider/read symbols uses `Boolean.TRUE` for the available value. Use fixed diagnostic reason strings, never arbitrary exception messages: `CLASS_PRESENT`, `METHOD_PRESENT`, `VALUE_READ`, `CLASS_ABSENT`, `METHOD_ABSENT`, `FIELD_ABSENT`, `NOT_APPLICABLE`, `READ_FAILED`, `NO_VALUE`. Empty strings and failed native reads produce `UNKNOWN`, not verified absence.

`SONY_SCALAR` means the ScalarProperties class was discovered, not that installation or a camera feature was hardware-verified. If that class is absent but Android Build is available, use `GENERIC_ANDROID`. If both are known absent, use `PLAIN_HOST`. If environment/provider detection is blocked or inconclusive, use `DETECTION_FAILURE`. Generic Android supplies model and Android API metadata only; Sony camera firmware and Scalar platform remain unavailable with reason `NOT_APPLICABLE`.

Only the following audited Sony read paths are permitted in this slice:

- Class: `com.sony.scalar.sysutil.ScalarProperties`.
- Model: public static `getString(String)` with argument `model.name`.
- Sony platform: public static `getString(String)` with argument `version.platform`.
- Camera firmware: public static `getFirmwareVersion()` with no arguments.

Source commit: `f8df3504d8684433a331ee869e64f67cc8be9730` in `ma1co/OpenMemories-Framework`. The source evidence is in `framework/src/main/java/com/github/ma1co/openmemories/framework/DeviceInfo.java` and `stubs/src/main/jasmin/com/sony/scalar/sysutil/ScalarProperties.j`. Reflection with these verified signatures avoids any need to create, download, or bundle a Sony stub artifact. No speculative Scalar method names or runtime implementation stubs are acceptable.

Android fallback uses reflective access to `android.os.Build.MODEL` and `android.os.Build$VERSION.SDK_INT`, so host compilation does not require Android jars. A Java host returns absent Android metadata explicitly; it does not pretend to be a camera. Test fixtures use isolated fake classes only in test outputs.

Do not catch every `Throwable` as a substitute for understanding expected reflection and linkage failures. A test seam can inject a class loader or small package-private probe. Avoid a public plugin/provider framework for a single initial adapter.

## Minimal example behaviour

The example shows runtime kind, model, camera firmware, Sony platform version, Android API level, and the exact probe states. Its package is `org.openpmca.sample`, application label is `Hello Camera`, initial version name is `0.1.0`, and version code is `1`. It clearly labels host/emulator data and unverified camera support.

The activity uses ordinary legacy Android widgets and a programmatic layout. It has a standard `MAIN`/`LAUNCHER` entry, explicit minimum and target SDK `10`, no declared permissions and no broadcast receivers for the proposed read-only operations. It does not allocate `CameraEx` or a display manager.

Handle MENU by finishing normally and returning to the launcher when needed, following the small upstream activity example. Do not copy the PMCADemo WebView process-kill receiver, boot-log receiver, or the tweak application's Wi-Fi suspend receiver. No hardware-key/dial framework is necessary to implement one exit action.

Default operation performs no file writes, network requests, installation, reboot, capture, persistent setting changes, or firmware changes. UI output should contain no serial numbers and no misleading feature-support claims.

## Acceptance criteria

### Core/build lane

- Record the actual toolchain and distinguish cached tools from dependencies that need a first download.
- Prove a host-only JAR build and tests with the selected compiler. Record class-file version and inspect packaged entries.
- Test provider absent, read method absent, successful reads from isolated test doubles, native/read failure, missing per-field data, and generic Android/plain-host fallback.
- Verify that camera firmware remains absent on generic Android and no serial-reading path is called.
- Keep all Android/Sony test doubles out of the produced SDK JAR. No Sony/Android compile-time dependency or stub module is necessary for the reflection-only core.
- State separately whether Android API-signature validation and DEX generation were performed. They must not be implied by host test results.

### Sample/verification lane

- Implement only the minimal read-only consumer using the frozen names above. The core/build lane and sample lane may work in parallel without changing those names.
- Test formatting and the distinction between available, unavailable, and unknown without needing a camera.
- Validate the manifest against accidental permissions or receivers and the sample against forbidden camera/network/storage calls.
- Once Android build tools are actually available, compile against API 10-compatible signatures, assemble the APK, and verify v1/JAR signing for minimum API 10.
- Report missing Android tools as a blocker, not as a successful APK build or installation.
- Write the manual camera smoke-test procedure and an evidence template. All hardware evidence begins as `NOT RUN`.

### Hardware evidence

A later manual test must record model, firmware, observed runtime, APK SHA-256, signature result, launch/exit results, and per-field probe results. Installation and execution are not part of this research task. A successful test covers only its recorded device/firmware and operations.

## File ownership and shared-file discipline

- Coordinator: `docs/research/`, `docs/design/`, and any eventual top-level project overview.
- Core/build lane: root build configuration and `framework/` for this initial implementation, plus `docs/development/build-prerequisites.md` now.
- Sample/verification lane: `sample/`, sample-specific tests for this initial implementation, and `docs/testing/read-only-camera-smoke-test.md` now.

The sample lane must not edit root build or SDK files. The core lane must not edit sample source or coordinator documents. Any necessary shared-file change must be announced to the coordinator rather than applied concurrently.

No commits, pushes, releases, global tool installations, desktop-project changes, or camera operations are included in this initial task. Missing Android packaging tools block APK verification but do not block implementing and testing the core or sample formatting logic.

## Source basis

The companion research brief records directly inspected repository, organization, consumer build, API-variation, launcher, and signing evidence:

- `../research/2026-10-11-pmca-framework-foundations.md`
- https://github.com/ma1co/OpenMemories-Framework
- https://github.com/ma1co/OpenMemories-Tweak/blob/master/app/src/main/java/com/github/ma1co/openmemories/tweak/BaseActivity.java
- https://github.com/ma1co/PMCADemo/blob/master/app/src/main/AndroidManifest.xml
- https://github.com/jonasjuffinger/TimeLapse/blob/master/app/build.gradle
- https://source.android.com/docs/security/features/apksigning/v2
- https://docs.oracle.com/en/java/javase/17/docs/specs/man/javac.html
