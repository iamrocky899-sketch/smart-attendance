/**
 * ============================================================================
 * SMART ATTENDANCE — Security & Backend Integration Test Suite
 * Run these functions directly in the Google Apps Script editor.
 * 
 * Fully isolated & repeatable: safe to run multiple times without data accumulation
 * or state pollution.
 * ============================================================================
 */

function runAllBackendTests() {
  Logger.log('====================================================================');
  Logger.log('STARTING SMART ATTENDANCE SECURITY & BACKEND INTEGRATION TEST SUITE');
  Logger.log('====================================================================');

  let passed = 0;
  let failed = 0;

  function assert(testName, condition, detail) {
    if (condition) {
      Logger.log(' [PASS] ' + testName);
      passed++;
    } else {
      Logger.log('❌ [FAIL] ' + testName + ' => ' + (detail || 'Assertion failed'));
      failed++;
    }
  }

  // --------------------------------------------------------------------------
  // PRE-TEST CLEANUP (Ensures Clean Isolated Test State)
  // --------------------------------------------------------------------------
  cleanupTestFixtures();

  // --------------------------------------------------------------------------
  // GROUP 1: SELF-INITIALIZATION & HEALTH CHECK
  // --------------------------------------------------------------------------
  try {
    const setupMsg = setupSmartAttendance();
    assert('1. setupSmartAttendance() Idempotent Initialization', typeof setupMsg === 'string' && setupMsg.indexOf('successfully') !== -1);
  } catch (e) { assert('1. setupSmartAttendance()', false, e.message); }

  try {
    const res = handleHealthCheck();
    const json = JSON.parse(res.getContent());
    assert('1A. Health Check API Online & Asia/Kolkata Timezone', json.success === true && json.data.timezone === 'Asia/Kolkata');
  } catch (e) { assert('1A. Health Check API', false, e.message); }

  try {
    const n1 = normalizeMobileNumber('6003090734');
    const n2 = normalizeMobileNumber('+91 60030 90734');
    const n3 = normalizeMobileNumber('916003090734');
    const n4 = normalizeMobileNumber('0916003090734');
    const n5 = normalizeMobileNumber('06003090734');
    const n6 = normalizeMobileNumber(6003090734);
    assert('1B. normalizeMobileNumber() Canonical 10-Digit Standard', 
      n1 === '6003090734' && 
      n2 === '6003090734' && 
      n3 === '6003090734' && 
      n4 === '6003090734' && 
      n5 === '6003090734' && 
      n6 === '6003090734'
    );
  } catch (e) { assert('1B. normalizeMobileNumber()', false, e.message); }

  // --------------------------------------------------------------------------
  // GROUP 2: ADMIN AUTHENTICATION (MOBILE NUMBER + PASSWORD)
  // --------------------------------------------------------------------------
  let adminToken = '';
  try {
    const res = handleAdminLogin({ identifier: '6003090734', password: '12345' });
    const json = JSON.parse(res.getContent());
    assert('2. Admin Login using Registered Mobile (6003090734)', json.success === true && json.data.role === 'ADMIN' && json.data.adminId === 'ADMIN-0001' && !!json.data.token);
    assert('2A. Admin Login does NOT return password/hash in payload', json.data && json.data.password === undefined && json.data.passwordHash === undefined);
    assert('2B. Admin Login returns normalized mobile number ("6003090734")', json.data && json.data.mobileNumber === '6003090734');
    adminToken = json.data ? json.data.token : '';
  } catch (e) { assert('2. Admin Login Mobile', false, e.message); }

  try {
    const res = handleAdminLogin({ identifier: '+91 60030 90734', password: '12345' });
    const json = JSON.parse(res.getContent());
    assert('2C. Admin Login using Formatted Mobile (+91 60030 90734)', json.success === true && json.data.adminId === 'ADMIN-0001' && json.data.mobileNumber === '6003090734');
  } catch (e) { assert('2C. Admin Login Formatted Mobile', false, e.message); }

  try {
    const res = handleAdminLogin({ identifier: '0916003090734', password: '12345' });
    const json = JSON.parse(res.getContent());
    assert('2D. Admin Login using 091-prefixed Mobile (0916003090734)', json.success === true && json.data.adminId === 'ADMIN-0001' && json.data.mobileNumber === '6003090734');
  } catch (e) { assert('2D. Admin Login 091-prefixed Mobile', false, e.message); }

  try {
    const res = handleAdminLogin({ identifier: 'ADMIN-0001', password: '12345' });
    const json = JSON.parse(res.getContent());
    assert('2E. Admin Login using Admin ID (ADMIN-0001)', json.success === true && json.data.adminId === 'ADMIN-0001' && json.data.mobileNumber === '6003090734');
  } catch (e) { assert('2E. Admin Login Admin ID', false, e.message); }

  try {
    const res = handleAdminLogin({ identifier: '6003090734', password: 'WrongAdminPassword' });
    const json = JSON.parse(res.getContent());
    assert('3. Admin Login Rejection on Invalid Password', json.success === false && json.errorCode === 'INVALID_CREDENTIALS');
  } catch (e) { assert('3. Admin Login Rejection', false, e.message); }

  // --------------------------------------------------------------------------
  // GROUP 3: WORKER CREATION & AUTO-INCREMENT ID GENERATION
  // --------------------------------------------------------------------------
  let worker1Id = '';
  let worker2Id = '';

  try {
    const res1 = handleCreateWorker({
      token: adminToken,
      fullName: '[TEST] Security Worker One',
      mobileNumber: '9988112233',
      workplaceName: 'Facility Alpha',
      designation: 'Staff',
      joiningDate: '2026-08-23',
      isActive: true
    });
    const json1 = JSON.parse(res1.getContent());
    worker1Id = json1.data ? json1.data.employeeId : '';
    assert('4. Worker 1 Creation with Auto ID (EMP-xxxx)', json1.success === true && !!worker1Id && worker1Id.startsWith('EMP-'), json1.message);

    const res2 = handleCreateWorker({
      token: adminToken,
      fullName: '[TEST] Security Worker Two',
      mobileNumber: '9988445566',
      workplaceName: 'Facility Beta',
      designation: 'Staff',
      joiningDate: '2026-08-23',
      isActive: true
    });
    const json2 = JSON.parse(res2.getContent());
    worker2Id = json2.data ? json2.data.employeeId : '';
    assert('5. Worker 2 Creation with Auto ID (EMP-xxxx)', json2.success === true && !!worker2Id && worker2Id.startsWith('EMP-') && worker2Id !== worker1Id, json2.message);
  } catch (e) { assert('Worker Creation', false, e.message); }

  // --------------------------------------------------------------------------
  // GROUP 4: WORKER DUAL LOGIN & SECURITY SANITIZATION
  // --------------------------------------------------------------------------
  let worker1Token = '';
  let worker2Token = '';

  try {
    const res = handleWorkerLogin({ identifier: worker1Id, password: '12345' });
    const json = JSON.parse(res.getContent());
    assert('6. Worker 1 Login using Employee ID', json.success === true && json.data.employeeId === worker1Id && json.data.mobileNumber === '9988112233');
    assert('6A. Worker Login does NOT return password/hash', json.data && json.data.password === undefined && json.data.passwordHash === undefined);
    worker1Token = json.data ? json.data.token : '';
  } catch (e) { assert('6. Worker 1 Login', false, e.message); }

  try {
    const res = handleWorkerLogin({ identifier: '+91 99881-12233', password: '12345' });
    const json = JSON.parse(res.getContent());
    assert('7. Worker 1 Login using Formatted Mobile (+91 99881-12233)', json.success === true && json.data.employeeId === worker1Id);
  } catch (e) { assert('7. Worker 1 Login Mobile', false, e.message); }

  try {
    const res = handleWorkerLogin({ identifier: worker1Id, password: 'WrongWorkerPassword' });
    const json = JSON.parse(res.getContent());
    assert('7A. Worker Login Rejection on Invalid Password', json.success === false && json.errorCode === 'INVALID_CREDENTIALS');
  } catch (e) { assert('7A. Worker Invalid Password', false, e.message); }

  try {
    const res = handleWorkerLogin({ identifier: worker2Id, password: '12345' });
    const json = JSON.parse(res.getContent());
    worker2Token = json.data ? json.data.token : '';
  } catch (e) {}

  // --------------------------------------------------------------------------
  // GROUP 5: TOKEN INTEGRITY & CRYPTOGRAPHIC VALIDATION
  // --------------------------------------------------------------------------
  // 5.1. Missing Token Rejection
  try {
    const res = handleListWorkers({ token: '' });
    const json = JSON.parse(res.getContent());
    assert('8. Missing Token Rejected on Protected Endpoint', json.success === false && json.errorCode === 'MISSING_TOKEN');
  } catch (e) { assert('8. Missing Token', false, e.message); }

  // 5.2. Malformed / Random Token Rejection
  try {
    const res = handleListWorkers({ token: 'NotAValidTokenString' });
    const json = JSON.parse(res.getContent());
    assert('9. Malformed Token Rejected', json.success === false && (json.errorCode === 'INVALID_TOKEN' || json.errorCode === 'TOKEN_VERIFICATION_ERROR'));
  } catch (e) { assert('9. Malformed Token', false, e.message); }

  // 5.3. Tampered Token Rejection (Modified Payload without Valid HMAC)
  try {
    const parts = (worker1Token || 'a.b').split('.');
    const fakePayloadJson = JSON.stringify({ sub: 'admin', role: 'ADMIN', exp: Date.now() + 1000000 });
    const tamperedBase64 = Utilities.base64EncodeWebSafe(fakePayloadJson);
    const tamperedToken = tamperedBase64 + '.' + parts[1];
    const res = handleListWorkers({ token: tamperedToken });
    const json = JSON.parse(res.getContent());
    assert('10. Tampered Token (Elevated to Admin) Rejected via HMAC Signature', json.success === false && json.errorCode === 'INVALID_TOKEN_SIGNATURE');
  } catch (e) { assert('10. Tampered Token', false, e.message); }

  // 5.4. Expired Token Rejection
  try {
    const expiredPayload = { sub: worker1Id || 'EMP-0001', role: 'WORKER', name: 'Test', iat: Date.now() - 1000000, exp: Date.now() - 5000 };
    const expiredPayloadJson = JSON.stringify(expiredPayload);
    const expiredBase64 = Utilities.base64EncodeWebSafe(expiredPayloadJson);
    const expiredSig = computeHmacSignature(expiredBase64);
    const expiredToken = expiredBase64 + '.' + expiredSig;

    const res = handleGetWorkerAttendance({ token: expiredToken, workerId: worker1Id || 'EMP-0001' });
    const json = JSON.parse(res.getContent());
    assert('11. Expired Token Rejected with TOKEN_EXPIRED', json.success === false && json.errorCode === 'TOKEN_EXPIRED');
  } catch (e) { assert('11. Expired Token', false, e.message); }

  // --------------------------------------------------------------------------
  // GROUP 6: ROLE-BASED ACCESS CONTROL (RBAC) & PRIVILEGE ESCALATION PREVENTION
  // --------------------------------------------------------------------------
  // 6.1. Worker Token accessing Admin-only endpoint (listWorkers)
  try {
    const res = handleListWorkers({ token: worker1Token });
    const json = JSON.parse(res.getContent());
    assert('12. Worker Token Rejected from Admin Endpoint (listWorkers)', json.success === false && json.errorCode === 'FORBIDDEN');
  } catch (e) { assert('12. Worker on Admin Endpoint', false, e.message); }

  // 6.2. Worker Token attempting to create new worker
  try {
    const res = handleCreateWorker({
      token: worker1Token,
      fullName: '[TEST] Illegal Worker',
      mobileNumber: '9111111111'
    });
    const json = JSON.parse(res.getContent());
    assert('13. Worker Token Rejected from Creating Workers', json.success === false && json.errorCode === 'FORBIDDEN');
  } catch (e) { assert('13. Worker Creating Worker', false, e.message); }

  // --------------------------------------------------------------------------
  // GROUP 7: IDENTITY IMPERSONATION & USER-SCOPING PREVENTION
  // --------------------------------------------------------------------------
  // 7.1. Worker 1 attempting to view Worker 2 attendance
  try {
    const res = handleGetWorkerAttendance({
      token: worker1Token,
      workerId: worker2Id || 'EMP-9999'
    });
    const json = JSON.parse(res.getContent());
    assert('14. Worker 1 Rejected from Accessing Worker 2 Attendance', json.success === false && json.errorCode === 'FORBIDDEN_USER_MISMATCH');
  } catch (e) { assert('14. Worker Accessing Other Worker Attendance', false, e.message); }

  // 7.2. Worker 1 attempting to punch attendance on behalf of Worker 2
  try {
    const fakePunchId = 'test-punch-fake-' + Date.now();
    const res = handleCreatePunch({
      token: worker1Token,
      workerId: worker2Id || 'EMP-9999',
      attendanceId: fakePunchId,
      latitude: 26.758412,
      longitude: 93.125678,
      accuracy: 10.0,
      localArea: 'Gameri'
    }, 'PUNCH_IN');
    const json = JSON.parse(res.getContent());
    assert('15. Worker 1 Blocked from Punching for Worker 2', json.success === false && json.errorCode === 'FORBIDDEN_USER_MISMATCH');
  } catch (e) { assert('15. Worker Impersonating Punch', false, e.message); }

  // --------------------------------------------------------------------------
  // GROUP 8: LEGITIMATE WORKER ATTENDANCE & IDEMPOTENCY
  // --------------------------------------------------------------------------
  const validPunchInId = 'test-punch-legit-' + Date.now();
  try {
    const res = handleCreatePunch({
      token: worker1Token,
      workerId: worker1Id,
      attendanceId: validPunchInId,
      latitude: 26.758412,
      longitude: 93.125678,
      accuracy: 12.0,
      localArea: 'Gameri HS'
    }, 'PUNCH_IN');
    const json = JSON.parse(res.getContent());
    assert('16. Legitimate Worker 1 Punch In Recorded', json.success === true && json.data.attendanceType === 'PUNCH_IN');
  } catch (e) { assert('16. Valid Punch In', false, e.message); }

  // Replay identical requestId (Idempotency)
  try {
    const res = handleCreatePunch({
      token: worker1Token,
      workerId: worker1Id,
      attendanceId: validPunchInId,
      latitude: 26.758412,
      longitude: 93.125678,
      accuracy: 12.0,
      localArea: 'Gameri HS'
    }, 'PUNCH_IN');
    const json = JSON.parse(res.getContent());
    assert('17. Replayed Request Idempotency (Duplicate Ignored Safely)', json.success === true && json.message.indexOf('Idempotent') !== -1);
  } catch (e) { assert('17. Idempotency Check', false, e.message); }

  // --------------------------------------------------------------------------
  // GROUP 9: ADMIN ATTENDANCE ACCESS & MANAGEMENT
  // --------------------------------------------------------------------------
  try {
    const res = handleGetAllAttendance({ token: adminToken });
    const json = JSON.parse(res.getContent());
    assert('18. Admin Can Access All Attendance Records', json.success === true && Array.isArray(json.data) && json.data.length >= 1);
  } catch (e) { assert('18. Admin Get All Attendance', false, e.message); }

  try {
    const res = handleCreateManualAttendance({
      token: adminToken,
      employeeId: worker1Id,
      date: '2026-08-23',
      inTime: '08:30:00',
      outTime: '17:00:00',
      notes: 'Verified on duty'
    });
    const json = JSON.parse(res.getContent());
    assert('19. Admin Can Record Manual Attendance with Admin Attribution', json.success === true && json.data.workerId === worker1Id);
  } catch (e) { assert('19. Admin Manual Attendance', false, e.message); }

  // --------------------------------------------------------------------------
  // GROUP 10: DUPLICATE MOBILE & DEACTIVATION
  // --------------------------------------------------------------------------
  try {
    const res = handleCreateWorker({
      token: adminToken,
      fullName: '[TEST] Duplicate Mobile Worker',
      mobileNumber: '9988112233', // Duplicate of Worker 1
      workplaceName: 'Alpha',
      designation: 'Staff'
    });
    const json = JSON.parse(res.getContent());
    assert('20. Duplicate Mobile Number Creation Rejected', json.success === false && json.errorCode === 'DUPLICATE_MOBILE_NUMBER');
  } catch (e) { assert('20. Duplicate Mobile', false, e.message); }

  try {
    handleSetWorkerStatus({ token: adminToken, employeeId: worker1Id, isActive: false });
    const res = handleWorkerLogin({ identifier: worker1Id, password: '12345' });
    const json = JSON.parse(res.getContent());
    assert('21. Deactivated Worker Account Login Blocked', json.success === false && json.errorCode === 'WORKER_INACTIVE');
  } catch (e) { assert('21. Deactivated Worker Login Block', false, e.message); }

  // --------------------------------------------------------------------------
  // POST-TEST CLEANUP
  // --------------------------------------------------------------------------
  cleanupTestFixtures();

  Logger.log('====================================================================');
  Logger.log('SECURITY & INTEGRATION TEST RESULTS: ' + passed + ' PASSED, ' + failed + ' FAILED');
  Logger.log('====================================================================');
}

