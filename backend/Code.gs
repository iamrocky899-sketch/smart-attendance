/**
 * ============================================================================
 * SMART ATTENDANCE — Secure Google Apps Script Backend Engine
 * Dedicated Account: attendancehalim@gmail.com
 * Timezone: Asia/Kolkata
 * Version: 1.1.0 (Hardened Security Release)
 * ============================================================================
 * 
 * ARCHITECTURE & SECURITY OVERVIEW:
 * 1. Mobile Android App (Worker/Admin) -> HTTPS JSON API -> Apps Script Web App
 * 2. Cryptographic HMAC-SHA256 Signed Session Tokens (Zero Trust Identity)
 * 3. Strict Role-Based Access Control (RBAC) separating Worker & Admin privileges
 * 4. User Scoping: Workers cannot view, modify, or punch attendance for other workers
 * 5. LockService concurrency protection for 50+ simultaneous worker punch events
 * 6. Salted SHA-256 password hashing for Worker & Admin credentials
 * 7. Server-enforced Idempotency preventing duplicate rows on offline sync retries
 * 8. Mobile Number Normalization & Strict Uniqueness Constraints
 * 9. Server-side sequential Employee ID generation (EMP-0001, EMP-0002...)
 * 10. Zero direct access to Google Drive or Sheets by worker devices
 */

// ============================================================================
// CONSTANTS & CONFIGURATION
// ============================================================================
const CONFIG = {
  TIMEZONE: 'Asia/Kolkata',
  DEFAULT_PASSWORD_SALT: 'SmartAttendance_Salt_2026',
  TOKEN_SECRET: 'SmartAttendance_HmacTokenSecret_v2_2026_SecureKey',
  TOKEN_EXPIRY_DAYS: 30,
  INITIAL_WORKER_PASSWORD: '12345',
  INITIAL_ADMIN_PASSWORD: '12345',
  SHEET_NAMES: {
    WORKERS: 'Workers',
    ATTENDANCE: 'Attendance',
    ADMINS: 'Admins',
    SETTINGS: 'Settings',
    SYNCLOG: 'SyncLog'
  }
};

// ============================================================================
// ENTRY POINTS (doGet & doPost)
// ============================================================================

/**
 * Handle HTTP GET Requests (Health Check & Diagnostics)
 */
function doGet(e) {
  try {
    const action = (e && e.parameter && e.parameter.action) ? e.parameter.action : 'healthCheck';
    
    if (action === 'healthCheck') {
      return createJsonResponse({
        success: true,
        message: 'SMART ATTENDANCE API is online and operational',
        data: {
          service: 'SMART ATTENDANCE Backend',
          version: '1.1.0',
          timezone: CONFIG.TIMEZONE,
          serverTime: getCurrentServerTime()
        }
      });
    }

    return createJsonError('Invalid GET operation. Use POST for authenticated operations.', 'INVALID_OPERATION');
  } catch (error) {
    return createJsonError('Internal server error: ' + error.message, 'INTERNAL_ERROR');
  }
}

/**
 * Handle HTTP POST Requests (All Core API Operations)
 */
function doPost(e) {
  try {
    if (!e || !e.postData || !e.postData.contents) {
      return createJsonError('Request body is empty or missing.', 'EMPTY_REQUEST_BODY');
    }

    let requestData;
    try {
      requestData = JSON.parse(e.postData.contents);
    } catch (parseError) {
      return createJsonError('Malformed JSON payload: ' + parseError.message, 'INVALID_JSON');
    }

    const action = requestData.action;
    if (!action) {
      return createJsonError('Missing "action" parameter in request payload.', 'MISSING_ACTION');
    }

    // Router dispatch
    switch (action) {
      // SYSTEM (Public)
      case 'healthCheck':
        return handleHealthCheck();

      // AUTHENTICATION (Public Login Endpoints)
      case 'workerLogin':
        return handleWorkerLogin(requestData);
      case 'adminLogin':
        return handleAdminLogin(requestData);

      // WORKER MANAGEMENT (Admin Authorized)
      case 'listWorkers':
        return handleListWorkers(requestData);
      case 'getWorker':
        return handleGetWorker(requestData);
      case 'createWorker':
        return handleCreateWorker(requestData);
      case 'updateWorker':
        return handleUpdateWorker(requestData);
      case 'setWorkerStatus':
        return handleSetWorkerStatus(requestData);
      case 'resetWorkerPassword':
        return handleResetWorkerPassword(requestData);

      // ATTENDANCE OPERATIONS (Token Protected)
      case 'createPunchIn':
        return handleCreatePunch(requestData, 'PUNCH_IN');
      case 'createPunchOut':
        return handleCreatePunch(requestData, 'PUNCH_OUT');
      case 'createManualAttendance':
        return handleCreateManualAttendance(requestData);
      case 'getWorkerAttendance':
        return handleGetWorkerAttendance(requestData);
      case 'getAllAttendance':
        return handleGetAllAttendance(requestData);
      case 'getTodayAttendance':
        return handleGetTodayAttendance(requestData);

      // OFFLINE SYNC (Token Protected)
      case 'syncAttendance':
      case 'recordAttendance':
        return handleSyncSingleAttendance(requestData);
      case 'syncPendingAttendance':
        return handleSyncBatchAttendance(requestData);

      default:
        return createJsonError('Unknown action: ' + action, 'UNKNOWN_ACTION');
    }
  } catch (error) {
    return createJsonError('Unhandled execution error: ' + error.message, 'EXECUTION_EXCEPTION');
  }
}

// ============================================================================
// SYSTEM INITIALIZATION & SPREADSHEET BINDINGS
// ============================================================================

/**
 * Get the Master Google Spreadsheet.
 * Resolves active sheet (when bound) or uses Script Property SPREADSHEET_ID.
 */
function getSpreadsheet() {
  try {
    const active = SpreadsheetApp.getActiveSpreadsheet();
    if (active) return active;
  } catch (_e) {}

  const scriptProp = PropertiesService.getScriptProperties().getProperty('SPREADSHEET_ID');
  if (scriptProp) {
    try {
      return SpreadsheetApp.openById(scriptProp);
    } catch (_e) {}
  }

  // Attempt to find or create spreadsheet
  setupSmartAttendance();
  const retryProp = PropertiesService.getScriptProperties().getProperty('SPREADSHEET_ID');
  if (retryProp) {
    return SpreadsheetApp.openById(retryProp);
  }

  throw new Error('Spreadsheet not bound and SPREADSHEET_ID could not be resolved.');
}

/**
 * ============================================================================
 * ONE-CLICK MASTER SETUP & SELF-INITIALIZATION
 * ============================================================================
 * 
 * Run this function ONCE after pasting the script into Google Apps Script.
 * Safe to execute multiple times (Idempotent).
 */
