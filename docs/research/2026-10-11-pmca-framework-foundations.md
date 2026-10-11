# PMCA framework research brief

Date: 2026-10-11
Status: research and proposed scope, not a hardware-validated SDK or release specification.

## User intent and repository baseline

The user wants a reusable framework for developing applications that run directly on Sony cameras supporting PlayMemories Camera Apps (PMCA). This session owns research, architectural analysis, and coordination. Existing peer sessions are intended to implement the resulting work.

At the start of this research, `OpenPMCA/PMCA-Tweak-Framework` contained only `README.md`, with the heading `PMCA-Tweak-Framework`. There was no framework source, build system, test suite, or project license.

During this session, the user renamed the same GitHub repository to `OpenPMCA/Open-PMCA-Tweak-Framework`. The coordinator renamed its real local directory to `/Users/phucdang/Documents/Open-PMCA-Tweak-Framework` and updated both origin fetch/push URLs to `https://github.com/OpenPMCA/Open-PMCA-Tweak-Framework.git`. Existing tracked and untracked work was preserved. Current work must use the new name and directory; the original baseline description above is historical.

The OpenPMCA organization profile describes this repository as an in-camera Android framework and tweak utilities. It describes the separate `Sony-Open-PMCA` project as the desktop application manager and camera diagnostics tool. The user's request broadens the useful foundation from a single tweak application to a reusable app SDK. The recommended separation is to keep ordinary application APIs in the framework and add persistent configuration-changing tweaks separately, after the read-only foundation is verified.

Sources:

- https://github.com/OpenPMCA/Open-PMCA-Tweak-Framework
- https://github.com/OpenPMCA/.github/blob/main/profile/README.md
- https://github.com/OpenPMCA/Sony-Open-PMCA

## Confirmed ecosystem evidence

`ma1co/sony-pmca-re` states that cameras supporting PMCA can install custom Android applications. Its custom-app documentation points developers to OpenMemories Framework for Sony-specific APIs. This is a basis for feasibility, not a guarantee for every model or firmware.

OpenMemories Framework describes itself as a wrapper around Sony-specific classes and provides dummy implementations to allow development on ordinary Android devices or emulators. Its historical dependency example uses JCenter, JitPack, a moving snapshot, and a `provided` dependency for Sony API stubs. That dependency recipe is evidence of the upstream boundary, not a recommendation to copy obsolete build settings.

Two real application consumers illustrate why compatibility must be feature-specific:

- `jonasjuffinger/TimeLapse` uses OpenMemories Framework. Its README says the author had an a6300 to test. The silent-shutter option does nothing on cameras without that feature. It also documents camera-specific shooting failures.
- `obs1dium/FocusBracket` is a proof-of-concept application using OpenMemories Framework. Its README reports testing only on an A5100 with the kit lens. The repository is archived.

A direct consumer-code check supplies stronger evidence than a README claim: TimeLapse's `ShootActivity.java` explicitly says `setSilentShutterMode` does not exist on all cameras and catches `NoSuchMethodError` around that call. Merely finding a Sony class is therefore insufficient to promise every method is present. A new framework should distinguish provider presence, method availability, and actual hardware-tested feature support.

The same consumer opens `CameraEx` during resume, removes scheduled shooting callbacks during pause, and releases the camera before exit. This supports making resource ownership and cancellation explicit in later camera-control APIs. It does not justify adding capture or display mutations to the first read-only example.

Do not confuse these on-camera applications with smartphone applications that remotely control cameras over Wi-Fi. Similar app names do not imply the same runtime or compatibility.

Sources:

- https://github.com/ma1co/sony-pmca-re
- https://github.com/ma1co/OpenMemories-Framework
- https://github.com/jonasjuffinger/TimeLapse
- https://github.com/jonasjuffinger/TimeLapse/blob/master/app/src/main/java/com/jonasjuffinger/timelapse/ShootActivity.java
- https://github.com/obs1dium/FocusBracket

## Architecture options and recommendation

Two implementation approaches meet the user's on-camera SDK goal:

1. Build a small compatibility facade using audited upstream API signatures and selectively reuse the relevant MIT-licensed wrappers. Publish a standard JAR when no Android resources are required. Keep Sony/Android compile-time signatures and test doubles outside the runtime artifact. This permits a narrowly verified first milestone without carrying all historical demo behavior into the SDK. The cost is a deliberately smaller initial API surface and explicit adapter tests.
2. Maintain a fuller fork of OpenMemories Framework and modernize its build and dependency resolution. This preserves more existing interfaces immediately, but requires reviewing a larger inherited surface and separating obsolete build behavior, dummy implementations, and feature-support promises. It should not be presented as hardware compatibility gained merely by rebuilding old source.

The recommendation is the small-facade approach for the first milestone, with selective upstream reuse rather than a wholesale rewrite of Sony hardware integration. Existing applications show that upstream work is valuable; their model-specific failures show why new support claims require stronger evidence. A resource-bearing Android library can be considered later only when a concrete feature needs it. There is no justification for starting with an AAR, plugin system, arbitrary-script executor, or native firmware subsystem.

