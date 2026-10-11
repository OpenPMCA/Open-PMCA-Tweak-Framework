# PMCA Tweak Framework: Build Prerequisites & Toolchain Verification

Date: 2026-10-11
Author: Core / Build Implementer Session (`pmca-tweak-framework-be`)
Status: Host verification report and technical prerequisites baseline. Does not modify product source or root build files.

---

## 1. Mục đích và Phạm vi

Tài liệu này xác minh thực tế trạng thái toolchain khả dụng trên máy phát triển, kết quả kiểm chứng prototype build offline, và các nguyên tắc kỹ thuật ràng buộc cho việc biên dịch **PMCA Tweak Framework** (hướng tới môi trường máy ảnh Sony PMCA chạy Android 2.3.7 Gingerbread, API level 10).

Tài liệu tách biệt rành mạch giữa:
1. **Kiểm chứng Compiler/JVM trên máy chủ (Host)**.
2. **Khả năng chuyển đổi DEX (Dalvik Executable)**.
3. **Tính tương thích runtime trên Dalvik VM của máy ảnh**.
4. **Quy chuẩn ký gói APK (v1 JAR signing vs v2 scheme)**.

---

## 2. Bằng chứng Khảo sát Toolchain trên Máy Phát triển (Host Evidence)

### 2.1. Môi trường Hệ điều hành
- **Hệ điều hành**: Darwin 27.2.0 (macOS, kiến trúc `arm64` - Apple Silicon).
- **Trạng thái Repo**: `/Users/phucdang/Documents/Open-PMCA-Tweak-Framework` (remote `OpenPMCA/Open-PMCA-Tweak-Framework`, git branch `main`).

### 2.2. Java Development Kit (JDK)
Kết quả kiểm tra qua `/usr/libexec/java_home -V`:
1. **OpenJDK 21.0.12.1 arm64** (mặc định tại `/opt/homebrew/opt/openjdk@21`):
   - Lệnh: `javac --release 7 Test.java`
   - Kết quả: `error: release version 7 not supported`.
   - Kết luận: JDK 21 đã gỡ bỏ hoàn toàn target Java 7, không thể sử dụng trực tiếp để build bytecode cho Dalvik API 10.
2. **OpenJDK 17.0.20.1 arm64** (khả dụng tại `/opt/homebrew/Cellar/openjdk@17/17.0.20.1/libexec/openjdk.jdk/Contents/Home`):
   - Lệnh: `javac --release 7 Test.java`
   - Kết quả: Thành công. Kiểm tra qua `javap -v Test.class` xác nhận `major version: 51` (Java 7).
   - Lệnh: `javac --release 6 Test.java` / `javac -source 6 -target 6`
   - Kết quả: `error: Source option 6 is no longer supported. Use 7 or later.` (JDK 17 không còn hỗ trợ target 6).
   - Kết luận: **OpenJDK 17 là compiler khả dụng duy nhất trên máy hiện tại có thể sinh bytecode tương thích Dalvik mà không cần cài đặt thêm JDK cũ**.
3. **Cảnh báo quan trọng về Toolchain**: Tuyệt đối **không hardcode** đường dẫn Homebrew Cellar vào file build cấu hình chung. Phải dùng cơ chế Gradle Java Toolchain (`JavaLanguageVersion.of(17)`) hoặc nhận diện thông qua biến môi trường tiêu chuẩn `JAVA_HOME`.

### 2.3. Android SDK & Build Tools
- `ANDROID_HOME` & `ANDROID_SDK_ROOT`: Chưa được thiết lập trong môi trường.
- Thư mục `~/Library/Android/sdk`: **Không tồn tại**.
- Android CLI & Build-tools: `sdkmanager`, `aapt`, `aapt2`, `d8`, `apksigner`, `zipalign`, `adb` **hoàn toàn chưa có trên máy**.
- Lưu ý nhầm lẫn: Binary `/opt/homebrew/bin/dx` trên máy thực chất là alias thực thi của Deno (`deno x`), không phải công cụ Dalvik `dx` của Android.