function setupSmartAttendance() {
  const lock = LockService.getScriptLock();
  try {
    lock.waitLock(30000);
  } catch (e) {
    throw new Error('Lock timeout while initializing SMART ATTENDANCE.');
  }

  try {
    const scriptProperties = PropertiesService.getScriptProperties();
    const now = getCurrentServerTime();

    // 1. Initialize Cryptographic Secrets in Script Properties if missing
    let tokenSecret = scriptProperties.getProperty('TOKEN_SECRET');
    let passwordSalt = scriptProperties.getProperty('PASSWORD_SALT');

    if (!tokenSecret) {
      tokenSecret = 'SmartAttendance_TokenSecret_' + Utilities.getUuid().replace(/-/g, '') + '_' + Date.now();
      scriptProperties.setProperty('TOKEN_SECRET', tokenSecret);
    }
    if (!passwordSalt) {
      passwordSalt = 'SmartAttendance_Salt_' + Utilities.getUuid().replace(/-/g, '') + '_' + Date.now();
      scriptProperties.setProperty('PASSWORD_SALT', passwordSalt);
    }

    // 2. Locate or Create Master Spreadsheet
    let ss;
    try {
      ss = SpreadsheetApp.getActiveSpreadsheet();
    } catch (_e) {}

    if (!ss) {
      const storedId = scriptProperties.getProperty('SPREADSHEET_ID');
      if (storedId) {
        try {
          ss = SpreadsheetApp.openById(storedId);
        } catch (_e) {}
      }
    }

    if (!ss) {
      // Find or create Drive Folder "SMART ATTENDANCE"
      let folder;
      const folders = DriveApp.getFoldersByName('SMART ATTENDANCE');
      if (folders.hasNext()) {
        folder = folders.next();
      } else {
        folder = DriveApp.createFolder('SMART ATTENDANCE');
        folder.createFolder('Workers');
        folder.createFolder('Attendance');
        folder.createFolder('Reports');
        folder.createFolder('Backend Documentation');
      }

      // Check if file exists inside folder
      const files = folder.getFilesByName('SMART ATTENDANCE DATABASE');
      if (files.hasNext()) {
        const file = files.next();
        ss = SpreadsheetApp.open(file);
      } else {
        ss = SpreadsheetApp.create('SMART ATTENDANCE DATABASE');
        const driveFile = DriveApp.getFileById(ss.getId());
        folder.addFile(driveFile);
        DriveApp.getRootFolder().removeFile(driveFile);
      }
      scriptProperties.setProperty('SPREADSHEET_ID', ss.getId());
    }

    const report = {
      spreadsheetName: ss.getName(),
      spreadsheetId: ss.getId(),
      spreadsheetUrl: ss.getUrl(),
      scriptPropertiesConfigured: true,
      sheetsConfigured: [],
      adminAccountReady: false,
      initialWorkerReady: false,
      settingsCount: 0
    };

    // 3. Configure Sheet Tabs (Idempotent)

    // 3.1 WORKERS
    let workersSheet = ss.getSheetByName(CONFIG.SHEET_NAMES.WORKERS);
    if (!workersSheet) {
      workersSheet = ss.insertSheet(CONFIG.SHEET_NAMES.WORKERS);
    }
    setupSheetHeader(workersSheet, [
      'workerId', 'workerName', 'mobileNumber', 'workplace', 
      'designation', 'joiningDate', 'passwordHash', 'status', 'createdAt', 'updatedAt'
    ]);
    report.sheetsConfigured.push(CONFIG.SHEET_NAMES.WORKERS);

    // Seed initial demo worker if table is empty
    if (workersSheet.getLastRow() <= 1) {
      const initialWorkerHash = computeHash(CONFIG.INITIAL_WORKER_PASSWORD, passwordSalt);
      workersSheet.appendRow([
        'EMP-0001', 'Rahul Das', '9876543210', 'Gameri HS', 
        'Field Staff', '2026-01-15', initialWorkerHash, 'ACTIVE', now, now
      ]);
      report.initialWorkerReady = true;
    } else {
      report.initialWorkerReady = true;
    }

    // 3.2 ATTENDANCE
    let attendanceSheet = ss.getSheetByName(CONFIG.SHEET_NAMES.ATTENDANCE);
    if (!attendanceSheet) {
      attendanceSheet = ss.insertSheet(CONFIG.SHEET_NAMES.ATTENDANCE);
    }
    setupSheetHeader(attendanceSheet, [
      'attendanceId', 'workerId', 'workerName', 'attendanceType', 'timestamp',
      'date', 'time', 'latitude', 'longitude', 'accuracy', 'localArea',
      'syncSource', 'createdByAdminId', 'createdByAdminName', 'notes', 'createdAt', 'updatedAt'
    ]);
    report.sheetsConfigured.push(CONFIG.SHEET_NAMES.ATTENDANCE);

    // 3.3 ADMINS
    let adminsSheet = ss.getSheetByName(CONFIG.SHEET_NAMES.ADMINS);
    if (!adminsSheet) {
      adminsSheet = ss.insertSheet(CONFIG.SHEET_NAMES.ADMINS);
    }
    setupSheetHeader(adminsSheet, [
      'adminId', 'adminName', 'mobileNumber', 'passwordHash', 'status', 'createdAt', 'updatedAt'
    ]);
    report.sheetsConfigured.push(CONFIG.SHEET_NAMES.ADMINS);

    // Verify, migrate, or seed initial admin ADMIN-0001
    const adminRows = adminsSheet.getDataRange().getValues();
    let adminFoundIndex = -1;
    for (let i = 1; i < adminRows.length; i++) {
      const rowId = String(adminRows[i][0]).trim().toUpperCase();
      const rowMob = normalizeMobileNumber(adminRows[i][2]);
      if (rowId === 'ADMIN-0001' || rowId === 'ADMIN' || rowMob === '6003090734') {
        adminFoundIndex = i + 1;
        break;
      }
    }

    const adminHash = computeHash(CONFIG.INITIAL_ADMIN_PASSWORD, passwordSalt);

    if (adminFoundIndex === -1) {
      adminsSheet.appendRow([
        'ADMIN-0001', 'System Administrator', '6003090734', adminHash, 'ACTIVE', now, now
      ]);
      report.adminAccountReady = true;
    } else {
      // Ensure row has correct 7-column alignment: adminId, adminName, mobileNumber, passwordHash, status
      const existingRow = adminRows[adminFoundIndex - 1];
      const currentMob = normalizeMobileNumber(existingRow[2]);
      if (currentMob !== '6003090734' || String(existingRow[0]).trim().toUpperCase() !== 'ADMIN-0001' || String(existingRow[4]).trim().toUpperCase() !== 'ACTIVE') {
        adminsSheet.getRange(adminFoundIndex, 1, 1, 7).setValues([[
          'ADMIN-0001', 'System Administrator', '6003090734', adminHash, 'ACTIVE', existingRow[5] || now, now
        ]]);
      }
      report.adminAccountReady = true;
    }

    // 3.4 SETTINGS
    let settingsSheet = ss.getSheetByName(CONFIG.SHEET_NAMES.SETTINGS);
    if (!settingsSheet) {
      settingsSheet = ss.insertSheet(CONFIG.SHEET_NAMES.SETTINGS);
    }
    setupSheetHeader(settingsSheet, ['key', 'value', 'updatedAt']);
    report.sheetsConfigured.push(CONFIG.SHEET_NAMES.SETTINGS);

    // Required settings key-value pairs
    const requiredSettings = {
      'appName': 'SMART ATTENDANCE',
      'timezone': CONFIG.TIMEZONE,
      'backendVersion': '1.0.0',
      'attendancePolicy': 'ANY_LOCATION',
      'workerInitialPasswordPolicy': '12345',
      'adminInitialPasswordPolicy': '12345',
      'tokenExpiryHours': '720'
    };

    const existingSettings = {};
    const settingRows = settingsSheet.getDataRange().getValues();
    for (let s = 1; s < settingRows.length; s++) {
      existingSettings[String(settingRows[s][0]).trim()] = s + 1;
    }

    for (const key in requiredSettings) {
      if (!existingSettings[key]) {
        settingsSheet.appendRow([key, requiredSettings[key], now]);
      }
    }
    report.settingsCount = settingsSheet.getLastRow() - 1;

    // 3.5 SYNCLOG
    let syncLogSheet = ss.getSheetByName(CONFIG.SHEET_NAMES.SYNCLOG);
    if (!syncLogSheet) {
      syncLogSheet = ss.insertSheet(CONFIG.SHEET_NAMES.SYNCLOG);
    }
    setupSheetHeader(syncLogSheet, [
      'syncId', 'requestId', 'workerId', 'operation', 'status', 'message', 'timestamp'
    ]);
    report.sheetsConfigured.push(CONFIG.SHEET_NAMES.SYNCLOG);

    // Clean up default Sheet1 if present
    const sheet1 = ss.getSheetByName('Sheet1');
    if (sheet1 && ss.getSheets().length > 1) {
      try { ss.deleteSheet(sheet1); } catch (_e) {}
    }

    Logger.log('====================================================');
    Logger.log('SMART ATTENDANCE SELF-INITIALIZATION COMPLETED');
    Logger.log(JSON.stringify(report, null, 2));
    Logger.log('====================================================');

    return 'SMART ATTENDANCE setup successfully completed.\nSpreadsheet URL: ' + ss.getUrl();
  } finally {
    lock.releaseLock();
  }
}