## Recommended first milestone

The first milestone should establish a buildable reusable SDK foundation and a read-only example application, rather than firmware modifications or a universal camera-support claim.

The first example should report device/runtime information and explicitly distinguish supported, unavailable, and unknown feature states. It should not alter language restrictions, video limits, thermal limits, firmware, persistent device properties, or execute arbitrary scripts. Camera serial numbers should remain local and should not enter public test evidence by default.

Keep desktop installation and USB transport outside the on-camera framework. Use the existing desktop project for installing the eventual APK. Do not modify that desktop project as part of this research task.

Prefer learning from and reusing the relevant upstream framework boundaries over inventing Sony methods. Reuse must retain applicable attribution and license notices. The new repository's own license is not selected by this brief. Sony runtime libraries and compile-time placeholders must not be mistaken for redistributable production implementations.

The inspected TimeLapse and FocusBracket application build files both set compile, minimum, and target SDK to API 10 and Java source/target compatibility to 1.6. This is direct evidence for a conservative legacy baseline, not proof that every future build tool must be equally old. A replacement toolchain must prove that it produces compatible DEX, resources, and signatures. Exact upstream framework targets, module layout, precise public API, pinned dependencies, and support matrix still require the parallel source verification and installed-toolchain report.

Consumer build evidence:

- https://github.com/jonasjuffinger/TimeLapse/blob/master/app/build.gradle
- https://github.com/obs1dium/FocusBracket/blob/master/app/build.gradle

The first identity-only example does not need to open the camera or allocate a display manager merely to imitate a larger demo. Do not import Wi-Fi, storage, or camera permissions from unrelated examples without a concrete required operation.

Peer analysis and a coordinator source check support a minimal launcher activity with `MAIN` and `LAUNCHER`, no permissions and no manifest receivers for the proposed identity-only behavior. PMCADemo includes networking, capture, installer, boot-logging, and WebView-exit behavior that should not be inherited blindly. OpenMemories-Tweak's base activity extends the ordinary Android Activity, handles MENU by finishing and returning to HOME, and does not perform PMCADemo app-info notification during resume. Its suspend receiver serves its Wi-Fi behavior, which the identity-only example does not have. This source evidence makes the minimal example reasonable; only a later camera smoke test can confirm launcher and exit behavior on a specific model.

Minimal activity and launcher evidence:

- https://github.com/ma1co/OpenMemories-Tweak/blob/master/app/src/main/java/com/github/ma1co/openmemories/tweak/BaseActivity.java
- https://github.com/ma1co/OpenMemories-Tweak/blob/master/app/src/main/AndroidManifest.xml
- https://github.com/ma1co/PMCADemo/blob/master/app/src/main/AndroidManifest.xml

## Verification contract

Track the following evidence separately. Passing an earlier level does not establish a later level.

| Evidence | What it establishes | What it does not establish |
| --- | --- | --- |
| Host-side unit tests | Value handling, capability-state logic, and behavior using test doubles | Actual Sony runtime APIs or on-camera execution |
| Android SDK build | Source and resource compilation, dependency resolution, artifact creation | Installability or working camera features |
| Packaged artifact inspection | SDK contents and absence of unwanted runtime stub classes | Correct Sony runtime behavior |
| Legacy APK signature verification | Signature verification for the specified minimum Android API | Camera launcher policy or successful execution |
| Camera smoke test with model and firmware recorded | Specific tested behavior on that device/firmware | All PMCA cameras, all firmware, or unrelated features |

Android introduced APK Signature Scheme v2 in Android 7.0. Older platforms ignore v2 and verify v1 signatures. Consequently, any eventual application supporting a pre-Android-7 camera runtime needs a v1/JAR signature. If source inspection establishes API 10 as the baseline, signature verification must explicitly use API 10. APK alignment must happen before signing.

Official signing references:

- https://source.android.com/docs/security/features/apksigning/v2
- https://developer.android.com/tools/apksigner

A future manual camera smoke test should cover launch, read-only information, normal exit, repeated launch/exit, and relevant hardware-key behavior. It must record the actual model, firmware, APK identity, observed runtime version, and feature results. No hardware operation has been authorized or performed by this research brief.

## Coordination boundaries

Independent work is split as follows:

- Research coordinator: repository/organization interpretation, upstream evidence synthesis, architecture and public contract, compatibility claims, final review.
- Core/build implementer: installed toolchain assessment and, once the contract is established, root build configuration and SDK implementation.
- Example/verification implementer: example application lifecycle and manifest requirements, example tests and manual verification instructions. No edits to root build or SDK-owned files without coordination.

Initial peer source exploration was read-only. After reporting their findings, peers were assigned independent documentation work: the build lane owns `docs/development/build-prerequisites.md`, and the example/verification lane owns `docs/testing/read-only-camera-smoke-test.md`. The build lane may also verify a self-written, throwaway Java-library build in a new temporary directory without network dependency downloads. These tasks do not authorize product source edits or hardware operations.

