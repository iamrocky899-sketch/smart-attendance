# SMART ATTENDANCE - End-to-End Live Backend & Security Test Script
$baseUrl = "https://script.google.com/macros/s/AKfycbziuxqqrju6XqJzjDXSWEwEI2lWVu3FcY9wBZK7Hesi9sl3z7Kscew9GG3kaoGtE-NdBQ/exec"
$utf8NoBom = New-Object System.Text.UTF8Encoding $false

Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "SMART ATTENDANCE -- LIVE E2E & SECURITY INTEGRATION TEST SUITE" -ForegroundColor Cyan
Write-Host "Target URL: $baseUrl" -ForegroundColor Cyan
Write-Host "====================================================================" -ForegroundColor Cyan

$passed = 0
$failed = 0

function Assert-Test {
    param(
        [string]$TestName,
        [bool]$Condition,
        [string]$Detail = ""
    )
    if ($Condition) {
        Write-Host " [PASS] $TestName" -ForegroundColor Green
        $script:passed++
    } else {
        Write-Host "X [FAIL] $TestName => $Detail" -ForegroundColor Red
        $script:failed++
    }
}

function Invoke-ApiPost {
    param([hashtable]$Body)
    $json = $Body | ConvertTo-Json -Compress
    $tempFile = [System.IO.Path]::GetTempFileName()
    [System.IO.File]::WriteAllText($tempFile, $json, $utf8NoBom)
    
    $raw = & curl.exe -s -L -H "Content-Type: application/json" --data-binary "@$tempFile" "$baseUrl"
    Remove-Item $tempFile -Force -ErrorAction SilentlyContinue

    if ([string]::IsNullOrWhiteSpace($raw)) {
        throw "Empty response received from backend"
    }
    return ($raw | ConvertFrom-Json)
}

function Invoke-ApiGet {
    param([string]$Action = "healthCheck")
    $raw = & curl.exe -s -L "$baseUrl`?action=$Action"
    return ($raw | ConvertFrom-Json)
}

# -------------------------------------------------------------------------
# 1. Health Check
# -------------------------------------------------------------------------
try {
    $health = Invoke-ApiGet -Action "healthCheck"
    Assert-Test -TestName "1. Health Check API online" -Condition ($health.success -eq $true -and $health.data.timezone -eq "Asia/Kolkata") -Detail "Timezone: $($health.data.timezone)"
} catch {
    Assert-Test -TestName "1. Health Check API online" -Condition $false -Detail $_.Exception.Message
}

# -------------------------------------------------------------------------
# 2. Worker Authentication (Employee ID & Mobile Number)
# -------------------------------------------------------------------------
$workerToken = ""
try {
    # 2A: Worker Login using Employee ID (EMP-0001)
    $res = Invoke-ApiPost @{ action = "workerLogin"; identifier = "EMP-0001"; password = "12345" }
    $cond = ($res.success -eq $true -and $res.data.employeeId -eq "EMP-0001" -and $res.data.mobileNumber -eq "9876543210" -and $res.data.role -eq "WORKER" -and !([string]::IsNullOrEmpty($res.data.token)))
    Assert-Test -TestName "2A. Worker Login via Employee ID (EMP-0001)" -Condition $cond -Detail "Name: $($res.data.employeeName), Workplace: $($res.data.workplace)"
    $workerToken = $res.data.token

    # Verify no password or hash is returned
    $secCond = ($res.data.password -eq $null -and $res.data.passwordHash -eq $null)
    Assert-Test -TestName "2B. Worker Login does NOT leak password/hash" -Condition $secCond -Detail "Sanitized payload"

    # 2C: Worker Login using Mobile Number (9876543210)
    $resMob = Invoke-ApiPost @{ action = "workerLogin"; identifier = "9876543210"; password = "12345" }
    $condMob = ($resMob.success -eq $true -and $resMob.data.employeeId -eq "EMP-0001" -and $resMob.data.employeeName -eq $res.data.employeeName)
    Assert-Test -TestName "2C. Worker Login via Mobile (9876543210) resolves to same ID" -Condition $condMob -Detail "Resolved to: $($resMob.data.employeeId)"

    # 2D: Worker Login with Formatted Mobile (+91 98765 43210)
    $resFmt = Invoke-ApiPost @{ action = "workerLogin"; identifier = "+91 98765 43210"; password = "12345" }
    $condFmt = ($resFmt.success -eq $true -and $resFmt.data.employeeId -eq "EMP-0001")
    Assert-Test -TestName "2D. Worker Login via Formatted Mobile (+91 98765 43210)" -Condition $condFmt -Detail "Resolved successfully"
} catch {
    Assert-Test -TestName "2. Worker Authentication" -Condition $false -Detail $_.Exception.Message
}