/**
 * Backward-compatible alias for setupSmartAttendance
 */
function initDatabase() {
  return setupSmartAttendance();
}

/**
 * Helper to apply consistent styling to sheet header rows
 */
function setupSheetHeader(sheet, columns) {
  if (sheet.getLastRow() === 0) {
    sheet.appendRow(columns);
  } else {
    const firstRow = sheet.getRange(1, 1, 1, columns.length).getValues()[0];
    let matches = true;
    for (let c = 0; c < columns.length; c++) {
      if (firstRow[c] !== columns[c]) {
        matches = false;
        break;
      }
    }
    if (!matches) {
      sheet.getRange(1, 1, 1, columns.length).setValues([columns]);
    }
  }

  const range = sheet.getRange(1, 1, 1, columns.length);
  range.setFontWeight('bold');
  range.setBackground('#1B365D');
  range.setFontColor('#FFFFFF');
  sheet.setFrozenRows(1);
}

/**
 * Check and return complete system diagnostic information
 */
function checkSystemDiagnostics() {
  const ss = getSpreadsheet();
  const scriptProperties = PropertiesService.getScriptProperties();
  
  return {
    service: 'SMART ATTENDANCE Backend',
    version: '1.0.0',
    timezone: CONFIG.TIMEZONE,
    serverTime: getCurrentServerTime(),
    spreadsheetId: ss.getId(),
    spreadsheetName: ss.getName(),
    hasTokenSecret: !!scriptProperties.getProperty('TOKEN_SECRET'),
    hasPasswordSalt: !!scriptProperties.getProperty('PASSWORD_SALT'),
    sheets: ss.getSheets().map(function(s) { return s.getName(); })
  };
}

// ============================================================================
// CRYPTOGRAPHY & TOKEN SECURITY ENGINE
// ============================================================================

/**
 * Retrieve a dynamic secret from Script Properties or fallback to default
 */
function getSecret(key, defaultValue) {
  try {
    const prop = PropertiesService.getScriptProperties().getProperty(key);
    if (prop) return prop;
  } catch (e) {}
  return defaultValue;
}

/**
 * Convert byte array to clean hex string
 */
function bytesToHex(bytes) {
  let hex = '';
  for (let i = 0; i < bytes.length; i++) {
    let byteStr = (bytes[i] & 0xFF).toString(16);
    if (byteStr.length === 1) byteStr = '0' + byteStr;
    hex += byteStr;
  }
  return hex;
}

/**
 * Compute Salted SHA-256 Hash for Password Storage & Verification
 */
function computeHash(input, salt) {
  const effectiveSalt = salt || getSecret('PASSWORD_SALT', CONFIG.DEFAULT_PASSWORD_SALT);
  const salted = effectiveSalt + ':' + String(input);
  const rawBytes = Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256, salted, Utilities.Charset.UTF_8);
  return bytesToHex(rawBytes);
}

/**
 * Compute HMAC-SHA256 Signature for Cryptographic Token Verification
 */
function computeHmacSignature(message, secret) {
  const effectiveSecret = secret || getSecret('TOKEN_SECRET', CONFIG.TOKEN_SECRET);
  const rawBytes = Utilities.computeHmacSha256Signature(message, effectiveSecret, Utilities.Charset.UTF_8);
  return bytesToHex(rawBytes);
}

/**
 * Generate a Cryptographically Signed Session Token
 */
function generateAuthToken(userId, role, name) {
  const payload = {
    sub: String(userId).trim(),
    role: String(role).trim().toUpperCase(),
    name: String(name || userId).trim(),
    iat: Date.now(),
    exp: Date.now() + (CONFIG.TOKEN_EXPIRY_DAYS * 24 * 60 * 60 * 1000)
  };
  const payloadJson = JSON.stringify(payload);
  const base64Payload = Utilities.base64EncodeWebSafe(payloadJson);
  const signature = computeHmacSignature(base64Payload);
  return base64Payload + '.' + signature;
}

/**
 * Verify Session Token, Role Privileges, and Optional User-Scope Match.
 * 
 * @param {string} token - The Bearer token provided in the request
 * @param {string} requiredRole - 'WORKER' or 'ADMIN'
 * @param {string} [expectedUserId] - Optional user ID to enforce worker self-scoping
 * @returns {object} { isValid: boolean, userId: string, role: string, userName: string, message: string, code: string }
 */