The coordinator issued the frozen initial contract and implementation plan and assigned the core/build and sample/verification peers to their implementation tasks. Product edits must use that shared contract and explicit file ownership. No session should commit, push, publish releases, flash firmware, install to a camera, or make destructive device changes during this task.

## Verified integration constraints and retained research limits

The source-research and independent-verification workflow has completed. Its recommendations were evaluated rather than copied wholesale. The coordinator directly checked the minimal activity, Sony read signatures, desktop APK validation, and official Android signing documentation.

The Sony read-only adapter has an immutable source basis in OpenMemories Framework commit `f8df3504d8684433a331ee869e64f67cc8be9730`:

- Model: static `ScalarProperties.getString(String)` with property key `model.name`.
- Sony platform revision: the same method with key `version.platform`. Keep its raw value; do not mistake the Scalar API revision for an Android API level.
- Camera firmware: static `ScalarProperties.getFirmwareVersion()` with no arguments. There is no need to invent a firmware property constant.
- Generic Android metadata comes from Build fields. On Sony cameras `Build.MODEL` can be `ScalarA`, not the actual ILCE/NEX model. The legacy fingerprint is brand `sony`, model `ScalarA`, device `dslr-diadem`.
- Do not call either the Sony serial-property path or `Build.SERIAL`. Generic Android build/OS identifiers must not be returned as camera firmware.

Pinned source:

- https://github.com/ma1co/OpenMemories-Framework/blob/f8df3504d8684433a331ee869e64f67cc8be9730/framework/src/main/java/com/github/ma1co/openmemories/framework/DeviceInfo.java
- https://github.com/ma1co/OpenMemories-Framework/blob/f8df3504d8684433a331ee869e64f67cc8be9730/stubs/src/main/jasmin/com/sony/scalar/sysutil/ScalarProperties.j

The desktop project's inspected APK validator requires a nonempty package name and version name, minimum SDK in the range 1 through 10, and one readable RSA certificate from the v1 signature entries. An example APK should therefore explicitly supply a version and use a legacy-compatible v1 RSA signature. The desktop parser extracts a certificate; that extraction is not a substitute for cryptographic verification by apksigner.

- https://github.com/OpenPMCA/Sony-Open-PMCA/blob/main/pmca/commands/usb.py
- https://github.com/OpenPMCA/Sony-Open-PMCA/blob/main/pmca/apk/__init__.py

A valid v1 signature can coexist with newer signature schemes. The research report's stronger claim that the mere presence of v2/v3 forces rejection is not adopted. Similarly, demo-specific process-kill, app-info notification, APO suppression, and display teardown are not unconditional requirements for an identity-only activity that does not allocate those resources.

The upstream camera table explicitly says its entries are cameras known to run Android, and includes Action Cams and Handycams. It is not an authoritative APK-installation or interactive-app support allowlist. The firmware database's `apps` metadata, SoC family, presence of Scalar classes, installer eligibility, and successful app execution must remain separate evidence. No exact model-count claim or blanket support list is adopted by this SDK proposal.

- https://github.com/ma1co/OpenMemories-Framework/blob/f8df3504d8684433a331ee869e64f67cc8be9730/docs/Cameras.md
- https://github.com/ma1co/Sony-PMCA-RE/issues/60

Remaining evidence boundaries:

These are research gaps, not conclusions:

- Exact upstream Android and Java build targets, and a reproducible replacement for obsolete repositories or moving snapshots.
- Core/build peer reports local OpenJDK 17 can compile Java 7 bytecode, while the default JDK 21 rejects release 7. Neither available compiler accepts Java 6. The peer reports no Android SDK or Android packaging tools and a cached Gradle 9 distribution. The peer completed a self-written, dependency-free offline Java-library probe in `/tmp/pmca-toolchain-test`. The coordinator inspected its build configuration, actual JAR contents, and class-file major version 51. This establishes a viable local host/JAR build path, not an SDK implementation, Android API-signature check, DEX/APK build, or camera compatibility. See `../development/build-prerequisites.md` for the build evidence and limitations.
- The distinction between officially listed PMCA models, custom-app installation reports, and feature-specific runtime verification.
- Sony's official ending notice was located, but direct retrieval of its full dated text was blocked. A developer-index search returned an official-page excerpt and a community issue about the download service, not a complete primary-source statement. Exact sales/download dates and promises about existing installations are therefore not incorporated as verified facts. The on-camera SDK does not depend on a Sony store download step.
- Existing desktop application's catalog and APK-validation contract.

The source research is complete; no camera was tested. The initial implementation contract is recorded in `../design/2026-10-11-read-only-sdk-contract.md`. The example/verification peer has produced the manual protocol at `../testing/read-only-camera-smoke-test.md`; its hardware results are unexecuted.
