# SMART ATTENDANCE — Google Backend Setup & Deployment Guide

This step-by-step guide explains how to set up the Google Sheets master database, install the Google Apps Script backend engine, run tests, and deploy the production Web App using the dedicated Google Account:
**`attendancehalim@gmail.com`**.

---

## 1. Google Drive Structure Setup

1. Open a browser and sign in to Google Drive with:
   - **Account**: `attendancehalim@gmail.com`
2. In **My Drive**, create a new folder named:
   ```
   SMART ATTENDANCE
   ```
3. Open the `SMART ATTENDANCE` folder and create the following subfolders for future document archiving:
   - `Workers`
   - `Attendance`
   - `Reports`
   - `Backend Documentation`

---

## 2. Create the Master Google Spreadsheet

1. Inside the `SMART ATTENDANCE` folder in Google Drive, click **New > Google Sheets > Blank spreadsheet**.
2. Rename the spreadsheet to:
   ```
   SMART ATTENDANCE DATABASE
   ```
3. Leave the default sheet open. The initialization script in Step 4 will automatically configure all 5 required tabs (`Workers`, `Attendance`, `Admins`, `Settings`, `SyncLog`) with standard corporate headers, formatting, frozen rows, and default data.

---

## 3. Install the Google Apps Script Code

1. In your `SMART ATTENDANCE DATABASE` spreadsheet, click the top menu:
   ```
   Extensions > Apps Script
   ```
2. A new Apps Script editor tab will open. Rename the project at the top from *Untitled project* to:
   ```
   SMART ATTENDANCE BACKEND
   ```
