# SMART ATTENDANCE — PROMPT 14 TEST PLAN

## 1. Objective
Establish an exhaustive, multi-tier testing and regression verification plan covering every component of the SMART ATTENDANCE application (Android Jetpack Compose client + Google Apps Script Web App backend).

---

## 2. Test Architecture & Scope

### Tier 1: Automated Unit & Integration Tests (Android Client)
- **DateTime & Timezone Precision**: Verification of strict `Asia/Kolkata` date/time formatting, 12-hour/24-hour/ISO parsing, standard shifts (8h, 8h 30m), and midnight-crossing durations (11:55 PM to 12:10 AM).
- **Session & Logout Integrity**: Validation that logout clears tokens, user roles, worker/admin identities, and backstack while retaining local Room attendance database entries.
- **Admin Dashboard KPI & Polling**: Verification of real-time KPI metrics (`Total Workers`, `Present Today`, `Punched In`, `Punched Out`, `Not Marked`) under all punch transitions.
- **Duplicate & Idempotency Guards**: Single-execution concurrency guard (`_isExecutingPunch`), Room local idempotency checks, and server requestId matching.
- **Sanitized Logging**: Logcat regex masking for passwords, tokens, authTokens, passwordHashes, Bearer headers, and URL query tokens.

### Tier 2: Backend Integration & Security Tests (`TEST_SUITE.gs`)
- **Initialization & Health Check**: Idempotent setup, timezone verification (`Asia/Kolkata`).
- **Mobile Number Canonical Normalization**: Standard 10-digit Indian format parsing (`+91`, `091`, `0`, spaces, dashes).
- **Admin & Worker Authentication**: Dual login support (Employee ID or Mobile Number), invalid password rejection, inactive account blocking.
- **Zero Credential Exposure**: Passwords and password hashes never returned in API payloads.
- **Token Cryptography & RBAC**: HMAC-SHA256 signature verification, expired token rejection, tampered token rejection, worker privilege escalation rejection.
- **IDOR Protection**: Worker 1 blocked from querying Worker 2 attendance or punching for Worker 2.
- **Attendance & Idempotency**: Concurrency locking via `LockService`, duplicate punch rejection, replay idempotency.
- **Employee Management**: Auto-sequential ID generation (`EMP-0001`, `EMP-0002`), duplicate mobile number rejection.

### Tier 3: Physical Device UI & Lifecycle Tests
- **WindowInsets & System Bars**: `statusBarsPadding()`, `navigationBarsPadding()`, and `imePadding()` on gesture and 3-button navigation.
- **FAB Overlap Prevention**: `Scaffold` architecture with `contentPadding` in `AdminEmployeesScreen` and `AdminAllAttendanceScreen`.
- **Location Flow**: Pre-check location lifecycle (`CHECKING` -> `READY` / `GPS_OFF` / `PERMISSION_REQUIRED`).
- **Offline-First & Network Reconnect**: Local-first Room commit (`<10ms` response) with auto-sync upon reconnection.
- **Report Export & Sharing**: PDF/Excel report generation and secure `FileProvider` URI exposure.

---

## 3. Test Execution Matrix (38 Categories)

