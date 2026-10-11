# Initial SDK build and verification record

Date: 2026-10-11

## Resume baseline

The user explicitly resumed building and careful testing after the quota-related pause. The canonical checkout remains `/Users/phucdang/Documents/Open-PMCA-Tweak-Framework` with origin `https://github.com/OpenPMCA/Open-PMCA-Tweak-Framework.git`.

At resume, only coordinator/peer documents and the modified README were present. There was no delivered `framework/` or `sample/` implementation and no implementation ledger marked complete. Prior research and the throwaway Java-library prototype must not be described as a completed SDK.

The coordinator created branch `feat/read-only-sdk` without committing or pushing and resumed the existing peer implementation sessions under their frozen file-ownership boundaries.

## Execution decisions

- Preserve the user-chosen execution method: existing core/build and sample/verification peers implement their lanes; the coordinator performs fresh cross-lane verification and independent review.
- Use a normal development branch, not an unsolicited git worktree. Existing uncommitted documentation is preserved.
- Retain the frozen reflection-only SDK contract. No new Sony stub dependency, camera control, serial-number path, persistent tweak operation, or desktop-project change is added.
- Host tests, static manifest/JAR checks, Android signature/API checks, DEX/APK packaging, and physical-camera evidence remain distinct.
- No commits, pushes, releases, global installs, or camera installation/execution are authorized by this milestone.

## Verification status

The SDK and sample sources are now delivered on `feat/read-only-sdk`, without committing or publishing. The coordinator reran the actual core and sample commands after both owner sessions handed off stable files. Earlier preparation sections below are historical; the current artifact evidence is recorded in the final section.

| Check | Current evidence |
| --- | --- |
| Offline core build and runtime suite | PASS, 17 scenarios including four instance-member subcases, every task rerun |
| Sample formatter suite using the final SDK JAR | PASS, 8 groups; root and different working directory |
| Missing SDK JAR | Expected exit 1 with an error, never a success message |
| Manifest/API/JAR/static boundaries | PASS; frozen public names, helper visibility, no packaged vendor/test classes |
| Supplementary legacy-signature compilation | PASS for all seven production types; limitations below |
| Comprehensive Android API audit and DEX execution | NOT RUN |
| Binary resources/APK assembly/alignment/v1 RSA signing | NOT RUN; Android packaging/signing tools unavailable |
| Camera installation/launch/exit/hardware | NOT RUN; no camera operation performed |
| Independent final implementation review | Completed; one confirmed finding fixed with RED/GREEN regressions and fresh whole-suite verification |

### Delivered sample source-manifest check

The coordinator read the actual activity source and parsed `sample/src/main/AndroidManifest.xml` after the sample peer created them. The source manifest passed checks for the frozen package/version, minimum and target API 10, exactly one activity with MAIN and LAUNCHER in the same intent filter, and zero permissions/receivers. The source activity uses ordinary Android widgets and standard MENU/HOME exit without allocating camera resources.

This is source-level evidence only. The activity was not compiled against Android signatures or run on a camera. The coordinator did not run a root Gradle build while the core peer was actively implementing its test/production files.

### Legacy Android signature inputs

The coordinator obtained the published `com.google.android:android:2.3.3` archive in an isolated temporary directory and checked it against its Maven-published checksum. Its POM identifies platform `android-10`, and the archive contains Activity, Build, and KeyEvent signatures. It contains no annotation-processor service entries and has not been executed or included in the SDK artifact.

This archive omits core `java.lang` signatures such as Object, Class, and Enum. Merely putting it on an ordinary classpath would not establish Android API 10 Java-library compatibility. It can serve a narrower Android-facing sample compilation check, but is not sufficient to claim a full Android bootclasspath audit. No full SDK, DEX/resource compiler, alignment/signing tool, or license acceptance was performed by obtaining this signature input.

### Independent checker preparation

The coordinator prepared an isolated, temporary artifact checker at `/tmp/pmca-independent-verification.idbVFu/check_artifacts.py`. It verifies source-manifest metadata and a single launcher filter, rejects permissions and receivers, checks the SDK JAR for vendor/test classes and Java 7 class-file targets, and rejects several known Android API 10-incompatible library references. These checks do not amount to a comprehensive Android API-signature audit.

The checker was tested against synthetic inputs: a valid static-contract fixture, unexpected camera permission, unexpected receiver, minimum SDK above the agreed value, packaged Sony stub, packaged test fixture, incorrect class-file target, and an AutoCloseable reference. All checker self-tests behaved as expected. These are tests of the checker, not successful SDK, APK, or hardware results.

A separate read-only workflow reviewed the frozen test requirements and highlighted mixed Sony/Android fallback pollution, lookup-versus-invocation failure semantics, independent reader failure, class-initialization/linkage errors, wrong metadata types, and default-classpath contamination. Relevant regression cases were sent to the owning peer lanes without expanding product scope.