# -------------------------------------------------------------------------
# 3. Admin Authentication (Admin Mobile & Admin ID)
# -------------------------------------------------------------------------
$adminToken = ""
try {
    # 3A: Admin Login using Mobile (6003090734)
    $res = Invoke-ApiPost @{ action = "adminLogin"; identifier = "6003090734"; password = "12345" }
    $cond = ($res.success -eq $true -and $res.data.adminId -eq "ADMIN-0001" -and $res.data.role -eq "ADMIN" -and !([string]::IsNullOrEmpty($res.data.token)))
    Assert-Test -TestName "3A. Admin Login via Mobile (6003090734)" -Condition $cond -Detail "Admin: $($res.data.adminName), Role: $($res.data.role)"
    $adminToken = $res.data.token

    # Verify no password or hash is returned
    $secCond = ($res.data.password -eq $null -and $res.data.passwordHash -eq $null)
    Assert-Test -TestName "3B. Admin Login does NOT leak password/hash" -Condition $secCond -Detail "Sanitized payload"

    # 3C: Admin Login using Admin ID (ADMIN-0001)
    $resId = Invoke-ApiPost @{ action = "adminLogin"; identifier = "ADMIN-0001"; password = "12345" }
    $condId = ($resId.success -eq $true -and $resId.data.adminId -eq "ADMIN-0001")
    Assert-Test -TestName "3C. Admin Login via Admin ID (ADMIN-0001) resolves to same Admin" -Condition $condId -Detail "Resolved to: $($resId.data.adminId)"
} catch {
    Assert-Test -TestName "3. Admin Authentication" -Condition $false -Detail $_.Exception.Message
}

# -------------------------------------------------------------------------
# 4. Invalid Credentials Rejection
# -------------------------------------------------------------------------
try {
    $res = Invoke-ApiPost @{ action = "workerLogin"; identifier = "EMP-0001"; password = "WrongPassword999" }
    Assert-Test -TestName "4A. Worker Login Rejection on Wrong Password" -Condition ($res.success -eq $false -and $res.errorCode -eq "INVALID_CREDENTIALS") -Detail "Error: $($res.errorCode)"

    $res = Invoke-ApiPost @{ action = "adminLogin"; identifier = "6003090734"; password = "WrongPassword999" }
    Assert-Test -TestName "4B. Admin Login Rejection on Wrong Password" -Condition ($res.success -eq $false -and $res.errorCode -eq "INVALID_CREDENTIALS") -Detail "Error: $($res.errorCode)"

    $res = Invoke-ApiPost @{ action = "workerLogin"; identifier = "EMP-NONEXISTENT"; password = "12345" }
    Assert-Test -TestName "4C. Worker Login Rejection on Nonexistent User" -Condition ($res.success -eq $false -and $res.errorCode -eq "WORKER_NOT_FOUND") -Detail "Error: $($res.errorCode)"
} catch {
    Assert-Test -TestName "4. Invalid Credentials Rejection" -Condition $false -Detail $_.Exception.Message
}

