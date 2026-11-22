package com.tixi.app.ui.practice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tixi.app.domain.model.BuiltInQuestionType
import com.tixi.app.domain.model.Category
import com.tixi.app.domain.model.Question
import com.tixi.app.domain.model.SessionStats
import com.tixi.app.domain.repository.CategoryRepository
import com.tixi.app.domain.repository.PracticeRepository
import com.tixi.app.domain.repository.QuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PracticePhase { ANSWERING, REVEALED }

data class PracticeUiState(
    val loading: Boolean = true,
    val categoryName: String = "",
    val questions: List<Question> = emptyList(),
    val index: Int = 0,
    val phase: PracticePhase = PracticePhase.ANSWERING,
    val selectedKey: String? = null,
    val fillText: String = "",
    val selfMastered: Boolean? = null,
    val complete: Boolean = false,
    val stats: SessionStats = SessionStats(),
    val eyeCare: Boolean = true,
    val message: String? = null,
) {
    val current: Question? get() = questions.getOrNull(index)
    val isLast: Boolean get() = questions.isNotEmpty() && index == questions.lastIndex
    val isFirst: Boolean get() = index == 0
    val total: Int get() = questions.size
    val progressFraction: Float
        get() = if (total <= 0) 0f else ((index + 1).toFloat() / total).coerceIn(0f, 1f)
    val revealed: Boolean get() = phase == PracticePhase.REVEALED

    val canSubmit: Boolean
        get() {
            val q = current ?: return false
            return when (q.builtInType) {
                BuiltInQuestionType.CHOICE -> selectedKey != null
                BuiltInQuestionType.FILL -> fillText.isNotBlank()
                else -> false
            }
        }

    /** 最后一题已提交（或大题在最后一题）时，主按钮转为「查看本轮结果」 */
    val primaryIsFinish: Boolean
        get() = isLast && (revealed || current?.isAnswerable == false)
}

@HiltViewModel
class PracticeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val questionRepository: QuestionRepository,
    private val categoryRepository: CategoryRepository,
    private val practiceRepository: PracticeRepository,
) : ViewModel() {

    private val rawCategoryId: Long = savedStateHandle.get<Long>("categoryId") ?: Category.UNCATEGORIZED_ID
    private val categoryId: Long? = rawCategoryId.takeIf { it != Category.UNCATEGORIZED_ID }

    private val _state = MutableStateFlow(PracticeUiState())
    val state: StateFlow<PracticeUiState> = _state.asStateFlow()

    private val startedAt = System.currentTimeMillis()

    /** 自评结果只用于本轮结算，不写回数据库（沿用 R3） */
    private val mastery = linkedMapOf<Long, Boolean>()

    init {
        viewModelScope.launch {
            // 用 first() 取名，避免 Room 的无限 Flow 把加载流程挂住
            val name = categoryId?.let { categoryRepository.observeCategory(it).first()?.name } ?: "未分类"

            val questions = questionRepository.questionsOfCategory(categoryId)
            if (questions.isEmpty()) {
                _state.update { it.copy(loading = false, categoryName = name, message = "这个分类还没有题目") }
                return@launch
            }
            val progress = practiceRepository.getProgress(rawCategoryId)
            val resumeIndex = questions.indexOfFirst { it.id == progress?.lastQuestionId }.coerceAtLeast(0)

            _state.update {
                it.copy(
                    loading = false,
                    categoryName = name,
                    questions = questions,
                    index = resumeIndex,
                )
            }
        }
    }

    fun selectOption(key: String) {
        val s = _state.value
        if (s.revealed || s.current?.builtInType != BuiltInQuestionType.CHOICE) return
        _state.update { it.copy(selectedKey = key) }
    }

    fun updateFill(text: String) {
        val s = _state.value
        if (s.revealed || s.current?.builtInType != BuiltInQuestionType.FILL) return
        _state.update { it.copy(fillText = text) }
    }

    fun submit() {
        val s = _state.value
        if (!s.canSubmit) return
        _state.update { it.copy(phase = PracticePhase.REVEALED) }
    }

    /** 自评：只影响本轮结算，可改选 */
    fun selfAssess(mastered: Boolean) {
        val s = _state.value
        val q = s.current ?: return
        mastery[q.id] = mastered
        _state.update { it.copy(selfMastered = mastered) }
    }

    fun next() {
        val s = _state.value
        if (s.primaryIsFinish) {
            finish()
            return
        }
        if (s.isLast) return
        moveTo(s.index + 1)
    }

    fun previous() {
        val s = _state.value
        if (s.isFirst) return
        moveTo(s.index - 1)
    }

    private fun moveTo(index: Int) {
        val s = _state.value
        val target = s.questions.getOrNull(index) ?: return
        _state.update {
            it.copy(
                index = index,
                phase = PracticePhase.ANSWERING,
                selectedKey = null,
                fillText = "",
                selfMastered = mastery[target.id],
            )
        }
        viewModelScope.launch { practiceRepository.saveProgress(rawCategoryId, target.id) }
    }

    fun toggleEyeCare() = _state.update { it.copy(eyeCare = !it.eyeCare) }

    fun finish() {
        val s = _state.value
        val elapsed = System.currentTimeMillis() - startedAt
        val stats = SessionStats(
            total = s.questions.size,
            elapsedMs = elapsed,
            mastered = mastery.count { it.value },
            weakStems = s.questions.filter { mastery[it.id] == false }.map { it.stem.take(24) },
        )
        _state.update { it.copy(complete = true, stats = stats) }
    }

    /** 重进分类时从头开始（「再来一轮」） */
    fun restart() {
        mastery.clear()
        _state.update {
            it.copy(
                complete = false,
                index = 0,
                phase = PracticePhase.ANSWERING,
                selectedKey = null,
                fillText = "",
                selfMastered = null,
                stats = SessionStats(),
            )
        }
        _state.value.questions.firstOrNull()?.let { first ->
            viewModelScope.launch { practiceRepository.saveProgress(rawCategoryId, first.id) }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun answerTextFor(question: Question): String {
        return when (question.builtInType) {
            BuiltInQuestionType.CHOICE -> question.answer.correctKeys.joinToString("、") { key ->
                val idx = key.trim().uppercase().firstOrNull()?.minus('A') ?: -1
                val option = question.options.getOrNull(idx)
                if (option != null) "$key · $option" else key
            }
            BuiltInQuestionType.FILL -> question.answer.correctKeys.joinToString(" / ")
                .ifBlank { question.answer.text ?: "—" }
            else -> question.answer.text ?: question.answer.correctKeys.joinToString("\n")
        }
    }
}
