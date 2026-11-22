package com.tixi.app.ui.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tixi.app.domain.repository.QuestionRepository
import com.tixi.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val step: Int = 1,
    val tokenInput: String = "",
    val configured: Boolean = false,
    val totalQuestions: Int = 0,
    val firstStem: String? = null,
    val message: String? = null,
    val finished: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
    questionRepository: QuestionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        OnboardingUiState(step = (savedStateHandle.get<String>("step")?.toIntOrNull() ?: 1).coerceIn(1, 4)),
    )
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observeTokenConfigured().collect { configured ->
                _state.update { it.copy(configured = configured) }
            }
        }
        viewModelScope.launch {
            questionRepository.observeAll().collect { questions ->
                _state.update {
                    it.copy(
                        totalQuestions = questions.size,
                        firstStem = questions.minByOrNull { q -> q.sortOrder }?.stem,
                    )
                }
            }
        }
    }

    fun goTo(step: Int) = _state.update { it.copy(step = step.coerceIn(1, 4)) }

    fun updateToken(value: String) = _state.update { it.copy(tokenInput = value) }

    fun saveToken() {
        val token = _state.value.tokenInput.trim()
        if (token.isEmpty()) {
            _state.update { it.copy(message = "填入 Token 后才能继续，也可以先跳过") }
            return
        }
        viewModelScope.launch {
            settingsRepository.saveToken(token)
            _state.update { it.copy(step = 3, tokenInput = "") }
        }
    }

    fun skipToken() = _state.update { it.copy(step = 3) }

    /** 跳过后仍能进工作台空态，不强制走完（设计 §13.1） */
    fun finish() {
        viewModelScope.launch {
            settingsRepository.setOnboardingComplete(true)
            _state.update { it.copy(finished = true) }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