function verifyTokenAndRole(token, requiredRole, expectedUserId) {
  if (!token || typeof token !== 'string') {
    return { isValid: false, message: 'Authentication token is required for this operation.', code: 'MISSING_TOKEN' };
  }

  try {
    const parts = token.trim().split('.');
    if (parts.length !== 2) {
      return { isValid: false, message: 'Malformed session token format.', code: 'INVALID_TOKEN' };
    }

    const base64Payload = parts[0];
    const signature = parts[1];

    const expectedSignature = computeHmacSignature(base64Payload);
    if (signature !== expectedSignature) {
      return { isValid: false, message: 'Invalid session token signature. Access denied.', code: 'INVALID_TOKEN_SIGNATURE' };
    }

    const payloadJson = Utilities.newBlob(Utilities.base64DecodeWebSafe(base64Payload)).getDataAsString();
    const payload = JSON.parse(payloadJson);

    if (!payload.sub || !payload.role || !payload.exp) {
      return { isValid: false, message: 'Invalid session token claims.', code: 'INVALID_TOKEN_CLAIMS' };
    }

    if (Date.now() > payload.exp) {
      return { isValid: false, message: 'Session token has expired. Please log in again.', code: 'TOKEN_EXPIRED' };
    }

    // Role-Based Access Control (RBAC):
    // If requiredRole is ADMIN, strictly require ADMIN role
    if (requiredRole === 'ADMIN' && payload.role !== 'ADMIN') {
      return { isValid: false, message: 'Forbidden: Administrator privileges required.', code: 'FORBIDDEN' };
    }

    // If requiredRole is WORKER, allow WORKER or ADMIN
    if (requiredRole === 'WORKER' && payload.role !== 'WORKER' && payload.role !== 'ADMIN') {
      return { isValid: false, message: 'Forbidden: Valid Worker or Admin token required.', code: 'FORBIDDEN' };
    }

    // User-Scoping Check:
    // If expectedUserId is specified and caller is not ADMIN, enforce matching identity
    if (expectedUserId && payload.role !== 'ADMIN') {
      const cleanExpected = String(expectedUserId).trim().toUpperCase();
      const cleanActual = String(payload.sub).trim().toUpperCase();
      if (cleanActual !== cleanExpected) {
        return { 
          isValid: false, 
          message: 'Forbidden: You cannot access or modify records belonging to another worker.', 
          code: 'FORBIDDEN_USER_MISMATCH' 
        };
      }
    }

    return {
      isValid: true,
      userId: payload.sub,
      role: payload.role,
      userName: payload.name || payload.sub
    };
  } catch (e) {
    return { isValid: false, message: 'Token verification failed: ' + e.message, code: 'TOKEN_VERIFICATION_ERROR' };
  }
}

/**
 * Normalize mobile number to a canonical 10-digit Indian mobile string.
 * Handles inputs like:
 * - 6003090734
 * - +91 60030 90734
 * - +91-60030-90734
 * - 916003090734
 * - 0916003090734
 * - 06003090734
 * - numeric types from Sheet cells
 */
function normalizeMobileNumber(input) {
  if (input === null || input === undefined) return '';
  let str = (typeof input === 'number') ? input.toFixed(0) : String(input).trim();
  let digits = str.replace(/\D/g, '');

  if (digits.length === 13 && digits.startsWith('091')) {
    digits = digits.substring(3);
  } else if (digits.length === 12 && digits.startsWith('91')) {
    digits = digits.substring(2);
  } else if (digits.length === 11 && digits.startsWith('0')) {
    digits = digits.substring(1);
  } else if (digits.length > 10) {
    digits = digits.slice(-10);
  }

  return digits;
}

// ============================================================================
// AUTHENTICATION HANDLERS
// ============================================================================

/**
 * Worker Login Handler
 * Supports TWO login identifiers in a single field:
 * 1. Employee ID (e.g. EMP-0001)
 * 2. Registered Mobile Number (e.g. 9876543210 or +919876543210)
 */
function handleWorkerLogin(data) {
  const identifier = (data.identifier || data.workerId || data.username || '').trim();
  const password = (data.password || '').trim();

  if (!identifier || !password) {
    return createJsonError('Employee ID / Mobile Number and Password are required.', 'MISSING_CREDENTIALS');
  }

  const ss = getSpreadsheet();
  const sheet = ss.getSheetByName(CONFIG.SHEET_NAMES.WORKERS);
  if (!sheet) return createJsonError('Workers database unavailable.', 'DATABASE_ERROR');

  const rows = sheet.getDataRange().getValues();
  const upperIdentifier = identifier.toUpperCase();
  const normalizedIdentifier = normalizeMobileNumber(identifier);

  // Columns: 0:workerId, 1:workerName, 2:mobileNumber, 3:workplace, 4:designation, 5:joiningDate, 6:passwordHash, 7:status
  for (let i = 1; i < rows.length; i++) {
    const row = rows[i];
    const rowEmpId = String(row[0]).trim().toUpperCase();
    const rowMobileNormalized = normalizeMobileNumber(row[2]);

    const isMatch = (rowEmpId === upperIdentifier) || 
                    (normalizedIdentifier.length >= 10 && rowMobileNormalized === normalizedIdentifier);

    if (isMatch) {
      const status = String(row[7]).trim().toUpperCase();
      if (status !== 'ACTIVE') {
        return createJsonError('This worker account is deactivated. Please contact Administrator.', 'WORKER_INACTIVE');
      }

      const storedHash = String(row[6]).trim();
      const inputHash = computeHash(password);

      let isValidPassword = false;
      if (storedHash) {
        isValidPassword = (storedHash === inputHash);
      } else if (password === CONFIG.INITIAL_WORKER_PASSWORD) {
        isValidPassword = true;
        sheet.getRange(i + 1, 7).setValue(inputHash);
      }

      if (!isValidPassword) {
        return createJsonError('Invalid password. Please verify your credentials.', 'INVALID_CREDENTIALS');
      }

      const canonicalWorkerId = String(row[0]).trim();
      const canonicalWorkerName = String(row[1]).trim();
      const canonicalMobile = normalizeMobileNumber(row[2]);

      const token = generateAuthToken(canonicalWorkerId, 'WORKER', canonicalWorkerName);
      return createJsonResponse({
        success: true,
        message: 'Worker authenticated successfully',
        data: {
          employeeId: canonicalWorkerId,
          employeeName: canonicalWorkerName,
          fullName: canonicalWorkerName,
          mobileNumber: canonicalMobile,
          workplace: String(row[3]).trim(),
          workplaceName: String(row[3]).trim(),
          designation: String(row[4]).trim(),
          joiningDate: String(row[5]).trim(),
          status: status,
          role: 'WORKER',
          token: token
        }
      });
    }
  }

  return createJsonError('Worker account not found with the provided Employee ID or Mobile Number.', 'WORKER_NOT_FOUND');
}

/**
 * Admin Login Handler
 * Authenticates using registered Admin Mobile Number (e.g. 6003090734) or Admin ID (ADMIN-0001).
 */
