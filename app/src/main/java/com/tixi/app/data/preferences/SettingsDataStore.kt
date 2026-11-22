package com.tixi.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tixi.app.data.security.TokenStore
import com.tixi.app.domain.model.ParseParams
import com.tixi.app.domain.model.ThemeMode
import com.tixi.app.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 设置项存储：Token 走 [TokenStore]（KeyStore 加密），其余走 Preferences DataStore。
 * Token 只在发起请求时读取，绝不写日志。
 */
@Singleton
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val tokenStore: TokenStore,
) : SettingsRepository {

    private val tokenConfigured = MutableStateFlow(tokenStore.hasToken())

    /** DataStore 读盘失败（文件损坏等）时回落默认值，避免设置页直接崩 */
    private val preferences: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    // ---- Token ----

    override fun observeTokenConfigured(): Flow<Boolean> = tokenConfigured.asStateFlow()

    override suspend fun getToken(): String? = withContext(Dispatchers.IO) { tokenStore.read() }

    override suspend fun saveToken(token: String) = withContext(Dispatchers.IO) {
        tokenStore.write(token)
        tokenConfigured.value = tokenStore.hasToken()
    }

    override suspend fun clearToken() = withContext(Dispatchers.IO) {
        tokenStore.clear()
        tokenConfigured.value = false
    }

    // ---- 主题 ----

    override fun observeThemeMode(): Flow<ThemeMode> =
        preferences.map { prefs -> ThemeMode.fromKey(prefs[KEY_THEME_MODE]) }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs -> prefs[KEY_THEME_MODE] = mode.key }
    }

    // ---- 解析参数 ----

    override fun observeParseParams(): Flow<ParseParams> = preferences.map { prefs ->
        ParseParams(
            language = prefs[KEY_LANGUAGE] ?: ParseParams.Default.language,
            enableTable = prefs[KEY_ENABLE_TABLE] ?: ParseParams.Default.enableTable,
            enableFormula = prefs[KEY_ENABLE_FORMULA] ?: ParseParams.Default.enableFormula,
            modelVersion = prefs[KEY_MODEL_VERSION] ?: ParseParams.Default.modelVersion,
            pollIntervalMs = prefs[KEY_POLL_INTERVAL_MS] ?: ParseParams.Default.pollIntervalMs,
        )
    }

    override suspend fun setParseParams(params: ParseParams) {
        dataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = params.language
            prefs[KEY_ENABLE_TABLE] = params.enableTable
            prefs[KEY_ENABLE_FORMULA] = params.enableFormula
            prefs[KEY_MODEL_VERSION] = params.modelVersion
            prefs[KEY_POLL_INTERVAL_MS] = params.pollIntervalMs
        }
    }

    // ---- 引导页 ----

    override fun observeOnboardingComplete(): Flow<Boolean> =
        preferences.map { prefs -> prefs[KEY_ONBOARDING_COMPLETE] ?: false }

    override suspend fun setOnboardingComplete(done: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_ONBOARDING_COMPLETE] = done }
    }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_ENABLE_TABLE = booleanPreferencesKey("enable_table")
        val KEY_ENABLE_FORMULA = booleanPreferencesKey("enable_formula")
        val KEY_MODEL_VERSION = stringPreferencesKey("model_version")
        val KEY_POLL_INTERVAL_MS = longPreferencesKey("poll_interval_ms")
    }
}
