# SMART ATTENDANCE — Backend API Documentation

This document describes the complete REST/JSON Web App API specifications for the **SMART ATTENDANCE** system, powered by Google Apps Script and Google Sheets.

---

## 1. General Overview

- **Base Web App URL Placeholder**:
  ```
  https://script.google.com/macros/s/AKfycbz_REPLACE_WITH_YOUR_DEPLOYMENT_ID/exec
  ```
- **HTTP Method**: `POST` (All data modifications & authenticated queries) / `GET` (`healthCheck`)
- **Headers**:
  ```http
  Content-Type: application/json
  ```
- **Server Timezone**: `Asia/Kolkata`
- **Response Format**: `application/json`

---

## 2. Standard Response Structure

### Success Response (`HTTP 200`)
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... }
}
```

### Error Response (`HTTP 200` with Error Payload)
```json
{
  "success": false,
  "isSuccess": false,
  "message": "Descriptive error message",
  "errorCode": "ERROR_CODE_IDENTIFIER"
}
```

---

## 3. Error Codes Reference

| Error Code | HTTP Status | Meaning |
| :--- | :--- | :--- |
| `INVALID_JSON` | 200 | Request body is not valid JSON. |
| `MISSING_ACTION` | 200 | `action` field is missing from request payload. |
| `MISSING_TOKEN` | 200 | Authentication token is missing from request. |
| `INVALID_TOKEN` | 200 | Session token is malformed. |
| `INVALID_TOKEN_SIGNATURE` | 200 | HMAC-SHA256 signature verification failed (tampered token). |
| `TOKEN_EXPIRED` | 200 | Session token has expired (exceeds 30-day lifetime). |
| `FORBIDDEN` | 200 | Role does not possess required privilege (e.g. Worker accessing Admin endpoint). |
| `FORBIDDEN_USER_MISMATCH` | 200 | Worker attempted to access or modify records of another worker. |
| `MISSING_CREDENTIALS` | 200 | Identifier/Username or password is empty. |
| `INVALID_CREDENTIALS` | 200 | Incorrect password or identifier. |
| `MISSING_MOBILE_NUMBER` | 200 | Mobile number field is empty. |
| `INVALID_MOBILE_NUMBER` | 200 | Mobile number format is invalid (fewer than 10 digits). |
| `DUPLICATE_MOBILE_NUMBER` | 200 | Mobile number is already registered to another worker. |
| `WORKER_INACTIVE` | 200 | Worker account is deactivated by administrator. |
| `WORKER_NOT_FOUND` | 200 | Worker account not found with the provided Employee ID or Mobile Number. |
| `ADMIN_INACTIVE` | 200 | Admin account is deactivated. |
| `ADMIN_NOT_FOUND` | 200 | Admin account not found. |
| `INVALID_LATITUDE` | 200 | GPS Latitude is out of bounds (-90 to +90). |
| `INVALID_LONGITUDE` | 200 | GPS Longitude is out of bounds (-180 to +180). |
| `LOCK_TIMEOUT` | 200 | Concurrency lock timeout (50+ simultaneous requests). |

---

## 4. Role-Based Access Control (RBAC) Matrix

| Endpoint | Worker Role | Admin Role | Public | Scope & Security Enforcement |
| :--- | :---: | :---: | :---: | :--- |
| `healthCheck` | Yes | Yes | Yes | Public diagnostics. No sensitive data. |
| `workerLogin` | Yes | - | Yes | Dual login (ID/Mobile). Returns token & sanitized profile. |
| `adminLogin` | - | Yes | Yes | Admin login. Returns admin token & profile. |
| `getWorker` | Yes (Self) | Yes (Any) | No | Worker can only query own profile (`sub == workerId`). |
| `listWorkers` | No | Yes | No | Admin only. |
| `createWorker` | No | Yes | No | Admin only. Server generates ID & checks mobile uniqueness. |
| `updateWorker` | No | Yes | No | Admin only. |
| `setWorkerStatus` | No | Yes | No | Admin only. |
| `resetWorkerPassword` | No | Yes | No | Admin only. Resets password to `12345`. |
| `createPunchIn` | Yes (Self) | Yes | No | Worker identity derived from authenticated token. Idempotent. |
| `createPunchOut` | Yes (Self) | Yes | No | Worker identity derived from authenticated token. Idempotent. |
| `createManualAttendance` | No | Yes | No | Admin only. Records Admin ID and attribution. |
| `getWorkerAttendance` | Yes (Self) | Yes (Any) | No | Worker can only query own attendance register. |
| `getAllAttendance` | No | Yes | No | Admin only. Multi-worker query with filters. |
| `getTodayAttendance` | Yes (Self) | Yes (All) | No | Worker receives today records for self; Admin receives all. |
| `syncAttendance` | Yes (Self) | Yes | No | Worker offline sync scoped to authenticated ID. |
| `syncPendingAttendance` | Yes (Self) | Yes | No | Batch sync scoped to authenticated worker. |

---

## 4. Authentication Endpoints

### 4.1. Worker Login (`workerLogin`)
Authenticates a field worker using **either**:
1. **Employee ID** (e.g. `EMP-0001`)
2. **Registered Mobile Number** (e.g. `9876543210` or `+91 98765 43210`)

Both identifiers resolve to the same underlying worker account and profile.

#### Option A Request (Using Employee ID):
```json
{
  "action": "workerLogin",
  "identifier": "EMP-0001",
  "password": "12345"
}
```

#### Option B Request (Using Mobile Number):
```json
{
  "action": "workerLogin",
  "identifier": "9876543210",
  "password": "12345"
}
```

#### Success Response:
```json
{
  "success": true,
  "message": "Worker authenticated successfully",
  "data": {
    "employeeId": "EMP-0001",
    "employeeName": "Rahul Das",
    "fullName": "Rahul Das",
    "mobileNumber": "9876543210",
    "workplace": "Gameri HS",
    "workplaceName": "Gameri HS",
    "designation": "Field Staff",
    "joiningDate": "2026-01-15",
    "status": "ACTIVE",
    "role": "WORKER",
    "token": "eyJ1c2VySWQiOiJFTVAtMDAwMSJ9.c7ad44c..."
  }
}
```

#### Inactive Worker Error Response:
```json
{
  "success": false,
  "isSuccess": false,
  "message": "This worker account is deactivated. Please contact Administrator.",
  "errorCode": "WORKER_INACTIVE"
}
```

---

### 4.2. Admin Login (`adminLogin`)
Authenticates an administrator using the registered Admin Mobile Number (`6003090734`) or Admin ID (`ADMIN-0001`).

#### Option A Request (Using Mobile Number):
```json
{
  "action": "adminLogin",
  "identifier": "6003090734",
  "password": "12345"
}
```

#### Option B Request (Using Formatted Mobile):
```json
{
  "action": "adminLogin",
  "identifier": "+91 60030 90734",
  "password": "12345"
}
```

#### Option C Request (Using Admin ID):
```json
{
  "action": "adminLogin",
  "identifier": "ADMIN-0001",
  "password": "12345"
}
```

#### Success Response:
```json
{
  "success": true,
  "message": "Admin authenticated successfully",
  "data": {
    "adminId": "ADMIN-0001",
    "adminName": "System Administrator",
    "fullName": "System Administrator",
    "mobileNumber": "6003090734",
    "status": "ACTIVE",
    "role": "ADMIN",
    "token": "eyJzdWIiOiJBRE1JTi0wMDAxIiwicm9sZSI6IkFETUlOIi...<HMAC_SIGNATURE>"
  }
}
```

---

## 5. Employee Management Endpoints (Admin Only)

### 5.1. List All Workers (`listWorkers`)
#### Request:
```json
{
  "action": "listWorkers",
  "token": "ADMIN_TOKEN_HERE"
}
```

#### Success Response:
```json
{
  "success": true,
  "data": [
    {
      "employeeId": "EMP-0001",
      "fullName": "Rahul Das",
      "mobileNumber": "9876543210",
      "workplaceName": "Gameri HS",
      "designation": "Field Staff",
      "joiningDate": "2026-01-15",
      "isActive": true,
      "createdAt": "2026-08-23T08:00:00.000+05:30"
    }
  ]
}
```

---

### 5.2. Create New Worker (`createWorker`)
Server auto-generates next unique ID (`EMP-0001`, `EMP-0002`...) and hashes default password `12345`.
Validates that `mobileNumber` is provided, formatted correctly (min 10 digits), and is unique across all workers.

#### Request:
```json
{
  "action": "createWorker",
  "token": "ADMIN_TOKEN_HERE",
  "fullName": "Anjali Sharma",
  "mobileNumber": "9812345678",
  "workplaceName": "Biswanath Primary School",
  "designation": "Operator",
  "joiningDate": "2026-08-24",
  "isActive": true
}
```

#### Success Response:
```json
{
  "success": true,
  "message": "Worker created successfully",
  "data": {
    "employeeId": "EMP-0002",
    "fullName": "Anjali Sharma",
    "mobileNumber": "9812345678",
    "workplaceName": "Biswanath Primary School",
    "designation": "Operator",
    "joiningDate": "2026-08-24",
    "initialPassword": "12345",
    "isActive": true
  }
}
```

#### Duplicate Mobile Error Response:
```json
{
  "success": false,
  "isSuccess": false,
  "message": "Mobile number 9812345678 is already registered to worker EMP-0002 (Anjali Sharma).",
  "errorCode": "DUPLICATE_MOBILE_NUMBER"
}
```

---

### 5.3. Update Worker Profile (`updateWorker`)
#### Request:
```json
{
  "action": "updateWorker",
  "token": "ADMIN_TOKEN_HERE",
  "employeeId": "EMP-0002",
  "fullName": "Anjali Sharma Borah",
  "mobileNumber": "9812345678",
  "workplaceName": "Biswanath HS",
  "designation": "Senior Operator",
  "joiningDate": "2026-08-24",
  "isActive": true
}
```

#### Success Response:
```json
{
  "success": true,
  "message": "Worker updated successfully",
  "data": {
    "employeeId": "EMP-0002"
  }
}
```

---

### 5.4. Activate / Deactivate Worker (`setWorkerStatus`)
#### Request:
```json
{
  "action": "setWorkerStatus",
  "token": "ADMIN_TOKEN_HERE",
  "employeeId": "EMP-0002",
  "isActive": false
}
```

#### Success Response:
```json
{
  "success": true,
  "message": "Worker status updated to INACTIVE",
  "data": {
    "employeeId": "EMP-0002",
    "isActive": false
  }
}
```

---

### 5.5. Reset Worker Password (`resetWorkerPassword`)
Resets worker password back to default `12345`.

#### Request:
```json
{
  "action": "resetWorkerPassword",
  "token": "ADMIN_TOKEN_HERE",
  "employeeId": "EMP-0002"
}
```

#### Success Response:
```json
{
  "success": true,
  "message": "Password reset successfully to default (12345)",
  "data": {
    "employeeId": "EMP-0002"
  }
}
```

---

## 6. Attendance Endpoints

### 6.1. Create Punch In (`createPunchIn`)
Single-shot GPS location capture with idempotency guard.

#### Request:
```json
{
  "action": "createPunchIn",
  "workerId": "EMP-0001",
  "attendanceId": "c68f1234-5678-4321-abcd-ef0123456789",
  "timestamp": 1755998400000,
  "latitude": 26.758412,
  "longitude": 93.125678,
  "accuracy": 12.4,
  "localArea": "Gameri, Biswanath",
  "notes": ""
}
```

#### Success Response:
```json
{
  "success": true,
  "message": "Punch In recorded successfully",
  "data": {
    "attendanceId": "c68f1234-5678-4321-abcd-ef0123456789",
    "workerId": "EMP-0001",
    "workerName": "Rahul Das",
    "attendanceType": "PUNCH_IN",
    "date": "2026-08-23",
    "time": "08:30:15",
    "localArea": "Gameri, Biswanath"
  }
}
```

---

### 6.2. Create Punch Out (`createPunchOut`)
#### Request:
```json
{
  "action": "createPunchOut",
  "workerId": "EMP-0001",
  "attendanceId": "d79a5678-1234-4321-bcde-fe0987654321",
  "timestamp": 1756030800000,
  "latitude": 26.758920,
  "longitude": 93.125110,
  "accuracy": 15.0,
  "localArea": "Gameri, Biswanath",
  "notes": ""
}
```

#### Success Response:
```json
{
  "success": true,
  "message": "Punch Out recorded successfully",
  "data": {
    "attendanceId": "d79a5678-1234-4321-bcde-fe0987654321",
    "workerId": "EMP-0001",
    "workerName": "Rahul Das",
    "attendanceType": "PUNCH_OUT",
    "date": "2026-08-23",
    "time": "17:30:00",
    "localArea": "Gameri, Biswanath"
  }
}
```

---

### 6.3. Create Manual Attendance (`createManualAttendance`)
Admin authorization with creator attribution.

#### Request:
```json
{
  "action": "createManualAttendance",
  "token": "ADMIN_TOKEN_HERE",
  "employeeId": "EMP-0001",
  "date": "2026-08-23",
  "inTime": "08:30:00",
  "outTime": "17:00:00",
  "notes": "Worker device battery discharged on-site",
  "latitude": 26.758412,
  "longitude": 93.125678,
  "accuracy": 10.0,
  "localArea": "Manual Entry by Admin"
}
```

#### Success Response:
```json
{
  "success": true,
  "message": "Manual attendance recorded successfully",
  "data": {
    "attendanceId": "f90b1122-3344-5566-7788-99aabbccdde",
    "workerId": "EMP-0001",
    "workerName": "Rahul Das",
    "date": "2026-08-23",
    "inTime": "08:30:00",
    "outTime": "17:00:00"
  }
}
```

---

### 6.4. Get Worker Attendance (`getWorkerAttendance`)
#### Request:
```json
{
  "action": "getWorkerAttendance",
  "workerId": "EMP-0001",
  "month": "2026-08"
}
```

#### Success Response:
```json
{
  "success": true,
  "data": [
    {
      "id": "c68f1234-5678-4321-abcd-ef0123456789",
      "employeeId": "EMP-0001",
      "employeeName": "Rahul Das",
      "attendanceType": "PUNCH_IN",
      "timestamp": 1755998400000,
      "date": "2026-08-23",
      "time": "08:30:15",
      "latitude": 26.758412,
      "longitude": 93.125678,
      "accuracy": 12.4,
      "localArea": "Gameri, Biswanath",
      "syncSource": "WORKER_APP",
      "createdByAdminId": null,
      "createdByAdminName": null,
      "notes": null,
      "createdAt": "2026-08-23T08:30:16.120+05:30"
    }
  ]
}
```

---

### 6.5. Get All Attendance Records (`getAllAttendance`)
Admin query with optional date, month, and workerId filters.

#### Request:
```json
{
  "action": "getAllAttendance",
  "token": "ADMIN_TOKEN_HERE",
  "date": "2026-08-23",
  "month": "",
  "workerId": ""
}
```

---

### 6.6. Batch Sync Pending Attendance (`syncPendingAttendance`)
#### Request:
```json
{
  "action": "syncPendingAttendance",
  "records": [
    {
      "attendanceId": "uuid-1",
      "employeeId": "EMP-0001",
      "attendanceType": "PUNCH_IN",
      "timestamp": 1755998400000,
      "latitude": 26.758412,
      "longitude": 93.125678,
      "accuracy": 14.0,
      "localArea": "Gameri"
    },
    {
      "attendanceId": "uuid-2",
      "employeeId": "EMP-0001",
      "attendanceType": "PUNCH_OUT",
      "timestamp": 1756030800000,
      "latitude": 26.758920,
      "longitude": 93.125110,
      "accuracy": 12.0,
      "localArea": "Gameri"
    }
  ]
}
```

#### Success Response:
```json
{
  "success": true,
  "message": "Batch synchronization completed",
  "data": {
    "syncedCount": 2,
    "totalCount": 2,
    "details": [
      { "attendanceId": "uuid-1", "success": true, "message": "Punch In recorded successfully" },
      { "attendanceId": "uuid-2", "success": true, "message": "Punch Out recorded successfully" }
    ]
  }
}
```