function handleAdminLogin(data) {
  const identifier = (data.identifier || data.mobileNumber || data.adminId || data.username || '').trim();
  const password = (data.password || '').trim();

  if (!identifier || !password) {
    return createJsonError('Admin Mobile Number / ID and Password are required.', 'MISSING_CREDENTIALS');
  }

  const ss = getSpreadsheet();
  const sheet = ss.getSheetByName(CONFIG.SHEET_NAMES.ADMINS);
  if (!sheet) return createJsonError('Admins database unavailable.', 'DATABASE_ERROR');

  const rows = sheet.getDataRange().getValues();
  const upperIdentifier = identifier.toUpperCase();
  const normalizedIdentifier = normalizeMobileNumber(identifier);

  // Columns: 0:adminId, 1:adminName, 2:mobileNumber, 3:passwordHash, 4:status, 5:createdAt, 6:updatedAt
  for (let i = 1; i < rows.length; i++) {
    const row = rows[i];
    const rowAdminId = String(row[0]).trim().toUpperCase();
    const rowMobileNormalized = normalizeMobileNumber(row[2]);

    const isMatch = (rowAdminId === upperIdentifier) || 
                    (normalizedIdentifier.length >= 10 && rowMobileNormalized === normalizedIdentifier);

    if (isMatch) {
      const status = String(row[4]).trim().toUpperCase();
      if (status !== 'ACTIVE') {
        return createJsonError('This Admin account is inactive.', 'ADMIN_INACTIVE');
      }

      const storedHash = String(row[3]).trim();
      const inputHash = computeHash(password);

      let isValidPassword = false;
      if (storedHash) {
        isValidPassword = (storedHash === inputHash);
      } else if (password === CONFIG.INITIAL_ADMIN_PASSWORD) {
        isValidPassword = true;
        sheet.getRange(i + 1, 4).setValue(inputHash);
      }

      if (!isValidPassword) {
        return createJsonError('Invalid Admin credentials.', 'INVALID_CREDENTIALS');
      }

      const canonicalAdminId = String(row[0]).trim();
      const canonicalAdminName = String(row[1]).trim();
      const canonicalMobile = normalizeMobileNumber(row[2]);

      const token = generateAuthToken(canonicalAdminId, 'ADMIN', canonicalAdminName);
      return createJsonResponse({
        success: true,
        message: 'Admin authenticated successfully',
        data: {
          adminId: canonicalAdminId,
          adminName: canonicalAdminName,
          fullName: canonicalAdminName,
          mobileNumber: canonicalMobile,
          status: status,
          role: 'ADMIN',
          token: token
        }
      });
    }
  }

  return createJsonError('Admin account not found with the provided Mobile Number or Admin ID.', 'ADMIN_NOT_FOUND');
}

// ============================================================================
// WORKER MANAGEMENT HANDLERS (Admin Authorized)
// ============================================================================

/**
 * List All Workers (Admin Only)
 */
function handleListWorkers(data) {
  const auth = verifyTokenAndRole(data.token, 'ADMIN');
  if (!auth.isValid) return createJsonError(auth.message, auth.code);

  const ss = getSpreadsheet();
  const sheet = ss.getSheetByName(CONFIG.SHEET_NAMES.WORKERS);
  const rows = sheet.getDataRange().getValues();
  const workers = [];

  for (let i = 1; i < rows.length; i++) {
    const row = rows[i];
    workers.push({
      employeeId: String(row[0]).trim(),
      fullName: String(row[1]).trim(),
      mobileNumber: normalizeMobileNumber(row[2]),
      workplaceName: String(row[3]).trim(),
      designation: String(row[4]).trim(),
      joiningDate: String(row[5]).trim(),
      isActive: String(row[7]).toUpperCase() === 'ACTIVE',
      createdAt: row[8]
    });
  }

  return createJsonResponse({
    success: true,
    data: workers
  });
}

/**
 * Get Single Worker Profile (Worker Self-Scoped or Admin)
 */
function handleGetWorker(data) {
  const targetWorkerId = (data.workerId || data.employeeId || '').trim().toUpperCase();
  if (!targetWorkerId) return createJsonError('workerId is required.', 'MISSING_WORKER_ID');

  // Verify token: Worker can only view self; Admin can view any worker
  const auth = verifyTokenAndRole(data.token, 'WORKER', targetWorkerId);
  if (!auth.isValid) return createJsonError(auth.message, auth.code);

  const ss = getSpreadsheet();
  const sheet = ss.getSheetByName(CONFIG.SHEET_NAMES.WORKERS);
  const rows = sheet.getDataRange().getValues();

  for (let i = 1; i < rows.length; i++) {
    const row = rows[i];
    if (String(row[0]).trim().toUpperCase() === targetWorkerId) {
      return createJsonResponse({
        success: true,
        data: {
          employeeId: String(row[0]).trim(),
          fullName: String(row[1]).trim(),
          mobileNumber: normalizeMobileNumber(row[2]),
          workplaceName: String(row[3]).trim(),
          designation: String(row[4]).trim(),
          joiningDate: String(row[5]).trim(),
          isActive: String(row[7]).toUpperCase() === 'ACTIVE',
          createdAt: row[8]
        }
      });
    }
  }

  return createJsonError('Worker not found.', 'WORKER_NOT_FOUND');
}

/**
 * Create New Worker (Admin Only)
 * Server-side Sequential ID Generation (EMP-0001, EMP-0002...) with Mobile Uniqueness Validation.
 */
function handleCreateWorker(data) {
  const auth = verifyTokenAndRole(data.token, 'ADMIN');
  if (!auth.isValid) return createJsonError(auth.message, auth.code);

  const name = (data.fullName || data.workerName || '').trim();
  const rawMobile = (data.mobileNumber || '').trim();
  const workplace = (data.workplaceName || data.workplace || '').trim() || 'Headquarters';
  const designation = (data.designation || '').trim() || 'Staff';
  const joiningDate = (data.joiningDate || getCurrentDate()).trim();
  const isActive = (data.isActive !== undefined) ? (data.isActive === true || data.isActive === 'true') : true;

  if (!name) return createJsonError('Worker name is required.', 'MISSING_WORKER_NAME');
  if (!rawMobile) return createJsonError('Mobile number is required.', 'MISSING_MOBILE_NUMBER');

  const normalizedMobile = normalizeMobileNumber(rawMobile);
  if (normalizedMobile.length < 10) {
    return createJsonError('Invalid mobile number format. Must be at least 10 digits.', 'INVALID_MOBILE_NUMBER');
  }

  const lock = LockService.getScriptLock();
  try {
    lock.waitLock(30000); // 30s timeout
  } catch (e) {
    return createJsonError('Server busy. Please try again.', 'LOCK_TIMEOUT');
  }

  try {
    const ss = getSpreadsheet();
    const sheet = ss.getSheetByName(CONFIG.SHEET_NAMES.WORKERS);
    const rows = sheet.getDataRange().getValues();
    
    // 1. Validate Mobile Number Uniqueness
    for (let i = 1; i < rows.length; i++) {
      const existingMobile = normalizeMobileNumber(rows[i][2]);
      if (existingMobile === normalizedMobile) {
        return createJsonError('Mobile number ' + rawMobile + ' is already registered to worker ' + rows[i][0] + ' (' + rows[i][1] + ').', 'DUPLICATE_MOBILE_NUMBER');
      }
    }

    // 2. Determine highest existing sequential ID number
    let maxIdNum = 0;
    const empRegex = /EMP-(\d+)/i;
    for (let i = 1; i < rows.length; i++) {
      const match = empRegex.exec(String(rows[i][0]));
      if (match) {
        const num = parseInt(match[1], 10);
        if (!isNaN(num) && num > maxIdNum) {
          maxIdNum = num;
        }
      }
    }

    const nextIdNum = maxIdNum + 1;
    const generatedId = 'EMP-' + ('0000' + nextIdNum).slice(-4);
    const initialHash = computeHash(CONFIG.INITIAL_WORKER_PASSWORD);
    const now = getCurrentServerTime();
    const status = isActive ? 'ACTIVE' : 'INACTIVE';

    sheet.appendRow([
      generatedId, name, normalizedMobile, workplace, designation, joiningDate, initialHash, status, now, now
    ]);

    return createJsonResponse({
      success: true,
      message: 'Worker created successfully',
      data: {
        employeeId: generatedId,
        fullName: name,
        mobileNumber: normalizedMobile,
        workplaceName: workplace,
        designation: designation,
        joiningDate: joiningDate,
        initialPassword: CONFIG.INITIAL_WORKER_PASSWORD,
        isActive: isActive
      }
    });
  } finally {
    lock.releaseLock();
  }
}