| ID | Test Category | Target Component | Success Criteria |
| :--- | :--- | :--- | :--- |
| 1 | **Build & Compilation** | Gradle, KSP, Compose | `testDebugUnitTest`, `compileDebugKotlin`, `assembleDebug` exit code 0. |
| 2 | **Backend Regression** | `TEST_SUITE.gs` | All 29+ integration tests pass with 0 failures. |
| 3 | **Authentication** | `AuthRepositoryImpl`, Login Screens | Correct credentials succeed; invalid credentials/inactive accounts rejected. |
| 4 | **Session Lifecycle** | `SessionManager`, `DataStore` | App restart restores valid session; logout wipes session; back button cannot reach dashboard. |
| 5 | **Worker Dashboard** | `WorkerDashboardScreen` | Responsive layout, name/date display, profile/settings navigation. |
| 6 | **Location Lifecycle** | `DefaultLocationClient`, `WorkerMarkAttendanceViewModel` | Location pre-checks immediately; no continuous collection. |
| 7 | **Instant Punch In** | `AttendanceRepositoryImpl`, Mark Attendance | UI updates in <10ms; Room record created; async background sync. |
| 8 | **Multi-Tap Debounce** | `WorkerMarkAttendanceViewModel` | 5-10 rapid taps generate exactly ONE attendance record. |
| 9 | **Instant Punch Out** | `AttendanceRepositoryImpl`, Mark Attendance | UI updates instantly; duration accurately computed. |
| 10 | **Offline Attendance** | `AttendanceDao`, `NetworkMonitor` | Airplane mode punches saved locally as `PENDING SYNC`; reconnected auto-syncs to `SYNCED`. |
| 11 | **Network Interruption** | `AttendanceRepositoryImpl` | Network drop during punch causes no crash or data loss; auto-syncs upon reconnect. |
| 12 | **Admin Live Dashboard** | `AdminDashboardViewModel` | 10s background polling updates KPIs when workers punch from other devices. |
| 13 | **Admin Attendance Log** | `AdminAllAttendanceScreen` | Complete daily register view with filtering by date/month/worker. |
| 14 | **Employee Management** | `AdminEmployeesScreen`, `WorkerRepositoryImpl` | Add/edit/deactivate workers; auto-sequential IDs; duplicate mobile rejection. |
| 15 | **Manual Attendance** | `AdminManualAttendanceScreen` | Admin records attendance with mandatory notes and audit attribution. |
| 16 | **Date & Timezone** | `DateTimeUtils` | All calculations use `Asia/Kolkata` across midnight shifts. |
| 17 | **Working Duration** | `DateTimeUtils` | 8h 00m, 8h 30m, and midnight shifts (11:55 PM - 12:10 AM -> 15m) accurate. |
| 18 | **Worker My Attendance UI** | `WorkerMyAttendanceScreen` | Date badge, status chip, punch rows, duration, no navigation bar clipping. |
| 19 | **Admin UI Quality** | Admin screens | No clipping, cards scrollable above FABs, safe system insets. |
| 20 | **Keyboard & Insets** | Input Forms | `imePadding()` ensures text fields and submit buttons are visible above keyboard. |
| 21 | **Report Generation** | `PdfReportGenerator`, `ExcelReportGenerator` | Clean PDF/Excel file creation; zero credential leakage. |
| 22 | **File Sharing** | `FileProvider`, `ReportFileHelper` | `ACTION_VIEW` and `ACTION_SEND` work without `FileUriExposedException`. |
| 23 | **Lifecycle & Rotation** | ViewModels | State preserved via StateFlow/Room flows; no duplicate network requests. |
| 24 | **App Backgrounding** | Location Client, Mark Attendance | Backgrounding and resuming retains valid state cleanly. |
| 25 | **App Killed During Sync** | `WorkManager`, Room | Record remains locally as `PENDING`; sync completes on next launch/WorkManager run. |
| 26 | **Room Database** | `SmartAttendanceDatabase` | Correct primary keys, relations, indices, and direct DAO queries. |
| 27 | **Concurrency** | `LockService`, Concurrency Guard | 50 simultaneous punch requests processed with zero data corruption. |
| 28 | **Security Regression** | `SanitizedLoggingInterceptor` | Zero secrets in APK, masked Logcat output, strict HMAC token checking. |
| 29 | **Memory & Resources** | Coroutine Scopes | ViewModel coroutines bounded; Admin polling stops when screen is invisible. |
| 30 | **Error Handling** | Repositories, ViewModels | Network drop, timeout, or server error displays friendly error; no endless loops. |
| 31 | **Data Consistency** | Room vs Sheets vs PDF | IDs, timestamps, dates, and locations match across all outputs. |
| 32 | **Regression Validation** | Complete Test Suite | All tests pass after every fix. |
| 33 | **Bug Classification** | P0 to P3 classification | Fixed in strict priority order. |
| 34 | **No Bug Masking** | Codebase Integrity | Zero masked errors or disabled assertions. |
| 35 | **Test Report** | Documentation | Full results recorded in `PROMPT14_TEST_REPORT.md`. |
| 36 | **Bug Report** | Documentation | Full bug tracking in `PROMPT14_BUG_REPORT.md`. |
| 37 | **Final Build** | `assembleDebug` | Verified `app-debug.apk` output binary. |
| 38 | **Final Report** | Executive Summary | Documented in `PROMPT14_FINAL_REPORT.md`. |