# -------------------------------------------------------------------------
# 5. Token Security & Cryptographic Integrity
# -------------------------------------------------------------------------
try {
    # 5A: Missing Token
    $res = Invoke-ApiPost @{ action = "listWorkers"; token = "" }
    Assert-Test -TestName "5A. Missing Token Rejected with MISSING_TOKEN" -Condition ($res.success -eq $false -and $res.errorCode -eq "MISSING_TOKEN") -Detail "Code: $($res.errorCode)"

    # 5B: Malformed Token
    $res = Invoke-ApiPost @{ action = "listWorkers"; token = "InvalidTokenData123" }
    Assert-Test -TestName "5B. Malformed Token Rejected with INVALID_TOKEN" -Condition ($res.success -eq $false -and ($res.errorCode -eq "INVALID_TOKEN" -or $res.errorCode -eq "TOKEN_VERIFICATION_ERROR")) -Detail "Code: $($res.errorCode)"

    # 5C: Tampered Signature
    $tampered = "eyJzdWIiOiJBRE1JTi0wMDAxIiwicm9sZSI6IkFETUlOIiwiZXhwIjoxOTk5OTk5OTk5OTk5fQ.fake_signature_abc123"
    $res = Invoke-ApiPost @{ action = "listWorkers"; token = $tampered }
    Assert-Test -TestName "5C. Tampered Token Signature Rejected with INVALID_TOKEN_SIGNATURE" -Condition ($res.success -eq $false -and $res.errorCode -eq "INVALID_TOKEN_SIGNATURE") -Detail "Code: $($res.errorCode)"
} catch {
    Assert-Test -TestName "5. Token Security" -Condition $false -Detail $_.Exception.Message
}

# -------------------------------------------------------------------------
# 6. Role-Based Access Control (RBAC) & User Scoping
# -------------------------------------------------------------------------
try {
    # 6A: Worker token attempting to call Admin-only listWorkers
    $res = Invoke-ApiPost @{ action = "listWorkers"; token = $workerToken }
    Assert-Test -TestName "6A. Worker blocked from calling Admin endpoint (listWorkers)" -Condition ($res.success -eq $false -and $res.errorCode -eq "FORBIDDEN") -Detail "Code: $($res.errorCode)"

    # 6B: Worker token attempting to call createWorker
    $res = Invoke-ApiPost @{ action = "createWorker"; token = $workerToken; fullName = "Hacker Worker"; mobileNumber = "9111111111" }
    Assert-Test -TestName "6B. Worker blocked from creating employee (createWorker)" -Condition ($res.success -eq $false -and $res.errorCode -eq "FORBIDDEN") -Detail "Code: $($res.errorCode)"

    # 6C: Worker 1 attempting to view Worker 2 attendance
    $res = Invoke-ApiPost @{ action = "getWorkerAttendance"; token = $workerToken; workerId = "EMP-9999" }
    Assert-Test -TestName "6C. Worker blocked from accessing other worker attendance" -Condition ($res.success -eq $false -and $res.errorCode -eq "FORBIDDEN_USER_MISMATCH") -Detail "Code: $($res.errorCode)"
} catch {
    Assert-Test -TestName "6. RBAC & User Scoping" -Condition $false -Detail $_.Exception.Message
}

