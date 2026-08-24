package com.attendancehalim.smartattendance.data.local.session

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.attendancehalim.smartattendance.domain.model.UserRole
import com.attendancehalim.smartattendance.domain.model.UserSession
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session_prefs")

class SessionManager(private val context: Context) {

    private object PreferencesKeys {
        val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val KEY_EMPLOYEE_ID = stringPreferencesKey("employee_id")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_MOBILE_NUMBER = stringPreferencesKey("mobile_number")
        val KEY_ROLE = stringPreferencesKey("user_role")
        val KEY_WORKPLACE_NAME = stringPreferencesKey("workplace_name")
        val KEY_PHOTO_URL = stringPreferencesKey("photo_url")
        val KEY_STATUS = stringPreferencesKey("status")
        val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        val KEY_TOKEN_EXPIRY = longPreferencesKey("token_expiry")
    }

    val sessionFlow: Flow<UserSession> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val isLoggedIn = preferences[PreferencesKeys.KEY_IS_LOGGED_IN] ?: false
            val employeeId = preferences[PreferencesKeys.KEY_EMPLOYEE_ID] ?: ""
            val userName = preferences[PreferencesKeys.KEY_USER_NAME] ?: ""
            val mobileNumber = preferences[PreferencesKeys.KEY_MOBILE_NUMBER] ?: ""
            val roleString = preferences[PreferencesKeys.KEY_ROLE] ?: UserRole.WORKER.name
            val role = UserRole.fromString(roleString)
            val workplaceName = preferences[PreferencesKeys.KEY_WORKPLACE_NAME] ?: ""
            val photoUrl = preferences[PreferencesKeys.KEY_PHOTO_URL] ?: ""
            val status = preferences[PreferencesKeys.KEY_STATUS] ?: "ACTIVE"
            val authToken = preferences[PreferencesKeys.KEY_AUTH_TOKEN] ?: ""
            val tokenExpiry = preferences[PreferencesKeys.KEY_TOKEN_EXPIRY] ?: 0L

            UserSession(
                isLoggedIn = isLoggedIn,
                employeeId = employeeId,
                userName = userName,
                mobileNumber = mobileNumber,
                role = role,
                workplaceName = workplaceName,
                photoUrl = photoUrl,
                status = status,
                authToken = authToken,
                tokenExpiry = tokenExpiry
            )
        }

    suspend fun saveSession(session: UserSession) {
        val calculatedExpiry = if (session.tokenExpiry > 0L) {
            session.tokenExpiry
        } else {
            extractExpiryFromToken(session.authToken)
        }

        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_IS_LOGGED_IN] = session.isLoggedIn
            preferences[PreferencesKeys.KEY_EMPLOYEE_ID] = session.employeeId
            preferences[PreferencesKeys.KEY_USER_NAME] = session.userName
            preferences[PreferencesKeys.KEY_MOBILE_NUMBER] = session.mobileNumber
            preferences[PreferencesKeys.KEY_ROLE] = session.role.name
            preferences[PreferencesKeys.KEY_WORKPLACE_NAME] = session.workplaceName
            preferences[PreferencesKeys.KEY_PHOTO_URL] = session.photoUrl
            preferences[PreferencesKeys.KEY_STATUS] = session.status
            preferences[PreferencesKeys.KEY_AUTH_TOKEN] = session.authToken
            preferences[PreferencesKeys.KEY_TOKEN_EXPIRY] = calculatedExpiry
        }
    }

    suspend fun updateRole(role: UserRole) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_ROLE] = role.name
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun getActiveSession(): UserSession {
        val session = sessionFlow.first()
        if (session.isLoggedIn && session.isTokenExpired) {
            clearSession()
            return UserSession()
        }
        return session
    }

    private fun extractExpiryFromToken(token: String): Long {
        if (token.isBlank()) return 0L
        return try {
            val parts = token.split(".")
            if (parts.size >= 2) {
                val payloadJson = String(Base64.decode(parts[0], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING), Charsets.UTF_8)
                val jsonObject = JsonParser.parseString(payloadJson).asJsonObject
                if (jsonObject.has("exp")) {
                    jsonObject.get("exp").asLong
                } else {
                    0L
                }
            } else {
                0L
            }
        } catch (_: Exception) {
            0L
        }
    }
}