### 2.4. Gradle Build System
- Binary `gradle` trong PATH: Chưa có.
- Gradle Wrapper Cache: Có sẵn bản giải nén tại `~/.gradle/wrapper/dists/gradle-9.0.0-bin/.../bin/gradle`.
- Daemon Gradle 9.0.0 khởi chạy thành công trên JVM 21, đồng thời có cơ chế tự động dò tìm và fork worker sang OpenJDK 17 khi cấu hình Java Toolchain.

---

## 3. Kiểm chứng Prototype Offline (`/tmp/pmca-toolchain-test`)

Để chứng minh khả năng build độc lập không phụ thuộc mạng, một project throwaway dạng `java-library` tối thiểu đã được tạo và kiểm thử tại `/tmp/pmca-toolchain-test`.

### 3.1. Cấu hình Prototype
**`settings.gradle`**:
```groovy
rootProject.name = 'pmca-toolchain-test'
```

**`build.gradle`**:
```groovy
plugins {
    id 'java-library'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.withType(JavaCompile).configureEach {
    options.compilerArgs += ['--release', '7']
}
```

**Source**: `src/main/java/com/openpmca/test/TestHello.java` (mã tự viết, không copy upstream).

### 3.2. Lệnh Thực thi & Kết quả Bằng chứng
```bash
cd /tmp/pmca-toolchain-test
~/.gradle/wrapper/dists/gradle-9.0.0-bin/.../bin/gradle build --offline --info
```

**Bằng chứng ghi nhận**:
1. Gradle 9 tự động phát hiện OpenJDK 17 qua `/usr/libexec/java_home -V`.
2. Khởi tạo `Gradle Worker Daemon` chạy trên OpenJDK 17 (`/opt/homebrew/Cellar/openjdk@17/.../bin/java`).
3. Thực thi hoàn toàn ở chế độ `--offline`, không tải thêm bất kỳ package nào từ Internet.
4. Thời gian build: `BUILD SUCCESSFUL in 3s` (2 actionable tasks: `compileJava`, `jar`).

### 3.3. Kiểm tra Artifact Sau Build
1. **Kiểm tra Bytecode**:
   ```bash
   javap -v build/classes/java/main/com/openpmca/test/TestHello.class | grep "major version"
   ```
   -> Kết quả: `major version: 51` (Java 7).
2. **Kiểm tra Cấu trúc File JAR**:
   ```bash
   jar tf build/libs/pmca-toolchain-test.jar
   ```
   -> Kết quả:
   ```
   META-INF/
   META-INF/MANIFEST.MF
   com/
   com/openpmca/
   com/openpmca/test/
   com/openpmca/test/TestHello.class
   ```
   -> Xác nhận: **Hoàn toàn sạch (Clean)**, không chứa bất kỳ stub nào của Sony (`com/sony/`) hay Android (`com/android/`).

---

## 4. Các Ranh giới Kỹ thuật & Cảnh báo Quan trọng (Crucial Non-Claims)