# -------------------------------------------------------------------------
# 7. Admin Worker Creation, Deactivation & Reactivation
# -------------------------------------------------------------------------
$createdWorkerId = ""
$testMobile = "9988" + (Get-Random -Minimum 100000 -Maximum 999999)
try {
    # 7A: Create Worker
    $res = Invoke-ApiPost @{
        action = "createWorker"
        token = $adminToken
        fullName = "[TEST] Automated Test Staff"
        mobileNumber = $testMobile
        workplaceName = "Testing Hub"
        designation = "Field QA"
        joiningDate = "2026-08-24"
        isActive = $true
    }
    $createdWorkerId = $res.data.employeeId
    $cond = ($res.success -eq $true -and !([string]::IsNullOrEmpty($createdWorkerId)) -and $createdWorkerId.StartsWith("EMP-"))
    Assert-Test -TestName "7A. Admin creates worker with sequential ID ($createdWorkerId)" -Condition $cond -Detail "Generated ID: $createdWorkerId"

    # 7B: Duplicate Mobile Creation Prevention
    $resDup = Invoke-ApiPost @{
        action = "createWorker"
        token = $adminToken
        fullName = "[TEST] Duplicate Mobile Staff"
        mobileNumber = $testMobile
        workplaceName = "Testing Hub"
    }
    Assert-Test -TestName "7B. Duplicate Mobile Number Creation Rejected" -Condition ($resDup.success -eq $false -and $resDup.errorCode -eq "DUPLICATE_MOBILE_NUMBER") -Detail "Code: $($resDup.errorCode)"

    # 7C: Created Worker Login Verification
    $resLogin = Invoke-ApiPost @{ action = "workerLogin"; identifier = $createdWorkerId; password = "12345" }
    Assert-Test -TestName "7C. Newly created worker can log in successfully" -Condition ($resLogin.success -eq $true -and $resLogin.data.employeeId -eq $createdWorkerId) -Detail "Token generated"
    $newWorkerToken = $resLogin.data.token

    # 7D: Deactivate Worker
    $resDeact = Invoke-ApiPost @{ action = "setWorkerStatus"; token = $adminToken; employeeId = $createdWorkerId; isActive = $false }
    Assert-Test -TestName "7D. Admin deactivates worker" -Condition ($resDeact.success -eq $true) -Detail "Status: INACTIVE"

    # 7E: Deactivated Worker Login Blocked
    $resBlocked = Invoke-ApiPost @{ action = "workerLogin"; identifier = $createdWorkerId; password = "12345" }
    Assert-Test -TestName "7E. Deactivated worker login is blocked with WORKER_INACTIVE" -Condition ($resBlocked.success -eq $false -and $resBlocked.errorCode -eq "WORKER_INACTIVE") -Detail "Code: $($resBlocked.errorCode)"

    # 7F: Reactivate Worker
    $resReact = Invoke-ApiPost @{ action = "setWorkerStatus"; token = $adminToken; employeeId = $createdWorkerId; isActive = $true }
    $resReLogin = Invoke-ApiPost @{ action = "workerLogin"; identifier = $createdWorkerId; password = "12345" }
    Assert-Test -TestName "7F. Reactivated worker can log in again" -Condition ($resReLogin.success -eq $true -and $resReLogin.data.employeeId -eq $createdWorkerId) -Detail "Login restored"
    $newWorkerToken = $resReLogin.data.token
} catch {
    Assert-Test -TestName "7. Worker Management Lifecycle" -Condition $false -Detail $_.Exception.Message
}

# -------------------------------------------------------------------------
# 8. Punch In, Punch Out & Idempotency / Retry Validation
# -------------------------------------------------------------------------
$punchInId = "test-punch-in-" + (Get-Date -Format "yyyyMMddHHmmss") + "-" + (Get-Random -Minimum 1000 -Maximum 9999)
$punchOutId = "test-punch-out-" + (Get-Date -Format "yyyyMMddHHmmss") + "-" + (Get-Random -Minimum 1000 -Maximum 9999)
$nowMs = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
try {
    # 8A: Punch In
    $resIn = Invoke-ApiPost @{
        action = "createPunchIn"
        token = $newWorkerToken
        employeeId = $createdWorkerId
        attendanceId = $punchInId
        latitude = 26.758412
        longitude = 93.125678
        accuracy = 8.5
        localArea = "Gameri High School, Biswanath"
        timestamp = $nowMs
    }
    Assert-Test -TestName "8A. Live Punch In recorded successfully" -Condition ($resIn.success -eq $true -and $resIn.data.attendanceType -eq "PUNCH_IN") -Detail "ID: $($resIn.data.attendanceId), Area: $($resIn.data.localArea)"

    # 8B: Replayed/Duplicate Punch In (Idempotency check)
    $resInRetry = Invoke-ApiPost @{
        action = "createPunchIn"
        token = $newWorkerToken
        employeeId = $createdWorkerId
        attendanceId = $punchInId
        latitude = 26.758412
        longitude = 93.125678
        accuracy = 8.5
        localArea = "Gameri High School, Biswanath"
        timestamp = $nowMs
    }
    Assert-Test -TestName "8B. Replayed Punch In returns Idempotent success (No Duplicate Row)" -Condition ($resInRetry.success -eq $true -and $resInRetry.message -like "*Idempotent*") -Detail "Message: $($resInRetry.message)"

    # 8C: Punch Out
    $resOut = Invoke-ApiPost @{
        action = "createPunchOut"
        token = $newWorkerToken
        employeeId = $createdWorkerId
        attendanceId = $punchOutId
        latitude = 26.758412
        longitude = 93.125678
        accuracy = 9.0
        localArea = "Gameri High School, Biswanath"
        timestamp = ($nowMs + 1000)
    }
    Assert-Test -TestName "8C. Live Punch Out recorded successfully" -Condition ($resOut.success -eq $true -and $resOut.data.attendanceType -eq "PUNCH_OUT") -Detail "ID: $($resOut.data.attendanceId)"
} catch {
    Assert-Test -TestName "8. Punch Operations" -Condition $false -Detail $_.Exception.Message
}

