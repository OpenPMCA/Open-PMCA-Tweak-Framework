# Read-Only Camera Smoke Test & Verification Procedure

Date: 2026-10-11

Status: Draft Verification Protocol

Execution Status: **NOT RUN** (Specification only; no hardware execution or device interaction performed)

---

## 1. Overview & Purpose

This document defines the manual hardware smoke test procedure and evidence template for the initial read-only **"Hello Camera" / Diagnostics** sample application.

The sample application is strictly non-destructive: it inspects device identity and platform capabilities without altering system settings, writing to backup/EEPROM partitions, or capturing/driving camera hardware (no `CameraEx` shutter or focus actuation).

### Core Architectural Principle
A strict distinction is maintained between:
1. **Desktop / Host-Side Unit Tests**: Validates business logic, parsing, and test doubles on JVM/CI. Marked exclusively as `DESKTOP PASS - UNVERIFIED ON HARDWARE`.
2. **Provider & Method Presence**: Verifies that Sony Scalar classes and methods exist on the runtime classpath.
3. **Hardware Verification**: Physical execution and manual verification on specific camera models and firmware versions. Passing earlier levels never implies hardware compatibility.

---

## 2. Pre-Requisite & Artifact Validation (Pre-Install)

Before attempting any deployment to a physical camera via USB (using `Sony-PMCA-RE`), the APK artifact must pass offline integrity and packaging checks.

### 2.1 APK Identity & Hash Generation
Compute and record the cryptographic hash of the APK artifact:
```bash
# Record artifact identity
sha256sum path/to/hello-camera-release.apk
# On macOS:
shasum -a 256 path/to/hello-camera-release.apk
```

### 2.2 Binary Packaging & Alignment
The artifact must be a built Android APK, not a ZIP containing Java source or a text manifest. Confirm that it contains binary Android resources/manifest and `classes.dex`. The DEX must contain the production sample and SDK only: exclude every test fixture, test bridge, and any bundled `com.sony.*` or `android.*` implementation class.

Inspect the archive entries and let `aapt` parse the binary manifest/resources (§2.4):
```bash
unzip -l path/to/hello-camera-release.apk
```

Align the APK **before signing**. After signing, check the final artifact without modifying it:
```bash
zipalign -c -v 4 path/to/hello-camera-release.apk
```
An alignment check is not signature verification. Do not realign or otherwise rewrite a signed APK; rebuild, align, and sign in that order if the check fails.

### 2.3 Legacy APK Signature Verification (v1 / JAR Scheme)
Sony PMCA cameras run Android 2.3.7 (Gingerbread, API 10) or Android 4.1.2 (JellyBean, API 16). Android versions prior to 7.0 (Nougat, API 24) cannot parse APK Signature Scheme v2/v3/v4. If an APK lacks a valid v1 JAR signature, the camera package manager rejects it with `INSTALL_PARSE_FAILED_NO_CERTIFICATES`.

Verify the signature using Android SDK's `apksigner`:
```bash
apksigner verify --verbose --print-certs --min-sdk-version 10 path/to/hello-camera-release.apk
```
**Acceptance Criteria**:
* `Verified using v1 scheme (JAR signing): true` must be reported, with verification exit code zero for minimum API 10.
* The signer certificate must report an **RSA** public key; retain the signature-verification output as evidence.
* If signed with v2/v3, v1 must still be explicitly present and valid.
* Missing resource, DEX, alignment, or signing tools are blockers, never evidence of a successful APK.