/**
 * Update Worker Profile (Admin Only)
 */
function handleUpdateWorker(data) {
  const auth = verifyTokenAndRole(data.token, 'ADMIN');
  if (!auth.isValid) return createJsonError(auth.message, auth.code);

  const workerId = (data.employeeId || data.workerId || '').trim().toUpperCase();
  if (!workerId) return createJsonError('workerId is required.', 'MISSING_WORKER_ID');

  const lock = LockService.getScriptLock();
  try { lock.waitLock(30000); } catch (e) { return createJsonError('Server busy.', 'LOCK_TIMEOUT'); }

  try {
    const ss = getSpreadsheet();
    const sheet = ss.getSheetByName(CONFIG.SHEET_NAMES.WORKERS);
    const rows = sheet.getDataRange().getValues();

    for (let i = 1; i < rows.length; i++) {
      if (String(rows[i][0]).trim().toUpperCase() === workerId) {
        const rowIndex = i + 1;

        if (data.mobileNumber !== undefined) {
          const rawMobile = String(data.mobileNumber).trim();
          const normalizedNewMobile = normalizeMobileNumber(rawMobile);
          if (normalizedNewMobile.length < 10) {
            return createJsonError('Invalid mobile number format. Must be at least 10 digits.', 'INVALID_MOBILE_NUMBER');
          }
          // Check uniqueness among OTHER workers
          for (let j = 1; j < rows.length; j++) {
            if (String(rows[j][0]).trim().toUpperCase() !== workerId) {
              const existingMobile = normalizeMobileNumber(rows[j][2]);
              if (existingMobile === normalizedNewMobile) {
                return createJsonError('Mobile number already registered to another worker (' + rows[j][0] + ').', 'DUPLICATE_MOBILE_NUMBER');
              }
            }
          }
          sheet.getRange(rowIndex, 3).setValue(normalizedNewMobile);
        }

        if (data.fullName !== undefined) sheet.getRange(rowIndex, 2).setValue(data.fullName);
        if (data.workplaceName !== undefined) sheet.getRange(rowIndex, 4).setValue(data.workplaceName);
        if (data.designation !== undefined) sheet.getRange(rowIndex, 5).setValue(data.designation);
        if (data.joiningDate !== undefined) sheet.getRange(rowIndex, 6).setValue(data.joiningDate);
        if (data.isActive !== undefined) {
          const status = (data.isActive === true || data.isActive === 'true' || data.isActive === 'ACTIVE') ? 'ACTIVE' : 'INACTIVE';
          sheet.getRange(rowIndex, 8).setValue(status);
        }
        sheet.getRange(rowIndex, 10).setValue(getCurrentServerTime());

        return createJsonResponse({
          success: true,
          message: 'Worker updated successfully',
          data: { employeeId: workerId }
        });
      }
    }

    return createJsonError('Worker not found.', 'WORKER_NOT_FOUND');
  } finally {
    lock.releaseLock();
  }
}

/**
 * Activate / Deactivate Worker (Admin Only)
 */
function handleSetWorkerStatus(data) {
  const auth = verifyTokenAndRole(data.token, 'ADMIN');
  if (!auth.isValid) return createJsonError(auth.message, auth.code);

  const workerId = (data.employeeId || data.workerId || '').trim().toUpperCase();
  const isActive = (data.isActive === true || data.isActive === 'true' || data.status === 'ACTIVE');

  if (!workerId) return createJsonError('workerId is required.', 'MISSING_WORKER_ID');

  const lock = LockService.getScriptLock();
  try { lock.waitLock(30000); } catch (e) { return createJsonError('Server busy.', 'LOCK_TIMEOUT'); }

  try {
    const ss = getSpreadsheet();
    const sheet = ss.getSheetByName(CONFIG.SHEET_NAMES.WORKERS);
    const rows = sheet.getDataRange().getValues();

    for (let i = 1; i < rows.length; i++) {
      if (String(rows[i][0]).trim().toUpperCase() === workerId) {
        const rowIndex = i + 1;
        const status = isActive ? 'ACTIVE' : 'INACTIVE';
        sheet.getRange(rowIndex, 8).setValue(status);
        sheet.getRange(rowIndex, 10).setValue(getCurrentServerTime());

        return createJsonResponse({
          success: true,
          message: 'Worker status updated to ' + status,
          data: { employeeId: workerId, isActive: isActive }
        });
      }
    }

    return createJsonError('Worker not found.', 'WORKER_NOT_FOUND');
  } finally {
    lock.releaseLock();
  }
}

/**
 * Reset Worker Password back to Default 12345 (Admin Only)
 */
function handleResetWorkerPassword(data) {
  const auth = verifyTokenAndRole(data.token, 'ADMIN');
  if (!auth.isValid) return createJsonError(auth.message, auth.code);

  const workerId = (data.employeeId || data.workerId || '').trim().toUpperCase();
  if (!workerId) return createJsonError('workerId is required.', 'MISSING_WORKER_ID');

  const lock = LockService.getScriptLock();
  try { lock.waitLock(30000); } catch (e) { return createJsonError('Server busy.', 'LOCK_TIMEOUT'); }

  try {
    const ss = getSpreadsheet();
    const sheet = ss.getSheetByName(CONFIG.SHEET_NAMES.WORKERS);
    const rows = sheet.getDataRange().getValues();

    for (let i = 1; i < rows.length; i++) {
      if (String(rows[i][0]).trim().toUpperCase() === workerId) {
        const rowIndex = i + 1;
        const defaultHash = computeHash(CONFIG.INITIAL_WORKER_PASSWORD);
        sheet.getRange(rowIndex, 7).setValue(defaultHash);
        sheet.getRange(rowIndex, 10).setValue(getCurrentServerTime());

        return createJsonResponse({
          success: true,
          message: 'Password reset successfully to default (12345)',
          data: { employeeId: workerId }
        });
      }
    }

    return createJsonError('Worker not found.', 'WORKER_NOT_FOUND');
  } finally {
    lock.releaseLock();
  }
}

// ============================================================================
// ATTENDANCE RECORDING & IDEMPOTENCY
// ============================================================================

/**
 * Core Punch In / Punch Out Handler
 * Authenticates request token, enforces user scoping (no worker impersonation), and checks idempotency.
 */