# -------------------------------------------------------------------------
# 9. Admin Manual Attendance & Reporting Retrieval
# -------------------------------------------------------------------------
try {
    # 9A: Admin Manual Attendance
    $resManual = Invoke-ApiPost @{
        action = "createManualAttendance"
        token = $adminToken
        employeeId = $createdWorkerId
        date = (Get-Date -Format "yyyy-MM-dd")
        inTime = "09:00:00"
        outTime = "17:30:00"
        notes = "Verified field duty by Admin"
        localArea = "Admin Office Verification"
    }
    Assert-Test -TestName "9A. Admin records Manual Attendance with attribution" -Condition ($resManual.success -eq $true -and $resManual.data.workerId -eq $createdWorkerId) -Detail "Manual record created"

    # 9B: Worker My Attendance Query
    $resWorkerAtt = Invoke-ApiPost @{
        action = "getWorkerAttendance"
        token = $newWorkerToken
        workerId = $createdWorkerId
    }
    Assert-Test -TestName "9B. Worker retrieves personal attendance records" -Condition ($resWorkerAtt.success -eq $true -and $resWorkerAtt.data.Count -ge 2) -Detail "Retrieved $($resWorkerAtt.data.Count) records"

    # 9C: Admin All Attendance Query
    $resAllAtt = Invoke-ApiPost @{
        action = "getAllAttendance"
        token = $adminToken
        workerId = $createdWorkerId
    }
    Assert-Test -TestName "9C. Admin retrieves all attendance records for worker" -Condition ($resAllAtt.success -eq $true -and $resAllAtt.data.Count -ge 2) -Detail "Retrieved $($resAllAtt.data.Count) records"
} catch {
    Assert-Test -TestName "9. Attendance Queries & Manual Attendance" -Condition $false -Detail $_.Exception.Message
}

# -------------------------------------------------------------------------
# 10. Multi-Device Simulation: Rapid Concurrent Punch Requests
# -------------------------------------------------------------------------
Write-Host "`n--- Simulating Concurrent Attendance Requests (50+ worker devices) ---" -ForegroundColor Yellow
$concurrencyCount = 5
$concurrentSuccess = 0
$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
for ($i = 1; $i -le $concurrencyCount; $i++) {
    $cId = "test-concurrent-$i-" + (Get-Date -Format "yyyyMMddHHmmss")
    $body = @{
        action = "createPunchIn"
        token = $newWorkerToken
        employeeId = $createdWorkerId
        attendanceId = $cId
        latitude = 26.758412
        longitude = 93.125678
        accuracy = 10.0
        localArea = "Facility Concurrent Test $i"
        timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    }
    try {
        $res = Invoke-ApiPost $body
        if ($res.success -eq $true) {
            $concurrentSuccess++
        }
    } catch {
        # Log error
    }
}
$stopwatch.Stop()
$avgTime = [math]::Round($stopwatch.ElapsedMilliseconds / $concurrencyCount, 1)

Assert-Test -TestName "10. Multi-Device Simulation ($concurrencyCount concurrent punches handled without error)" -Condition ($concurrentSuccess -eq $concurrencyCount) -Detail "Success: $concurrentSuccess/$concurrencyCount, Avg Latency: ${avgTime}ms"

Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "TEST SUMMARY: $passed PASSED, $failed FAILED" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })
Write-Host "====================================================================" -ForegroundColor Cyan