Official Reference:
* [Android APK Signature Scheme v2](https://source.android.com/docs/security/features/apksigning/v2)

### 2.4 Manifest & Permission Verification
Verify that the package contains zero permissions and zero background receivers using full badging and XML tree inspection:

```bash
# 1. Inspect package identity, launcher activity, and permissions
aapt dump badging path/to/hello-camera-release.apk

# 2. Inspect complete manifest XML hierarchy for Activity intent-filter and absence of receivers
aapt dump xmltree path/to/hello-camera-release.apk AndroidManifest.xml
```

**Acceptance Criteria**:
* **Identity & SDK Metadata**: Package `org.openpmca.sample`, application label `Hello Camera`, version code `1`, version name `0.1.0`, minimum SDK `10`, and target SDK `10`.
* **Zero Permissions**: There must be zero `uses-permission:` lines in the entire `aapt dump badging` output (acceptance requires zero declared permissions across the entire package for this read-only slice). Do not rely on grep exclusion as proof of passage.
* **Launchable Activity**: `launchable-activity:` must be present in the badging output.
* **Manifest Hierarchy**: In `aapt dump xmltree`:
  * Exactly one `<activity>` element declaring an `<intent-filter>` containing `<action android:name="android.intent.action.MAIN">` and `<category android:name="android.intent.category.LAUNCHER">`.
  * **Zero `<receiver>` elements**: The XML tree must contain no broadcast receivers whatsoever.

---

## 3. Hardware Test Setup & Privacy Protocol

### 3.1 Equipment & Setup
1. A chosen Sony PMCA target camera. Record its exact model and firmware; provider discovery or a model-name example is not proof of compatibility.
2. Battery level > 50%.
3. Host computer with `pmca-console` or `pmca-gui` from `Sony-Open-PMCA` / `Sony-PMCA-RE`.
4. USB connection in "Mass Storage" mode.

### 3.2 User Privacy & Zero Serial Collection Policy
* **Architectural Privacy Guarantee**: The SDK and sample application enforce **zero serial number collection by design**.
* Neither `ScalarProperties.PROP_MODEL_SERIAL_CODE` nor `android.os.Build.SERIAL` is queried, read, cached, or displayed in this slice. Eliminating the private-data reading path at the architecture level avoids handling sensitive hardware identifiers altogether, rather than relying on downstream redaction or post-processing sanitization.
* The sample application UI and runtime snapshot models must contain no serial number field.
* Verification includes checking that no serial query path is invoked during runtime inspection.

---

## 4. Step-by-Step Manual Smoke Test Procedure

| Step | Action | Expected Result | Failure Criteria |
|---|---|---|---|
| **1. Install** | Install APK via `pmca-console install -f hello-camera-release.apk`. | Installation completes successfully without signature or parse error. | Camera reports installation error or `INSTALL_PARSE_FAILED_NO_CERTIFICATES`. |
| **2. Cold Launch** | On the camera, navigate to Menu -> Application -> "Hello Camera". Press Enter. | Application launches into full-screen activity without Force Close (FC). | Black screen hang, immediate crash to camera liveview, or Force Close dialog. |
| **3. Read-Only Information** | Inspect displayed identity fields: RuntimeKind, Model Name, Camera Firmware, Sony Platform Version, Android API Level, and all provider/reader probe states and reasons. | Data renders clearly with explicit RuntimeKind (`SONY_SCALAR` or `GENERIC_ANDROID`/`PLAIN_HOST`). Unread or absent fields display `Unknown [UNAVAILABLE/UNKNOWN] (reason)` without crashing. `Hardware verification: NOT RUN` remains displayed, even on a physical camera; external evidence is recorded below. | Application throws `NoClassDefFoundError`, substitutes Android OS release for camera firmware, or crashes on unsupported fields. |
| **4. Clean Exit (MENU / HOME)** | Press the camera's physical `MENU` button. | Application cleanly terminates; camera returns immediately to standard Shooting Liveview or Application Launcher via standard Android `KEYCODE_MENU` handling. | Camera freezes, key input ignored, or camera reboots. |
| **5. Repeated Launch/Exit Stress** | Launch and exit the application consecutively 5 times (Cold launch -> Render -> Menu exit). | All 5 cycles succeed smoothly without memory exhaustion or lag. | App fails to launch on subsequent attempts, leaks native resources, or camera crashes. |

---

## 5. Three-Tier Capability Matrix

When reporting runtime probes and property queries, the SDK and sample application strictly distinguish probe status from execution environment (`RuntimeKind`):

### 5.1 Probe Status Tiers
| Status Tier | Definition | Example / Trigger |
|---|---|---|
| **`AVAILABLE`** | A provider/read symbol was discovered, or a field reader returned a non-null value. Neither establishes physical-camera compatibility. | Provider: `CLASS_PRESENT`; reader: `METHOD_PRESENT`; `getString("model.name")` returned a model: `VALUE_READ`. |
| **`UNAVAILABLE`** | A class, method, or field is known absent, or the property is not applicable to this runtime. | `CLASS_ABSENT`, `METHOD_ABSENT`, `FIELD_ABSENT`, or `NOT_APPLICABLE`. |
| **`UNKNOWN`** | Lookup was blocked/inconclusive, a read failed, or a field returned null/empty. The symbol need not have been discovered first. | `READ_FAILED` for blocked lookup/native failure, or `NO_VALUE` for null/empty data. A failed field read does not invalidate a discovered reader. |

### 5.2 RuntimeKind vs. Hardware Evidence Separation
* **`RuntimeKind`**: The execution environment is exposed as an orthogonal classification: `SONY_SCALAR` (Sony ScalarProperties class discovered; may be test double on host or physical camera hardware), `GENERIC_ANDROID` (generic Android Build discovered), `PLAIN_HOST` (plain desktop JVM without Android Build or Scalar), or `DETECTION_FAILURE` (blocked or inconclusive environment/provider detection).
* **Host/Emulator is not automatically `UNKNOWN`**: On desktop or emulator, a mock provider or probe can be evaluated as `AVAILABLE` within its test double. Probes are not conflated with the runtime environment.
* **`HARDWARE_VERIFIED` is an External Evidence Claim**: Passing host unit tests or finding available symbols does **not** establish hardware compatibility. The sample UI displays `Hardware verification: NOT RUN` by default. Actual hardware support is recorded strictly as an external claim in the Hardware Evidence Report (§6).

---

## 6. Hardware Evidence Report Template

Copy this section into individual test run submissions. **All fields default to `NOT RUN`.**

```markdown
### Hardware Verification Report

- **Run Date**: NOT RUN
- **Tester / Environment**: NOT RUN
- **APK Filename**: NOT RUN
- **APK Version Code / Name**: NOT RUN
- **Git Commit SHA**: NOT RUN
- **APK SHA-256**: NOT RUN
- **Binary Resources / Manifest / DEX Verified**: NOT RUN
- **Alignment Verified (Aligned Before Signing)**: NOT RUN
- **v1 RSA Signature Verified for Minimum API 10**: NOT RUN
- **Artifact Verification Output / Exit Codes**: NOT RUN

#### Target Hardware Information
- **Camera Model**: NOT RUN (e.g., ILCE-6000)
- **Camera Firmware Version**: NOT RUN (e.g., 3.20)
- **Scalar Platform Version**: NOT RUN (e.g., 2.0)
- **Android API Level**: NOT RUN (e.g., 10)
- **Observed RuntimeKind**: NOT RUN
- **Serial Number**: NONE (Not queried by design; zero collection)

#### Smoke Test Execution Matrix
| Check | Status (PASS / FAIL / NOT RUN) | Observed Behavior / Notes |
|---|---|---|
| 1. Installation via PMCA USB | NOT RUN | |
| 2. Cold Launch to UI | NOT RUN | |
| 3. Read-Only Identity Render | NOT RUN | |
| 4. Clean Menu / Home Exit | NOT RUN | |
| 5. Repeated Launch/Exit (5x) | NOT RUN | |

#### Capability Verification
| Probe / Field | Availability | Value Observed | Fixed Reason | Hardware Evidence Verdict |
|---|---|---|---|---|
| Scalar Provider | NOT RUN | NOT RUN | NOT RUN | NOT RUN |
| Scalar String Reader | NOT RUN | NOT RUN | NOT RUN | NOT RUN |
| Scalar Firmware Reader | NOT RUN | NOT RUN | NOT RUN | NOT RUN |
| Model Name | NOT RUN | NOT RUN | NOT RUN | NOT RUN |
| Camera Firmware Version | NOT RUN | NOT RUN | NOT RUN | NOT RUN |
| Sony Platform Version | NOT RUN | NOT RUN | NOT RUN | NOT RUN |
| Android API Level | NOT RUN | NOT RUN | NOT RUN | NOT RUN |

Capture and active-display operations are outside this slice and must not be attempted as part of this read-only smoke test. A passing report covers only the recorded camera, firmware, APK, and read-only operations.

- **Overall Smoke Test Verdict**: NOT RUN
```

---

## 7. Appendix: Upstream Source Evidence for Exclusions

This appendix records the exact source code evidence justifying the exclusion of permissions, receivers, and background subsystems from the initial read-only sample:

1. **Exclusion of `ExitCompletedReceiver`**:
   * Upstream file: `ma1co/PMCADemo`: [`app/src/main/AndroidManifest.xml:11-16`](https://github.com/ma1co/PMCADemo/blob/master/app/src/main/AndroidManifest.xml#L11-L16)
   * Implementation: `ma1co/PMCADemo`: [`app/src/main/java/com/github/ma1co/pmcademo/app/ExitCompletedReceiver.java:8-15`](https://github.com/ma1co/PMCADemo/blob/master/app/src/main/java/com/github/ma1co/pmcademo/app/ExitCompletedReceiver.java#L8-L15)
   * Evidence: Ma1co explicitly commented: `// KILL_MYSELF_TO_RELEASE_WEBVIEW_DB: Kill process to release WebView when app is terminated (this hack is used by SNSDirect, too)`.
   * Conclusion: This is an inherited workaround for WebView SQLite lockups. Pure native non-WebView applications do not need it. Neither `jonasjuffinger/TimeLapse` nor `obs1dium/FocusBracket` nor `OpenMemories-Tweak` declares it.

2. **Exclusion of `BootCompletedReceiver`**:
   * Upstream file: `ma1co/PMCADemo`: [`app/src/main/AndroidManifest.xml:17-22`](https://github.com/ma1co/PMCADemo/blob/master/app/src/main/AndroidManifest.xml#L17-L22)
   * Implementation: `ma1co/PMCADemo`: [`app/src/main/java/com/github/ma1co/pmcademo/app/BootCompletedReceiver.java:8-12`](https://github.com/ma1co/PMCADemo/blob/master/app/src/main/java/com/github/ma1co/pmcademo/app/BootCompletedReceiver.java#L8-L12)
   * Evidence: Implementation only logs `Boot completed`. It serves no function for on-demand launched PMCA applications.

3. **Exclusion of `SyncAndroidService.SUSPEND`**:
   * Upstream file: `ma1co/OpenMemories-Tweak`: [`app/src/main/AndroidManifest.xml:21-26`](https://github.com/ma1co/OpenMemories-Tweak/blob/master/app/src/main/AndroidManifest.xml#L21-L26)
   * Implementation: `ma1co/OpenMemories-Tweak`: [`app/src/main/java/com/github/ma1co/openmemories/tweak/DisableWiFiOnSuspend.java:10-13`](https://github.com/ma1co/OpenMemories-Tweak/blob/master/app/src/main/java/com/github/ma1co/openmemories/tweak/DisableWiFiOnSuspend.java#L10-L13)
   * Evidence: Exclusively invokes `WifiManager.setWifiEnabled(false)` to preserve battery when the tweak application enables Wi-Fi. Irrelevant for offline read-only sample.

4. **Exclusion of Permissions (`CAMERA`, `WRITE_EXTERNAL_STORAGE`, `ACCESS_WIFI_STATE`)**:
   * Upstream file: `ma1co/PMCADemo`: [`app/src/main/AndroidManifest.xml:4-8`](https://github.com/ma1co/PMCADemo/blob/master/app/src/main/AndroidManifest.xml#L4-L8)
   * Evidence: Declared solely because PMCADemo bundles `WifiActivity`, `CameraActivity`, and `InstallActivity`. `PropertyActivity` ([`PropertyActivity.java:28-42`](https://github.com/ma1co/PMCADemo/blob/master/app/src/main/java/com/github/ma1co/pmcademo/app/PropertyActivity.java#L28-L42)) accesses device properties without requiring any of these permissions.

5. **Proven Clean Exit Mechanism**:
   * Upstream file: `ma1co/OpenMemories-Tweak`: [`app/src/main/java/com/github/ma1co/openmemories/tweak/BaseActivity.java:18-24`](https://github.com/ma1co/OpenMemories-Tweak/blob/master/app/src/main/java/com/github/ma1co/openmemories/tweak/BaseActivity.java#L18-L24)
   * Implementation: Intercepts `KeyEvent.KEYCODE_MENU`, calls `finish()` and launches `Intent.ACTION_MAIN` with `Intent.CATEGORY_HOME` and `Intent.FLAG_ACTIVITY_NEW_TASK`.
   * Alternative upstream file: `jonasjuffinger/TimeLapse`: [`app/src/main/java/com/jonasjuffinger/timelapse/SettingsActivity.java:397-401`](https://github.com/jonasjuffinger/TimeLapse/blob/master/app/src/main/java/com/jonasjuffinger/timelapse/SettingsActivity.java#L397-L401) overrides `onMenuKeyUp()` to trigger `onBackPressed()`.