/**
 * Safely removes test fixtures generated during automated test runs.
 * Leaves all legitimate production workers (e.g. EMP-0001 - Rahul Das), attendance, admins, and settings intact.
 */
function cleanupTestFixtures() {
  try {
    const ss = getSpreadsheet();
    const testMobiles = ['9988112233', '9988445566', '9111111111', '9999900001', '9999900002'];

    // 1. Workers Sheet - Clean test workers
    const workersSheet = ss.getSheetByName(CONFIG.SHEET_NAMES.WORKERS);
    if (workersSheet && workersSheet.getLastRow() > 1) {
      const rows = workersSheet.getDataRange().getValues();
      for (let i = rows.length - 1; i >= 1; i--) {
        const row = rows[i];
        const empId = String(row[0]).trim();
        const name = String(row[1]).trim();
        const mobile = normalizeMobileNumber(row[2]);

        // Never delete default demo worker EMP-0001 or production non-test workers
        if (empId !== 'EMP-0001' && (name.indexOf('[TEST]') !== -1 || testMobiles.indexOf(mobile) !== -1 || name.indexOf('Security Worker') !== -1 || name.indexOf('Duplicate Mobile') !== -1 || name.indexOf('Illegal Worker') !== -1)) {
          workersSheet.deleteRow(i + 1);
        }
      }
    }

    // 2. Attendance Sheet - Clean test punches
    const attendanceSheet = ss.getSheetByName(CONFIG.SHEET_NAMES.ATTENDANCE);
    if (attendanceSheet && attendanceSheet.getLastRow() > 1) {
      const rows = attendanceSheet.getDataRange().getValues();
      for (let i = rows.length - 1; i >= 1; i--) {
        const row = rows[i];
        const attId = String(row[0]).trim();
        const name = String(row[2]).trim();

        if (attId.startsWith('punch-') || attId.startsWith('test-') || name.indexOf('[TEST]') !== -1 || name.indexOf('Security Worker') !== -1) {
          attendanceSheet.deleteRow(i + 1);
        }
      }
    }

    // 3. SyncLog Sheet - Clean test logs
    const syncLogSheet = ss.getSheetByName(CONFIG.SHEET_NAMES.SYNCLOG);
    if (syncLogSheet && syncLogSheet.getLastRow() > 1) {
      const rows = syncLogSheet.getDataRange().getValues();
      for (let i = rows.length - 1; i >= 1; i--) {
        const row = rows[i];
        const reqId = String(row[1]).trim();

        if (reqId.startsWith('punch-') || reqId.startsWith('test-') || reqId.startsWith('sync-test-')) {
          syncLogSheet.deleteRow(i + 1);
        }
      }
    }
  } catch (e) {
    Logger.log('cleanupTestFixtures notice: ' + e.message);
  }
}
