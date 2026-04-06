package cn.reddragon.eportal.nt.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.IOException

private const val DATASTORE_NAME = "campus_account_datastore"
private val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

data class StoredAccountState(
    val accounts: List<AccountItem>,
    val selectedAccountId: String?,
    val selectedServiceTypeName: String,
    val pollingIntervalSeconds: Int,
    val autoLoginWhenOffline: Boolean,
    val autoLoginStart: Boolean,
)

class AccountPreferencesStore(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
    }
    
    val accountStateFlow: Flow<StoredAccountState> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val accounts = decodeAccounts(preferences[KEY_ACCOUNTS_JSON])
            val selectedAccountId = preferences[KEY_SELECTED_ACCOUNT_ID]
            val selectedServiceTypeName = preferences[KEY_SELECTED_SERVICE_TYPE_NAME] ?: DEFAULT_SELECTED_SERVICE_TYPE
            val pollingIntervalSeconds =
                (preferences[KEY_POLLING_INTERVAL_SECONDS] ?: DEFAULT_POLLING_INTERVAL_SECONDS)
                    .coerceIn(MIN_POLLING_INTERVAL_SECONDS, MAX_POLLING_INTERVAL_SECONDS)
            StoredAccountState(
                accounts = accounts,
                selectedAccountId = selectedAccountId,
                selectedServiceTypeName = selectedServiceTypeName,
                pollingIntervalSeconds = pollingIntervalSeconds,
                autoLoginWhenOffline = preferences[KEY_AUTO_LOGIN_WHEN_OFFLINE] ?: true,
                autoLoginStart = preferences[KEY_AUTO_LOGIN_START] ?: true,
            )
        }
    
    suspend fun saveAccountState(accounts: List<AccountItem>, selectedAccountId: String?) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ACCOUNTS_JSON] = encodeAccounts(accounts)
            if (selectedAccountId.isNullOrBlank()) {
                preferences.remove(KEY_SELECTED_ACCOUNT_ID)
            } else {
                preferences[KEY_SELECTED_ACCOUNT_ID] = selectedAccountId
            }
        }
    }
    
    suspend fun savePollingIntervalSeconds(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_POLLING_INTERVAL_SECONDS] =
                seconds.coerceIn(MIN_POLLING_INTERVAL_SECONDS, MAX_POLLING_INTERVAL_SECONDS)
        }
    }
    
    suspend fun saveAutoLoginWhenOffline(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_LOGIN_WHEN_OFFLINE] = enabled
        }
    }
    
    suspend fun saveAutoLoginStart(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_LOGIN_START] = enabled
        }
    }
    
    suspend fun saveSelectedServiceTypeName(serviceTypeName: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_SERVICE_TYPE_NAME] = serviceTypeName
        }
    }
    
    private fun decodeAccounts(raw: String?): List<AccountItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(ListSerializer(AccountItem.serializer()), raw) }
            .getOrDefault(emptyList())
            .filter { it.studentId.isNotBlank() }
    }
    
    private fun encodeAccounts(accounts: List<AccountItem>): String {
        return json.encodeToString(ListSerializer(AccountItem.serializer()), accounts)
    }
    
    companion object {
        private const val DEFAULT_POLLING_INTERVAL_SECONDS = 10
        const val MIN_POLLING_INTERVAL_SECONDS = 0
        const val MAX_POLLING_INTERVAL_SECONDS = 60
        private const val DEFAULT_SELECTED_SERVICE_TYPE = "WAN"
        
        private val KEY_ACCOUNTS_JSON: Preferences.Key<String> = stringPreferencesKey("accounts_json")
        private val KEY_SELECTED_ACCOUNT_ID: Preferences.Key<String> =
            stringPreferencesKey("selected_account_id")
        private val KEY_SELECTED_SERVICE_TYPE_NAME: Preferences.Key<String> =
            stringPreferencesKey("selected_service_type_name")
        private val KEY_POLLING_INTERVAL_SECONDS: Preferences.Key<Int> =
            intPreferencesKey("polling_interval_seconds")
        private val KEY_AUTO_LOGIN_WHEN_OFFLINE: Preferences.Key<Boolean> =
            booleanPreferencesKey("auto_login_when_offline")
        private val KEY_AUTO_LOGIN_START: Preferences.Key<Boolean> =
            booleanPreferencesKey("auto_login_start")
    }
}
