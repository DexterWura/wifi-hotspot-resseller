package com.clouderp.wifireseller.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "wifi_reseller_session")

class SessionStore(private val context: Context) {
    private val tokenKey = stringPreferencesKey("token")
    private val companyIdKey = stringPreferencesKey("company_uuid")
    private val companyNameKey = stringPreferencesKey("company_name")
    private val userNameKey = stringPreferencesKey("user_name")
    private val userEmailKey = stringPreferencesKey("user_email")

    val session: Flow<SessionSnapshot> = context.dataStore.data.map { prefs ->
        SessionSnapshot(
            token = prefs[tokenKey],
            companyUuid = prefs[companyIdKey],
            companyName = prefs[companyNameKey],
            userName = prefs[userNameKey],
            userEmail = prefs[userEmailKey],
        )
    }

    suspend fun saveLogin(token: String, userName: String?, userEmail: String?) {
        context.dataStore.edit { prefs ->
            prefs[tokenKey] = token
            if (userName != null) prefs[userNameKey] = userName else prefs.remove(userNameKey)
            if (userEmail != null) prefs[userEmailKey] = userEmail else prefs.remove(userEmailKey)
            prefs.remove(companyIdKey)
            prefs.remove(companyNameKey)
        }
    }

    suspend fun saveCompany(uuid: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[companyIdKey] = uuid
            prefs[companyNameKey] = name
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}

data class SessionSnapshot(
    val token: String?,
    val companyUuid: String?,
    val companyName: String?,
    val userName: String?,
    val userEmail: String?,
) {
    val isLoggedIn: Boolean get() = !token.isNullOrBlank()
    val hasCompany: Boolean get() = !companyUuid.isNullOrBlank()
}