function handleCreatePunch(data, punchType) {
  // 1. Authenticate Request Token
  const rawTargetId = (data.employeeId || data.workerId || '').trim().toUpperCase();
  const auth = verifyTokenAndRole(data.token, 'WORKER', rawTargetId || undefined);
  if (!auth.isValid) return createJsonError(auth.message, auth.code);

  // Enforce that worker identity is determined by authenticated token if worker role
  const workerId = (auth.role === 'ADMIN' && rawTargetId) ? rawTargetId : auth.userId;
  const requestId = (data.attendanceId || data.requestId || generateUUID()).trim();
  const latitude = parseFloat(data.latitude);
  const longitude = parseFloat(data.longitude);
  const accuracy = parseFloat(data.accuracy) || 0.0;
  const localArea = (data.localArea || data.address || 'Location unavailable').trim();
  const clientTimestamp = data.timestamp || Date.now();

  // 2. Validate mandatory coordinates
  if (isNaN(latitude) || latitude < -90 || latitude > 90) {
    return createJsonError('Invalid latitude coordinate: ' + latitude, 'INVALID_LATITUDE');
  }
  if (isNaN(longitude) || longitude < -180 || longitude > 180) {
    return createJsonError('Invalid longitude coordinate: ' + longitude, 'INVALID_LONGITUDE');
  }

  const lock = LockService.getScriptLock();
  try {
    lock.waitLock(30000); // Concurrency guard
  } catch (e) {
    return createJsonError('Server busy recording attendance.', 'LOCK_TIMEOUT');
  }

  try {
    const ss = getSpreadsheet();
    const workersSheet = ss.getSheetByName(CONFIG.SHEET_NAMES.WORKERS);
    const attendanceSheet = ss.getSheetByName(CONFIG.SHEET_NAMES.ATTENDANCE);

    // 3. Verify worker exists and is ACTIVE
    let workerName = auth.userName || workerId;
    let isWorkerActive = false;
    let workerFound = false;
    const workerRows = workersSheet.getDataRange().getValues();
    for (let i = 1; i < workerRows.length; i++) {
      if (String(workerRows[i][0]).trim().toUpperCase() === workerId) {
        workerFound = true;
        workerName = workerRows[i][1];
        isWorkerActive = String(workerRows[i][7]).trim().toUpperCase() === 'ACTIVE';
        break;
      }
    }

    if (!workerFound) {
      logSyncEvent(requestId, workerId, punchType, 'FAILED', 'Worker not found');
      return createJsonError('Worker record not found in system.', 'WORKER_NOT_FOUND');
    }
    if (!isWorkerActive) {
      logSyncEvent(requestId, workerId, punchType, 'FAILED', 'Worker account deactivated');
      return createJsonError('Worker account is inactive. Punch disallowed.', 'WORKER_INACTIVE');
    }

    // 4. IDEMPOTENCY CHECK: Ensure duplicate retry does not create new rows
    const attRows = attendanceSheet.getDataRange().getValues();
    for (let j = 1; j < attRows.length; j++) {
      if (String(attRows[j][0]).trim() === requestId) {
        logSyncEvent(requestId, workerId, punchType, 'DUPLICATE_IGNORED', 'Idempotent response returned');
        return createJsonResponse({
          success: true,
          message: 'Attendance already recorded (Idempotent)',
          data: {
            attendanceId: attRows[j][0],
            workerId: attRows[j][1],
            workerName: attRows[j][2],
            attendanceType: attRows[j][3],
            date: attRows[j][5],
            time: attRows[j][6]
          }
        });
      }
    }

    // 5. Record new attendance entry
    const serverTimestamp = getCurrentServerTime();
    const currentDate = getCurrentDate();
    const currentTime = getCurrentTime();

    attendanceSheet.appendRow([
      requestId, workerId, workerName, punchType, clientTimestamp,
      currentDate, currentTime, latitude, longitude, accuracy, localArea,
      'WORKER_APP', '', '', (data.notes || ''), serverTimestamp, serverTimestamp
    ]);

    logSyncEvent(requestId, workerId, punchType, 'SUCCESS', 'Punch recorded successfully');

    return createJsonResponse({
      success: true,
      message: (punchType === 'PUNCH_IN' ? 'Punch In' : 'Punch Out') + ' recorded successfully',
      data: {
        attendanceId: requestId,
        workerId: workerId,
        workerName: workerName,
        attendanceType: punchType,
        date: currentDate,
        time: currentTime,
        localArea: localArea
      }
    });
  } finally {
    lock.releaseLock();
  }
}

/**
 * Create Manual Attendance (Admin Only)
 */
function handleCreateManualAttendance(data) {
  const auth = verifyTokenAndRole(data.token, 'ADMIN');
  if (!auth.isValid) return createJsonError(auth.message, auth.code);

  const workerId = (data.employeeId || data.workerId || '').trim().toUpperCase();
  const date = (data.date || getCurrentDate()).trim();
  const inTime = (data.inTime || data.punchIn || '').trim();
  const outTime = (data.outTime || data.punchOut || '').trim();
  const notes = (data.notes || data.reason || 'Manual entry by admin').trim();
  const adminId = auth.userId;
  const adminName = auth.userName;
  const lat = parseFloat(data.latitude) || 0.0;
  const lng = parseFloat(data.longitude) || 0.0;
  const acc = parseFloat(data.accuracy) || 0.0;
  const localArea = data.localArea || 'Manual Entry by Admin';

  if (!workerId) return createJsonError('workerId is required.', 'MISSING_WORKER_ID');
  if (!inTime) return createJsonError('Punch in time is required.', 'MISSING_IN_TIME');

  const lock = LockService.getScriptLock();
  try { lock.waitLock(30000); } catch (e) { return createJsonError('Server busy.', 'LOCK_TIMEOUT'); }

  try {
    const ss = getSpreadsheet();
    const workersSheet = ss.getSheetByName(CONFIG.SHEET_NAMES.WORKERS);
    const attendanceSheet = ss.getSheetByName(CONFIG.SHEET_NAMES.ATTENDANCE);

    // Verify worker exists
    let workerName = workerId;
    const workerRows = workersSheet.getDataRange().getValues();
    for (let i = 1; i < workerRows.length; i++) {
      if (String(workerRows[i][0]).trim().toUpperCase() === workerId) {
        workerName = workerRows[i][1];
        break;
      }
    }

    const inRecordId = generateUUID();
    const serverTimestamp = getCurrentServerTime();

    // Insert Manual Punch In Record
    attendanceSheet.appendRow([
      inRecordId, workerId, workerName, 'MANUAL', Date.now(),
      date, inTime, lat, lng, acc, localArea,
      'ADMIN_APP', adminId, adminName, notes, serverTimestamp, serverTimestamp
    ]);

    // Insert Punch Out if supplied
    if (outTime) {
      const outRecordId = generateUUID();
      attendanceSheet.appendRow([
        outRecordId, workerId, workerName, 'PUNCH_OUT', Date.now() + 1000,
        date, outTime, lat, lng, acc, localArea,
        'ADMIN_APP', adminId, adminName, notes, serverTimestamp, serverTimestamp
      ]);
    }

    logSyncEvent(inRecordId, workerId, 'MANUAL', 'SUCCESS', 'Manual entry created by ' + adminId);

    return createJsonResponse({
      success: true,
      message: 'Manual attendance recorded successfully',
      data: {
        attendanceId: inRecordId,
        workerId: workerId,
        workerName: workerName,
        date: date,
        inTime: inTime,
        outTime: outTime
      }
    });
  } finally {
    lock.releaseLock();
  }
}