## Final host integration evidence

### Lane handoffs and corrections

- `open-pmca-tweak-framework-19` completed the core/build lane. Probe factories are no longer public, and all observations use the fixed diagnostic reasons in the frozen contract. The suite grew from 10 to 16 scenarios, including individually missing readers, failed model reads with successful independent readers, a missing Android SDK field, blocked provider lookup, and public-API visibility.
- `open-pmca-tweak-framework-6a` completed the sample/verification lane. The suite grew from 4 to 8 groups, covering all runtime kinds and availability states. A test-only bridge and isolated fixtures do not enter the SDK JAR or production sample output; sample tests no longer depend on unauthorized public factories. The existing activity, formatter and source manifest required no behavior changes.
- Independent inspection found a further crash: `Class.getMethod` can resolve an unrelated method's unavailable return type and throw `NoClassDefFoundError` before any reader is invoked. The coordinator reproduced exit 1 against the built JAR. The core owner added a failing regression, guarded each reader lookup against `LinkageError`, and retained the discovered provider and independent Android metadata. The coordinator reran the original reproducer: exit 0, `SONY_SCALAR` with the affected reader `UNKNOWN / READ_FAILED`.

The peer reports record RED/GREEN evidence for the fixed diagnostic/API contract and method-lookup crash. Sample runner wiring also failed before its test bridge was compiled and then passed. Initial test-first ordering of code already present at this continuation is not inferred retrospectively.

### Pre-review commands and artifact

The coordinator first ran, on the stable pre-review handoff sources:

```bash
./gradlew --offline --rerun-tasks check :framework:jar
./sample/test-host.sh
```

The core command exited zero and printed `All 16 PmcaRuntimeHostTest scenarios PASSED successfully.` The normal Gradle `test` task is deliberately disabled because the dependency-free `hostTest` runner is the suite; `check` explicitly depends on that runner. The sample command exited zero with all eight formatter groups passing.

The absolute sample script also passed from a separate working directory with explicit JDK 17 and framework-JAR inputs. A deliberately nonexistent `FRAMEWORK_JAR` produced expected exit 1 and `ERROR: Framework JAR not found.`, without a success marker. `bash -n sample/test-host.sh` passed. JDK 17 emitted expected obsolete source/target 7 warnings; there were no test failures in these final commands.

Artifact: `framework/build/libs/framework.jar`.

SHA-256 at the pre-review host handoff (superseded below):

```text
be963e9575944da8876cfe4d1989026e0fd7ba7e7043566e0e54bc5e91a766a7
```

`javap -public` and the runtime API-visibility regression verify the frozen getters, enums and public `PmcaRuntime.inspect()`. Package-private/private constructors, factories and `inspect(ClassLoader)` remain outside the consumer API. The JAR contains only the five SDK types, with class-file major version 51, and no `com/sony/`, `android/`, sample, fixture or host-test implementation classes. The README public-API example compiled and ran against the standalone JAR, reporting `PLAIN_HOST` and absent model metadata.

The independently parsed source manifest has the exact package/version/API metadata, one MAIN/LAUNCHER activity in the same filter, zero permissions, receivers and aliases. Source inspection confirms ordinary widgets, one runtime inspection, and MENU finish/HOME exit without camera allocation. This does not verify physical key behavior.

### Supplementary legacy platform compilation

Two retained signature inputs were inspected without executing their contents or downloading dependencies during this continuation:

- `com.google.android:android:2.3.3`: SHA-1 `949a2bef1ffe62e81af5d8919d17dfcc1309c43c`, matching its retained published checksum; its POM identifies platform `android-10`. It lacks core `java.lang` signatures. Compiling the Android-facing activity/formatter with it on the ordinary classpath passed, but this is only a narrow Android-symbol check.
- A retained complete legacy platform archive at `/tmp/pmca-aosp-api10-signatures.Uw8hKi/android-api10.jar`: SHA-256 `0160896aafd1da1d3fdd9cf3a3dd0d6c9ab7ac98fffeaba8ebf44ccdc2e37356`. Its computed Git blob `ae040e6b525c8ee454c53be80bfca146a32fce7e` matches the retained platform-directory index. It contains core Java and Android signatures and no annotation-processor service entry.

The coordinator compiled all five SDK and both sample production types in a separate output directory using JDK 17 `-source 7 -target 7 -proc:none -bootclasspath <retained-platform-archive>`. All seven compiled. This separate check does not replace the host build's `--release 7` and adds no SDK dependency to production.