3. In the left file tree, you will see `Code.gs`:
   - Select all existing contents in `Code.gs` and delete them.
   - Copy the entire contents of [`backend/Code.gs`](file:///d:/smartattendence/backend/Code.gs) from this repository and paste it into `Code.gs`.
4. Click the **+** (Add a file) button next to *Files* on the left, choose **Script**, and name it:
   ```
   TEST_SUITE
   ```
   - Copy the entire contents of [`backend/TEST_SUITE.gs`](file:///d:/smartattendence/backend/TEST_SUITE.gs) and paste it into `TEST_SUITE.gs`.
5. Click the **Save project** icon (disk icon or `Ctrl + S`).

---

## 4. Initialize Database & Run Backend Tests

1. At the top toolbar of Apps Script editor, find the function dropdown (next to *Debug* and *Run*).
2. Select **`initDatabase`** and click **Run**.
3. **Authorization Required**:
   - Google will prompt: *"Authorization required"*. Click **Review permissions**.
   - Select your account `attendancehalim@gmail.com`.
   - Click **Advanced** (bottom left of dialog) $\rightarrow$ click **Go to SMART ATTENDANCE BACKEND (unsafe)**.
   - Click **Allow**.
4. The execution log at the bottom will display:
   ```
   Database initialized successfully with 5 master tabs.
   ```
5. Return to your Google Spreadsheet tab and verify that all 5 tabs have been generated:
   - `Workers` (prepopulated with demo worker `EMP-0001` - Rahul Das, Mobile: `9876543210`)
   - `Attendance`
   - `Admins` (prepopulated with admin account `ADMIN-0001` - Mobile: `6003090734`, Password: `12345`)
   - `Settings` (prepopulated with timezone `Asia/Kolkata`)
   - `SyncLog`
6. Return to Apps Script editor, select function **`runAllBackendTests`**, and click **Run**.
7. View the execution log:
   ```
   ====================================================================
   STARTING SMART ATTENDANCE SECURITY & BACKEND INTEGRATION TEST SUITE
   ====================================================================
    [PASS] 1. Health Check API Online & Asia/Kolkata Timezone
    [PASS] 2. Admin Login using Registered Mobile (6003090734)
    [PASS] 2A. Admin Login does NOT return password/hash in payload
    [PASS] 2B. Admin Login returns normalized mobile number
    [PASS] 2C. Admin Login using Formatted Mobile (+91 60030 90734)
    [PASS] 2D. Admin Login using Admin ID (ADMIN-0001)
    [PASS] 3. Admin Login Rejection on Invalid Password
    [PASS] 4. Worker 1 Creation with Auto ID (EMP-xxxx)
    [PASS] 5. Worker 2 Creation with Auto ID
    [PASS] 6. Worker 1 Login using Employee ID
    [PASS] 6A. Worker Login does NOT return password/hash
    [PASS] 7. Worker 1 Login using Formatted Mobile (+91 99881-12233)
    [PASS] 8. Missing Token Rejected on Protected Endpoint
    [PASS] 9. Malformed Token Rejected
    [PASS] 10. Tampered Token (Elevated to Admin) Rejected via HMAC Signature
    [PASS] 11. Expired Token Rejected with TOKEN_EXPIRED
    [PASS] 12. Worker Token Rejected from Admin Endpoint (listWorkers)
    [PASS] 13. Worker Token Rejected from Creating Workers
    [PASS] 14. Worker 1 Rejected from Accessing Worker 2 Attendance
    [PASS] 15. Worker 1 Blocked from Punching for Worker 2
    [PASS] 16. Legitimate Worker 1 Punch In Recorded
    [PASS] 17. Replayed Request Idempotency (Duplicate Ignored Safely)
    [PASS] 18. Admin Can Access All Attendance Records
    [PASS] 19. Admin Can Record Manual Attendance with Admin Attribution
    [PASS] 20. Duplicate Mobile Number Creation Rejected
    [PASS] 21. Deactivated Worker Account Login Blocked
   ====================================================================
   SECURITY & INTEGRATION TEST RESULTS: 24 PASSED, 0 FAILED
   ====================================================================
   ```

---

## 5. Deploy as Web App

1. In the Apps Script editor, click the blue **Deploy** button (top right) $\rightarrow$ select **New deployment**.
2. In the modal, click the gear icon (*Select type*) next to *Select type* and choose **Web app**.
3. Configure the deployment settings:
   - **Description**: `SMART ATTENDANCE Web App v1`
   - **Execute as**: `Me (attendancehalim@gmail.com)`
   - **Who has access**: `Anyone`
   
   > [!IMPORTANT]
   > **Why "Anyone" access with "Execute as Me"?**
   > - Worker phones do NOT have individual Google account credentials and must NOT have direct access to Google Drive or Sheets.
   > - When configured with `Execute as: Me` and `Who has access: Anyone`, the Web App acts as a secure API gateway executing under `attendancehalim@gmail.com`'s authority on the cloud.
   > - All access is protected at the API layer by worker IDs, passwords, session tokens, and input validation.

4. Click **Deploy**.
5. Once deployment finishes, a modal will display:
   - **Web app URL**:
     ```
     https://script.google.com/macros/s/AKfycbz_XXXXXX_YYYYYY_ZZZZZZ/exec
     ```
6. **Copy this Web App URL and save it in a safe note.**
   - This URL will be used in **Prompt 5B** when connecting the Android application.

---

## 6. Verification with cURL / Postman (Optional)

You can verify the deployed Web App from PowerShell / Terminal using `curl`:

```bash
curl -L -X POST "https://script.google.com/macros/s/YOUR_DEPLOYMENT_ID/exec" \
  -H "Content-Type: application/json" \
  -d '{"action":"healthCheck"}'
```

Expected Response:
```json
{
  "success": true,
  "message": "SMART ATTENDANCE backend operational",
  "data": {
    "serverTime": "2026-08-23T22:45:00.000+05:30",
    "timezone": "Asia/Kolkata"
  }
}
```

### Test Admin Login via cURL:
```bash
curl -L -X POST "https://script.google.com/macros/s/YOUR_DEPLOYMENT_ID/exec" \
  -H "Content-Type: application/json" \
  -d '{"action":"adminLogin","identifier":"6003090734","password":"12345"}'
```

### Test Worker Login via cURL:
```bash
curl -L -X POST "https://script.google.com/macros/s/YOUR_DEPLOYMENT_ID/exec" \
  -H "Content-Type: application/json" \
  -d '{"action":"workerLogin","identifier":"EMP-0001","password":"12345"}'
```

---

## 7. Stop Boundary

> [!NOTE]
> - Prompt 5A is now complete.
> - Android code has **NOT** been modified.
> - Once you have set up and verified the Google Apps Script Web App using the steps above, you are ready to proceed to **Prompt 5B** (Android Client Integration).
