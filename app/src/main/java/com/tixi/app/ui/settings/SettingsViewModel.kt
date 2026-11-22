package com.tixi.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tixi.app.domain.model.Category
import com.tixi.app.domain.model.ParseParams
import com.tixi.app.domain.model.QuestionTypeEntry
import com.tixi.app.domain.model.ThemeMode
import com.tixi.app.domain.repository.CategoryRepository
import com.tixi.app.domain.repository.ImportRepository
import com.tixi.app.domain.repository.QuestionRepository
import com.tixi.app.domain.repository.QuestionTypeRepository
import com.tixi.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val tokenInput: String = "",
    val configured: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val enableTable: Boolean = true,
    val enableFormula: Boolean = true,
    val language: String = "ch",
    val pollSeconds: Int = 3,
    val types: List<QuestionTypeEntry> = emptyList(),
    val categories: List<Category> = emptyList(),
    val uncategorizedCount: Int = 0,
    val busy: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val typeRepository: QuestionTypeRepository,
    private val categoryRepository: CategoryRepository,
    private val questionRepository: QuestionRepository,
    private val importRepository: ImportRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private var tokenLoaded = false

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.observeTokenConfigured(),
                settingsRepository.observeThemeMode(),
                settingsRepository.observeParseParams(),
            ) { configured, theme, params -> Triple(configured, theme, params) }
                .collect { (configured, theme, params) ->
                    if (configured && !tokenLoaded) {
                        tokenLoaded = true
                        val token = settingsRepository.getToken().orEmpty()
                        _state.update { it.copy(tokenInput = token) }
                    }
                    if (!configured) {
                        tokenLoaded = false
                        _state.update { it.copy(tokenInput = "") }
                    }
                    _state.update {
                        it.copy(
                            configured = configured,
                            themeMode = theme,
                            enableTable = params.enableTable,
                            enableFormula = params.enableFormula,
                            language = params.language,
                            pollSeconds = (params.pollIntervalMs / 1000L).toInt().coerceAtLeast(1),
                        )
                    }
                }
        }
        viewModelScope.launch {
            typeRepository.observeTypes().collect { types ->
                _state.update { it.copy(types = types) }
            }
        }
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { categories ->
                _state.update { it.copy(categories = categories) }
            }
        }
        viewModelScope.launch {
            questionRepository.observeAll().collect { questions ->
                _state.update { it.copy(uncategorizedCount = questions.count { q -> q.categoryId == null }) }
            }
        }
    }

    fun updateToken(value: String) = _state.update { it.copy(tokenInput = value) }

    fun saveToken() {
        val token = _state.value.tokenInput.trim()
        if (token.isEmpty()) {
            _state.update { it.copy(message = "Token 不能为空") }
            return
        }
        viewModelScope.launch {
            settingsRepository.saveToken(token)
            _state.update { it.copy(message = "Token 已加密保存") }
        }
    }

    fun clearToken() {
        viewModelScope.launch {
            settingsRepository.clearToken()
            _state.update { it.copy(tokenInput = "", message = "Token 已删除") }
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setEnableTable(enabled: Boolean) = updateParams { it.copy(enableTable = enabled) }
    fun setEnableFormula(enabled: Boolean) = updateParams { it.copy(enableFormula = enabled) }
    fun setPollSeconds(seconds: Int) = updateParams { it.copy(pollIntervalMs = seconds * 1000L) }

    private fun updateParams(transform: (ParseParams) -> ParseParams) {
        viewModelScope.launch {
            val current = settingsRepository.observeParseParams().first()
            settingsRepository.setParseParams(transform(current))
        }
    }

    fun addType(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val key = typeRepository.addType(trimmed)
            _state.update {
                it.copy(message = if (key == null) "题型「$trimmed」已存在" else "已新增题型「$trimmed」")
            }
        }
    }

    fun renameType(key: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            typeRepository.renameType(key, trimmed)
            _state.update { it.copy(message = "题型已改名") }
        }
    }

    fun deleteType(key: String) {
        val entry = _state.value.types.firstOrNull { it.key == key } ?: return
        if (entry.builtIn) {
            _state.update { it.copy(message = "内置题型不可删除") }
            return
        }
        viewModelScope.launch {
            typeRepository.deleteType(key)
            _state.update { it.copy(message = "已删除题型，已有题目保留") }
        }
    }

    fun addCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            categoryRepository.create(trimmed)
            _state.update { it.copy(message = "已新建分类「$trimmed」") }
        }
    }

    fun renameCategory(id: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            categoryRepository.rename(id, trimmed)
            _state.update { it.copy(message = "分类已改名") }
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            categoryRepository.delete(id)
            _state.update { it.copy(message = "分类已删除，题目已移入未分类") }
        }
    }

    /** 清空题库：逐条删除（数据量为百级，无需额外 DAO） */
    fun clearAllData() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            questionRepository.observeAll().first().forEach { questionRepository.delete(it.id) }
            categoryRepository.observeCategories().first().forEach { categoryRepository.delete(it.id) }
            importRepository.observeRecords().first().forEach { importRepository.deleteRecord(it.id) }
            settingsRepository.setOnboardingComplete(false)
            _state.update { it.copy(busy = false, message = "题库已清空") }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