Compiler controls exposed an important limit: `java.util.Objects` and `java.nio.file.Path` were rejected against these signatures, but a declaration of `java.lang.AutoCloseable` was accepted even though that class is absent from the archive. The installed compiler's `Symtab.java` calls `synthesizeEmptyInterfaceIfMissing(autoCloseableType)`. Therefore bootclasspath compilation alone is **not** a comprehensive Android API audit. The artifact checker separately scans for known incompatible references including `AutoCloseable`; none were found in the actual production outputs. Neither check establishes DEX or camera-runtime compatibility.

### Remaining blockers and evidence boundaries

No `sdkmanager`, `aapt`, `aapt2`, `d8`, `zipalign`, `apksigner` or `adb` was found. The `dx` command in PATH belongs to Deno, not Android. Retained signature archives are not an installed Android packaging toolchain.

No binary Android manifest/resources, `classes.dex`, APK, alignment result or v1 RSA signature was produced or verified. No camera installation, launch, read, exit, capture or setting change was performed. Hardware evidence remains `NOT RUN` in the manual smoke-test template.

The build is offline on this machine because its Gradle distribution and JDK are already available. A fresh clone with an empty wrapper cache is not guaranteed offline: wrapper bootstrap may download Gradle before Gradle processes `--offline`. No commits, pushes, releases, global installations or desktop-project edits were performed.


## Independent review outcome

The final stable source snapshot was reviewed independently, and a separate verifier challenged each candidate finding. One Important finding survived: invoking a reflected instance member with a null receiver raises `NullPointerException`, which escapes the individual reader and resets the entire snapshot to `DETECTION_FAILURE`. The coordinator independently reproduced this against the built JAR for all four paths: instance `getString`, instance firmware getter, instance Android `MODEL`, and instance `SDK_INT`.

The core owner completed one minimal fix pass with four-path regressions. Each reflected read validates static modifiers and handles the unsupported member locally as `UNKNOWN / READ_FAILED`; discovery of the provider/runtime and successful siblings remain intact. No provider objects are allocated. The reviewed sample required no behavior change. The earlier hash and 16-scenario result identify the pre-fix artifact; current results follow below.

The review explicitly set aside the dependency-free test runner, Java 7 host target without a runtime claim, intentional absence of serial collection, intentional absence of camera/display controls, and expected linkage-error guards. Those remain contract constraints, not unfixed defects. These decisions do not waive the separate Android/API/DEX/APK/hardware evidence gates.


## Final post-review integration closure

The coordinator reran `./gradlew --offline --rerun-tasks check :framework:jar` after the reviewed fix. Exit zero, all **17 core scenarios** passed, including the four non-static-member cases in scenario 17. All 21 actionable Gradle tasks executed freshly. Expected Java 7 source/target warnings were observed; no test failure occurred.

The coordinator reran `./sample/test-host.sh` against this post-review JAR: exit zero, all **eight formatter groups** passed. The same absolute script passed from a separate working directory with explicit JDK/JAR inputs. A nonexistent JAR was again rejected with expected exit 1 and no success marker; shell syntax validation passed.

The independently written four-case reproducer was compiled against the new JAR and rerun. It asserts runtime classification, affected probe availability/reason/null value, and the exact values of successful sibling reads. All four cases now exit zero. The original unrelated-method-return-type linkage reproducer also remains green. The core owner's recorded failing regression before the static-member guards establishes RED/GREEN sensitivity, not merely a post-fix passing test.

Current artifact: `framework/build/libs/framework.jar`.

Current SHA-256:

```text
eca49897274b24842a2fc5153e6381fa8f7b825fbb2d7d10a5e36893cfa8ac34
```

Final artifact checks passed again: exact frozen public API and non-public helpers, five SDK types at class-file major 51, zero vendor/Android/sample/test implementation classes in the JAR, and exact source-manifest metadata with zero permissions/receivers. All seven production types again compiled in the separate legacy boot-signature check and passed the known-forbidden-reference scan. The standalone README consumer still reports a plain host with absent model metadata.

The independent reviewer explicitly reported coverage of 14 source/config/test/spec paths; capturing 50 files for review is not a claim that every wrapper, fixture and document was individually audited. Fixture execution, packaging/static boundaries and compiler/toolchain controls have their separate results above. One Important finding was confirmed and fixed; no other actionable or deferred minor finding was reported. No second review was substituted for the covering regression and full-suite rerun.

The host-tested read-only SDK/sample-source milestone is complete. Comprehensive Android API auditing, DEX/APK/resource assembly, alignment/v1 RSA signing, installation and physical-camera testing remain **NOT RUN**, for the documented toolchain/authorization boundaries. The SDK JAR is not an APK. At the implementation handoff, all changes remained uncommitted on `feat/read-only-sdk`; no push, release, global install, desktop edit or camera operation had been performed. The user subsequently authorized repository publication; release scope and limits are recorded in `../releases/v0.1.0.md`.