// ============================================================================
// ATTENDANCE RETRIEVAL & QUERY HANDLERS
// ============================================================================

/**
 * Get Attendance for a Specific Worker (Worker Self-Scoped or Admin)
 */
function handleGetWorkerAttendance(data) {
  const targetWorkerId = (data.employeeId || data.workerId || '').trim().toUpperCase();
  if (!targetWorkerId) return createJsonError('workerId is required.', 'MISSING_WORKER_ID');

  // Verify token: Worker can only fetch their own attendance; Admin can fetch anyone's
  const auth = verifyTokenAndRole(data.token, 'WORKER', targetWorkerId);
  if (!auth.isValid) return createJsonError(auth.message, auth.code);

  const monthFilter = (data.month || '').trim(); // e.g. "2026-08"

  const ss = getSpreadsheet();
  const sheet = ss.getSheetByName(CONFIG.SHEET_NAMES.ATTENDANCE);
  const rows = sheet.getDataRange().getValues();
  const records = [];

  for (let i = rows.length - 1; i >= 1; i--) {
    const row = rows[i];
    if (String(row[1]).trim().toUpperCase() === targetWorkerId) {
      const rowDate = String(row[5]);
      if (!monthFilter || rowDate.indexOf(monthFilter) === 0) {
        records.push(mapRowToAttendance(row));
      }
    }
  }

  return createJsonResponse({
    success: true,
    data: records
  });
}

/**
 * Get All Attendance Records (Admin Only)
 */
function handleGetAllAttendance(data) {
  const auth = verifyTokenAndRole(data.token, 'ADMIN');
  if (!auth.isValid) return createJsonError(auth.message, auth.code);

  const dateFilter = (data.date || '').trim();
  const monthFilter = (data.month || '').trim();
  const workerFilter = (data.workerId || data.employeeId || '').trim().toUpperCase();

  const ss = getSpreadsheet();
  const sheet = ss.getSheetByName(CONFIG.SHEET_NAMES.ATTENDANCE);
  const rows = sheet.getDataRange().getValues();
  const records = [];

  for (let i = rows.length - 1; i >= 1; i--) {
    const row = rows[i];
    const rWorkerId = String(row[1]).trim().toUpperCase();
    const rDate = String(row[5]);

    const matchesWorker = !workerFilter || rWorkerId === workerFilter;
    const matchesDate = !dateFilter || rDate === dateFilter;
    const matchesMonth = !monthFilter || rDate.indexOf(monthFilter) === 0;

    if (matchesWorker && matchesDate && matchesMonth) {
      records.push(mapRowToAttendance(row));
    }
  }

  return createJsonResponse({
    success: true,
    data: records
  });
}

/**
 * Get Today's Live Attendance Records (Scoped by Role)
 */
function handleGetTodayAttendance(data) {
  const auth = verifyTokenAndRole(data.token, 'WORKER');
  if (!auth.isValid) return createJsonError(auth.message, auth.code);

  const today = getCurrentDate();
  const ss = getSpreadsheet();
  const sheet = ss.getSheetByName(CONFIG.SHEET_NAMES.ATTENDANCE);
  const rows = sheet.getDataRange().getValues();
  const records = [];

  for (let i = 1; i < rows.length; i++) {
    const row = rows[i];
    if (String(row[5]) === today) {
      // If WORKER, only return their own records; If ADMIN, return all
      if (auth.role === 'ADMIN' || String(row[1]).trim().toUpperCase() === auth.userId) {
        records.push(mapRowToAttendance(row));
      }
    }
  }

  return createJsonResponse({
    success: true,
    data: records
  });
}

// ============================================================================
// OFFLINE SYNC HANDLERS
// ============================================================================

/**
 * Sync Single Attendance Record from Android Client (Token Protected)
 */
function handleSyncSingleAttendance(data) {
  const punchType = (data.attendanceType || data.type || 'PUNCH_IN').toUpperCase();
  return handleCreatePunch(data, punchType);
}

/**
 * Batch Sync Pending Attendance Records (Token Protected)
 */
function handleSyncBatchAttendance(data) {
  const auth = verifyTokenAndRole(data.token, 'WORKER');
  if (!auth.isValid) return createJsonError(auth.message, auth.code);

  const records = data.records;
  if (!records || !Array.isArray(records) || records.length === 0) {
    return createJsonResponse({
      success: true,
      message: 'No pending records to sync',
      data: { syncedCount: 0 }
    });
  }

  let successCount = 0;
  const results = [];

  for (let i = 0; i < records.length; i++) {
    const item = records[i];
    item.token = data.token; // propagate authenticated token
    const punchType = (item.attendanceType || item.type || 'PUNCH_IN').toUpperCase();
    const res = handleCreatePunch(item, punchType);
    if (res.success) {
      successCount++;
    }
    results.push({
      attendanceId: item.attendanceId || item.id,
      success: res.success,
      message: res.message
    });
  }

  return createJsonResponse({
    success: true,
    message: 'Batch synchronization completed',
    data: {
      syncedCount: successCount,
      totalCount: records.length,
      details: results
    }
  });
}

// ============================================================================
// HELPER UTILITIES
// ============================================================================

function handleHealthCheck() {
  return createJsonResponse({
    success: true,
    message: 'SMART ATTENDANCE backend operational',
    data: {
      serverTime: getCurrentServerTime(),
      timezone: CONFIG.TIMEZONE
    }
  });
}

function getCurrentServerTime() {
  return Utilities.formatDate(new Date(), CONFIG.TIMEZONE, "yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
}

function getCurrentDate() {
  return Utilities.formatDate(new Date(), CONFIG.TIMEZONE, "yyyy-MM-dd");
}

function getCurrentTime() {
  return Utilities.formatDate(new Date(), CONFIG.TIMEZONE, "HH:mm:ss");
}

function generateUUID() {
  return Utilities.getUuid();
}

function logSyncEvent(requestId, workerId, operation, status, message) {
  try {
    const ss = getSpreadsheet();
    const sheet = ss.getSheetByName(CONFIG.SHEET_NAMES.SYNCLOG);
    if (sheet) {
      const syncId = 'log-' + Date.now() + '-' + Math.floor(Math.random() * 1000);
      sheet.appendRow([
        syncId, requestId, workerId, operation, status, message, getCurrentServerTime()
      ]);
    }
  } catch (_e) {}
}

function mapRowToAttendance(row) {
  return {
    id: row[0],
    employeeId: row[1],
    employeeName: row[2],
    attendanceType: row[3],
    timestamp: row[4],
    date: row[5],
    time: row[6],
    latitude: row[7],
    longitude: row[8],
    accuracy: row[9],
    localArea: row[10],
    syncSource: row[11],
    createdByAdminId: row[12] || null,
    createdByAdminName: row[13] || null,
    notes: row[14] || null,
    createdAt: row[15]
  };
}

function createJsonResponse(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}

function createJsonError(message, errorCode) {
  return ContentService
    .createTextOutput(JSON.stringify({
      success: false,
      isSuccess: false,
      message: message,
      errorCode: errorCode || 'ERROR'
    }))
    .setMimeType(ContentService.MimeType.JSON);
}