### 4.1. "Java 7 Compiler Pass" KHÔNG tương đương "Android API 10 Runtime Compatibility"
- **Ranh giới Cú pháp vs Thư viện Nền tảng**: OpenJDK 17 compile thành công bytecode `major version 51` với cờ `--release 7` chỉ chứng minh cấu trúc classfile tương thích cú pháp ngữ nghĩa Java 7. Tuy nhiên, theo tài liệu chính thức của Oracle javac ([Oracle JDK 17 javac specs](https://docs.oracle.com/en/java/javase/17/docs/specs/man/javac.html)), `--release 7` biên dịch đối chiếu với **Java SE 7 platform APIs** (thông qua database `ct.sym` nội bộ của JDK), **KHÔNG PHẢI Android API 10 APIs**.
- **Xung đột Chiến lược: `--release 7` vs Bootclasspath**:
  - Tùy chọn `--release` và các cờ platform-override (`--boot-class-path`, `-bootclasspath`, `--system`) **hoàn toàn loại trừ lẫn nhau**. Thử nghiệm trực tiếp với javac 17 cho kết quả:
    `error: option --boot-class-path cannot be used together with --release`
  - Do đó, không thể cấu hình đồng thời `options.release = 7` và custom `bootclasspath`.
- **Giới hạn của `compileClasspath` / `compileOnly`**:
  - Việc đưa `android.jar` hoặc `com.google.android:android:2.3.3` vào `compileClasspath` thông thường (hoặc cấu hình `compileOnly`) **không thay thế hay ghi đè các platform classes của JDK** (`java.lang.*`, `java.util.*`, v.v.).
  - Khi dùng `--release 7`, javac vẫn ưu tiên signature của Java SE 7. Vì vậy, việc chỉ khai báo `compileOnly` **chưa đảm bảo** ngăn chặn lập trình viên vô tình gọi các API của Java 7 stdlib.
- **Rào cản Runtime thực tế trên Camera (Android 2.3.7, API 10)**:
  - Dalvik VM trên Android 2.3.7 **hoàn toàn không có các lớp và phương thức của Java 7 stdlib**:
    - `java.lang.AutoCloseable`: Chỉ có từ Android API 19 (KitKat). Gọi AutoCloseable hoặc try-with-resources không desugar sẽ gây `NoClassDefFoundError`.
    - `java.nio.file.*`: Chỉ có từ Android API 26 (Oreo).
    - `java.util.Objects`: Chỉ có từ Android API 19.
- **Kỷ luật Mã nguồn & Giải pháp Kiểm chứng**:
  1. **Source 1.6 idioms là kỷ luật viết code, không phải compiler flag**: Không thể đặt cờ compiler `sourceCompatibility = 1.6` trên JDK 17 vì JDK 17 từ chối target 6 (`error: Source option 6 is no longer supported`). "Java 6 idioms" (không try-with-resources, không diamond operator, không gọi Java 7 stdlib) là kỷ luật tác giả mã nguồn phải tuân thủ nghiêm ngặt.
  2. **Audit Android API 10 chưa được verify đầy đủ chỉ bằng cờ compile**: Dùng `--release 7` là giải pháp host/JAR probe hiện tại. Thử nghiệm thực tế với JDK 17 cho thấy ngay cả khi dùng `-source 7 -target 7 -bootclasspath <api10.jar>`, javac vẫn tự động tổng hợp interface `java.lang.AutoCloseable` nếu thiếu (`Symtab.java:594 synthesizeEmptyInterfaceIfMissing(autoCloseableType)`). Trong khi `Objects` hay `java.nio.file.*` bị chặn đúng, reference tới `AutoCloseable` vẫn có thể sinh vào bytecode. Do đó, bootclasspath compile đơn lẻ không bảo đảm ngăn chặn triệt để mọi API mới; supplementary check và bytecode reference scanning vẫn là yêu cầu bắt buộc đối với artifact cuối cùng. Không đưa ra nhận định chưa được kiểm chứng rằng `compileOnly` hay `bootclasspath` đã đủ để bảo đảm tính tương thích runtime.

### 4.2. Không xác nhận khả năng chạy Dalvik / DEX & Giới hạn của DEX Compiler
- Việc tạo ra file `.class` hoặc `.jar` **chưa phải là bằng chứng** ứng dụng chạy được trên máy ảnh Sony.
- **Quy trình Đóng gói APK Hoàn chỉnh**:
  - Công cụ `d8` (hoặc standalone `r8.jar`) **chỉ chuyển đổi class bytecode sang `classes.dex`**, nó **không tự tạo ra file APK hoàn chỉnh**.
  - Một APK hoàn chỉnh có thể cài đặt được trên Android đòi hỏi:
    1. Android platform jar và resource definitions (`android.jar`).
    2. Trình biên dịch tài nguyên (`aapt` hoặc `aapt2`) để compile `AndroidManifest.xml` sang định dạng binary XML và đóng gói tài nguyên vào container APK.
    3. Trình biên dịch Dalvik bytecode (`d8` hoặc `dx`) để sinh `classes.dex` (format 035, `--min-api 10`) đưa vào APK.
    4. Công cụ căn chỉnh 4-byte (`zipalign`).
    5. Công cụ ký chữ ký số (`apksigner`).
  - Hiện tại máy chủ hoàn toàn **chưa có các công cụ Android SDK trên**, do đó việc chuyển đổi DEX và đóng gói APK được ghi nhận rõ là **CHƯA ĐƯỢC KIỂM CHỨNG (NOT VERIFIED)**.

### 4.3. Ràng buộc Ký APK (v1 JAR Signature Constraint & Coexistence)
- Căn cứ tài liệu bảo mật chính thức của Android ([APK Signing Reference](https://source.android.com/docs/security/features/apksigning/v2) và [apksigner docs](https://developer.android.com/tools/apksigner)):
  - **Bắt buộc có v1 signature**: Android 2.3.7 (API 10) trên camera Sony chỉ đọc và xác minh **Signature Scheme v1 (JAR signing)** nằm trong thư mục `META-INF/`.
  - **Khả năng cùng tồn tại (Coexistence)**: Chữ ký v2/v3 hoàn toàn có thể **cùng tồn tại** song song với chữ ký v1 hợp lệ. Trên Android 2.3.7, Dalvik/PackageParser chỉ đọc `META-INF/` và bỏ qua khối APK Signing Block; do đó sự xuất hiện của khối v2 không gây lỗi, miễn là chữ ký v1 toàn vẹn. Camera chỉ từ chối cài đặt (với lỗi `INSTALL_PARSE_FAILED_NO_CERTIFICATES`) khi APK **chỉ ký v2-only** (thiếu v1) hoặc chữ ký v1 bị hỏng.
  - **Quy tắc Zipalign Nhất quán**: Theo tài liệu chuẩn của `apksigner`, **`zipalign` BẮT BUỘC phải thực hiện TRƯỚC KHI ký** (`zipalign` trước, sau đó mới dùng `apksigner sign`). Mọi can thiệp vào file APK sau khi ký đều làm hỏng chữ ký số mật mã.
  - Quy trình kiểm chứng APK cho camera bắt buộc phải chạy:
    `apksigner verify --verbose --min-sdk-version 10 <apk>`
    để xác nhận chữ ký v1 hợp lệ cho baseline API 10.

### 4.4. Phân biệt Khảo sát Ban đầu vs Thực thi Thực tế (Zero-Dependency Offline Core)
- **Khảo sát Ban đầu (Historical Alternative)**: Phương án dùng `compileOnly 'com.google.android:android:2.3.3'` và JUnit bên ngoài từng được xem xét trong giai đoạn khảo sát, và nếu áp dụng sẽ đòi hỏi một lần bootstrap tải mạng ban đầu.
- **Thực thi Thực tế (Actual Implementation)**:
  - Core SDK được thiết kế hoàn toàn theo mô hình **Reflection-only thuần túy**, **KHÔNG phụ thuộc** vào bất kỳ dependency Android hay Sony stub nào ở compile-time.
  - Bộ kiểm thử `hostTest` sử dụng test runner độc lập tự viết (dependency-free `main` runner), **KHÔNG tải JUnit**.
  - Nhờ đó, Core SDK đạt trạng thái **100% Offline thực thụ trên máy đã có cache** với Gradle 9 và OpenJDK 17 mà không cần bất kỳ bước bootstrap mạng nào.
  - Lưu ý về phạm vi "Zero-Dependency": Core SDK không tải thêm thư viện bên ngoài, nhưng trên một máy mới hoàn toàn (cold-cache clone), môi trường vẫn cần có sẵn OpenJDK 17 và một lần cache phân phối Gradle wrapper ban đầu trước khi có thể chạy hoàn toàn `--offline`.
  - `android.jar` chỉ là thành phần tùy chọn nếu sau này cần audit signature Android chuyên biệt hoặc build module Sample APK.

---

## 5. Tổng hợp Rào cản Thực tế (Real Blockers) & Hướng Khắc phục

| Rào cản (Blocker) | Thực trạng | Hướng khắc phục tối thiểu |
| :--- | :--- | :--- |
| **Compiler Java 6 thiếu** | Host chỉ có JDK 21 và JDK 17 (không hỗ trợ target 6). | Dùng JDK 17 với `--release 7` cho host/JAR probe, kết hợp kỷ luật tác giả Java 6 idiom; bổ sung bytecode reference scanning độc lập thay vì chỉ dựa vào bootclasspath. |
| **Android SDK thiếu** | Không có `android.jar`, `d8`/`dx`, `aapt`/`aapt2`, `zipalign`, `apksigner`. | **Core SDK**: Hoàn toàn độc lập, không cần Android SDK local (dùng Reflection thuần túy, zero-stub).<br>**Sample APK**: Pipeline hoàn chỉnh cần bổ sung `android.jar`, `aapt`/`aapt2` (compile manifest/resources), `d8`/`dx` (chuyển class sang DEX), `zipalign` (căn chỉnh trước khi ký) và `apksigner` (ký v1). Toàn bộ pipeline APK này chưa cài đặt trên host và chưa được verify. |
| **Toolchain hardcoding** | Đường dẫn OpenJDK 17 nằm trong Cellar Homebrew của user. | Cấu hình qua Gradle Java Toolchain `JavaLanguageVersion.of(17)` và tôn trọng `JAVA_HOME`. |
| **Dependency Bootstrap** | Đã loại bỏ hoàn toàn khỏi Core SDK. | Core SDK là reflection-only và dùng dependency-free test runner, chạy 100% `--offline` khi đã có wrapper cache và JDK 17. |

---

## 6. Trạng thái Triển khai Task 1
- **Task 1 Đã Hoàn thành**:
  + Root build files (`settings.gradle`, `build.gradle`, Gradle Wrapper pinned official SHA256, `.gitignore`).
  + Module `framework/`: Đóng gói `framework.jar` (Java 7 bytecode, `major version: 51`).
  + Xác nhận cấu trúc JAR: Hoàn toàn tinh sạch, chỉ chứa đúng 5 public types trong `org.openpmca.framework` (`Availability`, `RuntimeKind`, `Probe`, `RuntimeSnapshot`, `PmcaRuntime`).
  + Kiểm định API visibility: Các hàm khởi tạo của `Probe`, `RuntimeSnapshot`, `PmcaRuntime` và các factory methods (`available`, `unavailable`, `unknown`) cùng test seam `inspect(ClassLoader)` đều được giữ ở mức package-private / private. Public surface chỉ chứa các public enum và getter bất biến theo đúng đặc tả contract.
  + Bộ kiểm thử `hostTest`: Đầy đủ 17/17 kịch bản kiểm thử pass 100% ở chế độ `--offline` (bao gồm cô lập reader failure, missing reader riêng lẻ, contract reason mapping, detection failure khi blocked lookup, kiểm chứng không có serial path, xử lý NoClassDefFoundError/LinkageError trong Class.getMethod lookup, và tiền kiểm tra Modifier.isStatic để hạ cấp an toàn non-static instance members thành UNKNOWN READ_FAILED mà không làm sập RuntimeSnapshot).
- Giữ nguyên ranh giới phân quyền: Không chạm vào `sample/`, `README.md`, hay các tài liệu của coordinator (`docs/research/`, `docs/design/`, `docs/superpowers/`).
