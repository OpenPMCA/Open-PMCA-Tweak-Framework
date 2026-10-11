# Open-PMCA-Tweak-Framework

A reusable application-development foundation for Sony cameras that support PlayMemories Camera Apps. Applications run on the camera's Android subsystem, not on a desktop or smartphone.

The separate [Sony-Open-PMCA](https://github.com/OpenPMCA/Sony-Open-PMCA) project handles desktop app management and installation. This repository does not duplicate its USB transport or change that project.

## Initial development milestone

The first slice provides a read-only runtime/device-information SDK and a small `Hello Camera` reference application source. The core is a Java library; the Android activity and its pure-Java diagnostic formatter live in `sample/` and consume the frozen SDK contract.

The SDK returns immutable probes for:

- Sony Scalar provider and read-method presence.
- Model, camera firmware, Sony platform revision, and Android API level.
- Explicit `AVAILABLE`, `UNAVAILABLE`, or `UNKNOWN` state and a fixed diagnostic reason.
- Independent runtime classification for Sony Scalar, generic Android, a plain Java host, or inconclusive detection.

Provider presence is not proof that a device can install or run a custom application. Successful host tests are not physical-camera verification.

This slice does not read serial numbers, allocate camera hardware, change settings, flash firmware, execute arbitrary scripts, access the network, or write device storage. The reference application's source manifest declares one launcher activity, minimum and target API 10, and no permissions or broadcast receivers.

## Release v0.1.0

The [v0.1.0 release](https://github.com/OpenPMCA/Open-PMCA-Tweak-Framework/releases/tag/v0.1.0) is a **host-tested SDK preview**, not an installable camera application. It provides `framework.jar`, `SHA256SUMS`, and the tagged framework/sample source. See the [release notes](docs/releases/v0.1.0.md) for the exact scope and verification limits.

The tag identifies the release source independently of the repository's default branch. Check out `v0.1.0` to reproduce this version. No APK, DEX, Android-runtime compatibility, signature or physical-camera test is claimed. A project license has not been selected; this release does not assign a new license.

## Developer entry points

- [Verified ecosystem research and architecture trade-offs](docs/research/2026-10-11-pmca-framework-foundations.md).
- [Exact initial SDK and sample contract](docs/design/2026-10-11-read-only-sdk-contract.md).
- [Full-framework development direction and feature boundaries](docs/design/framework-development-roadmap.md).
- [Implementation plan and file ownership](docs/superpowers/plans/2026-10-11-read-only-sdk.md).
- [Local build prerequisites and verification boundaries](docs/development/build-prerequisites.md).
- [Sample source and offline formatter tests](sample/README.md).
- [Build and verification evidence](docs/research/2026-10-11-build-verification.md).
- [Manual read-only camera smoke-test protocol](docs/testing/read-only-camera-smoke-test.md).

## Build and test on the host

Prerequisites: an installed JDK 17 compiler and the pinned Gradle 9.0.0 wrapper distribution. Host tests need no Android SDK, Sony stubs, or external test library.

```bash
./gradlew --offline :framework:check :framework:jar
./sample/test-host.sh
```

The SDK artifact is `framework/build/libs/framework.jar`. The framework `check` task runs the dependency-free runtime tests, including isolated Sony/Android test doubles. The sample script compiles and tests the formatter against that actual JAR; it deliberately does **not** compile the Android activity or assemble an APK. Root Gradle configuration includes only the framework module.

Offline execution requires the Gradle distribution and JDK to be available locally already. On a cold cache, the wrapper may download Gradle before the `--offline` option reaches Gradle itself.

The host compiler emits Java 7 class files (major version 51). Java 6-era source idioms and API 10-compatible library calls remain required for the intended legacy runtime; `--release 7` checks Java SE signatures, not Android API 10 compatibility.

## Use the SDK

Add the framework JAR to the consuming application's dependencies and inspect a snapshot:

```java
import org.openpmca.framework.PmcaRuntime;
import org.openpmca.framework.Probe;
import org.openpmca.framework.RuntimeSnapshot;

RuntimeSnapshot snapshot = PmcaRuntime.inspect();
Probe<String> model = snapshot.getModel();
System.out.println("Runtime: " + snapshot.getRuntimeKind());
System.out.println("Model: " + (model.getValue() != null ? model.getValue() : "Unknown"));
System.out.println("Model probe: " + model.getAvailability() + " / " + model.getReason());
```

A plain JVM reports `PLAIN_HOST`; generic Android supplies Android metadata without substituting it for Sony camera firmware. `SONY_SCALAR` records discovery of the vendor provider, not hardware compatibility. Unknown and unavailable probes have no value; use their status and reason rather than guessing a fallback.

## Verification boundaries

The local host currently lacks Android SDK resource, DEX, alignment, and APK-signing tools. A host JAR build does not establish Android API compatibility, APK installability, or camera execution. APK and physical-camera results remain unverified until those separate checks are actually performed.

## Compatibility and provenance

The inspected upstream examples use minimum and target Android SDK API 10. Older camera runtimes require a valid v1/JAR APK signature. Valid v1 signatures can coexist with newer schemes; a v2-only signature is not sufficient.

Sony firmware databases and tables of devices that contain Android are not interactive-app support allowlists. Compatibility must be recorded by actual model, firmware, runtime, and tested operation. No universal Sony-camera support claim is made here.

Research references include [OpenMemories Framework](https://github.com/ma1co/OpenMemories-Framework), [PMCADemo](https://github.com/ma1co/PMCADemo), and [OpenMemories Tweak](https://github.com/ma1co/OpenMemories-Tweak). Their licenses and notices must be retained if source is reused. This repository has not selected its own project license.

OpenPMCA is independent and is not affiliated with or endorsed by Sony.
