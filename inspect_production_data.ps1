$baseUrl = "https://script.google.com/macros/s/AKfycbziuxqqrju6XqJzjDXSWEwEI2lWVu3FcY9wBZK7Hesi9sl3z7Kscew9GG3kaoGtE-NdBQ/exec"
$utf8NoBom = New-Object System.Text.UTF8Encoding $false

function PostJson($body) {
    $json = $body | ConvertTo-Json -Compress
    $tmp = [System.IO.Path]::GetTempFileName()
    [System.IO.File]::WriteAllText($tmp, $json, $utf8NoBom)
    $raw = & curl.exe -s -L -H "Content-Type: application/json" --data-binary "@$tmp" "$baseUrl"
    Remove-Item $tmp -Force -ErrorAction SilentlyContinue
    return ($raw | ConvertFrom-Json)
}

$login = PostJson @{ action = "adminLogin"; identifier = "6003090734"; password = "12345" }
if ($login.success) {
    $token = $login.data.token
    Write-Host "Admin Logged In Successfully. Admin ID: $($login.data.adminId)" -ForegroundColor Green

    $workers = PostJson @{ action = "listWorkers"; token = $token }
    Write-Host "`nRegistered Workers Count: $($workers.data.Count)" -ForegroundColor Cyan
    $workers.data | Select-Object employeeId, fullName, mobileNumber, workplaceName, designation, isActive | Format-Table -AutoSize

    $att = PostJson @{ action = "getAllAttendance"; token = $token }
    Write-Host "Total Attendance Rows: $($att.data.Count)" -ForegroundColor Cyan
    $att.data | Select-Object id, employeeId, employeeName, attendanceType, date, time, localArea, createdByAdminId | Format-Table -AutoSize
} else {
    Write-Host "Admin login failed: $($login.message)" -ForegroundColor Red
}
