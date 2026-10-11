# Framework development boundaries and roadmap

This repository is intended to become a reusable SDK for applications running on Sony PlayMemories Camera Apps cameras, not merely a device-information application. The initial read-only slice establishes verified runtime boundaries and a developer feedback loop. It is not the complete camera SDK.

## Product separation

- `Sony-Open-PMCA` remains the desktop app installer, app manager, USB transport, and desktop diagnostics project.
- `Open-PMCA-Tweak-Framework` owns the on-camera development SDK, examples, and feature-specific compatibility evidence.
- Persistent camera tweaks are optional features above the application SDK. They must not be required for ordinary application development or enabled by a read-only diagnostic example.

## Milestone order and proof requirements

| Milestone | Deliverable | Evidence before claiming completion |
| --- | --- | --- |
| Read-only foundation | Immutable runtime/property probes, host-tested Java JAR, minimal reference activity | Host tests, artifact inspection, manifest/source checks. APK and hardware results remain separate. |
| Android build and first camera smoke test | Reproducible binary APK and a recorded manual test on a PMCA app host | Android API-signature check, DEX/resource packaging, v1 RSA signature validation, model/firmware/APK-specific launch and exit evidence. |
| Application input and lifecycle | Reusable hardware-key/dial handling and explicit resource ownership | Source-backed scan-code behavior, cancellation/release tests, and per-body runtime observations. No unconditional self-kill, sleep suppression, or receiver copied from a larger demo. |
| Camera capture and live view | Small camera-session and surface APIs using the verified Sony runtime | Open/release/cancel behavior, parameter/method feature probes, hardware-specific capture tests, normal exit and recovery evidence. |
| Optional tweak utilities | Explicit user-triggered operations separate from read-only application APIs | Exact property/firmware support evidence, validation, recoverability/restore testing, clear before/after state. No automatic camera modifications. |

Only the first milestone is under implementation in the current task. Later milestones describe the direction needed to meet the user's broader framework goal. They are not promises of already implemented features, universal support, or permission to operate a camera now.

## Compatibility model

Record the actual model, firmware, observed Android runtime, Scalar API revision, APK identity, and tested operation. Keep these facts separate:

1. Firmware metadata indicates an Android or Scalar component exists.
2. The runtime exposes a class or method.
3. The camera has a working application installer and launcher.
4. A particular operation succeeded on that model and firmware.

Neither a SoC family nor a table labeled "known to run Android" establishes all four. A mock implementation can establish host logic behavior only. It cannot validate Sony camera support.

## Distribution and provenance

A locally built application should remain independent of a moving snapshot dependency or an online Sony app-store download step. The existing desktop installer already owns the relevant local APK validation and USB deployment boundary. This task does not duplicate it or redistribute Sony application binaries.

Primary references are recorded in the [research brief](../research/2026-10-11-pmca-framework-foundations.md). Source reuse must retain upstream licenses and notices. The repository's own license and any public release are outside this initial task.

## Current execution contracts

- [Read-only SDK contract](2026-10-11-read-only-sdk-contract.md).
- [Implementation plan](../superpowers/plans/2026-10-11-read-only-sdk.md).
- [Build prerequisites](../development/build-prerequisites.md).
- [Manual camera test protocol](../testing/read-only-camera-smoke-test.md).
