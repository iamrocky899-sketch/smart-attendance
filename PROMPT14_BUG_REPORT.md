# SMART ATTENDANCE — PROMPT 14 BUG REPORT

## Bug Tracking Log

### Bug #01 (Resolved)
- **BUG ID**: BUG-P14-01
- **Severity**: P2 (Medium)
- **Screen / Component**: `DateTimeUtils.kt` / `AdminAttendanceViewModel.kt` / `AdminReportsViewModel.kt` / `WorkerMyAttendanceViewModel.kt`
- **Steps to Reproduce**:
  1. Admin creates manual attendance with In time "08:30:00" and Out time "17:00:00".
  2. Because `punchIn.createdAt` and `punchOut.createdAt` were recorded simultaneously at the moment of form submission, epoch calculation evaluated duration to "0m".
- **Expected**: Working duration should evaluate to "8h 30m".
- **Actual**: Evaluated to "0m" because it relied strictly on epoch timestamp differences.
- **Root Cause**: `DateTimeUtils.calculateDuration(inTimestamp, outTimestamp)` lacked fallback parsing of formatted time strings (`inTime` and `outTime`) when epoch timestamps are identical or zero.
- **Fix**: Enhanced `DateTimeUtils.calculateDuration` to take optional `inTime` and `outTime` parameters and added `calculateDurationFromTimes()` with overnight shift modulo arithmetic.
- **Regression Test**: Added unit tests in `DateTimeUtilsTest.kt` (`testCalculateDurationFromTimes_standard12Hour`, `testCalculateDurationFromTimes_overnightShift`, `testCalculateDurationFromTimes_24Hour`). All tests PASSED.
- **Status**: **RESOLVED**

---

### Bug #02 (Resolved)
- **BUG ID**: BUG-P14-02
- **Severity**: P3 (Low / UX)
- **Screen / Component**: `WorkerMyAttendanceViewModel.kt`
- **Steps to Reproduce**:
  1. Worker views Attendance Register for a day where a manual punch entry was recorded by the Admin.
  2. Card status displayed "0m" duration.
- **Expected**: Display "8h 30m" based on recorded manual punch times.
- **Actual**: Displayed "0m".
- **Root Cause**: Model passed only epoch timestamps without providing formatted time strings.
- **Fix**: Updated `WorkerMyAttendanceViewModel.kt` to pass `punchIn.time` and `punchOut.time` to `DateTimeUtils.calculateDuration()`.
- **Regression Test**: `testDebugUnitTest` verified.
- **Status**: **RESOLVED**

---

## Active / Unresolved Bugs
**NO UNRESOLVED BUGS FOUND**. All P0, P1, P2, and P3 issues have been resolved, verified with unit tests, and validated in clean compilation and APK builds.
