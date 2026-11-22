package com.tixi.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tixi.app.domain.model.Category
import com.tixi.app.domain.model.PracticeProgress
import com.tixi.app.domain.model.Question
import com.tixi.app.domain.repository.CategoryRepository
import com.tixi.app.domain.repository.PracticeRepository
import com.tixi.app.domain.repository.QuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CurrentProgress(
    val categoryId: Long,
    val name: String,
    val index: Int,
    val total: Int,
    val lastStem: String?,
) {
    val fraction: Float get() = if (total <= 0) 0f else (index.toFloat() / total).coerceIn(0f, 1f)
}

data class RecentCategory(val categoryId: Long, val name: String, val count: Int, val index: Int)

data class HomeUiState(
    val loading: Boolean = true,
    val totalQuestions: Int = 0,
    val categoryCount: Int = 0,
    val current: CurrentProgress? = null,
    val recent: List<RecentCategory> = emptyList(),
)

private data class CategorySnapshot(
    val category: Category,
    val questions: List<Question>,
    val progress: PracticeProgress,
    val index: Int,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val questionRepository: QuestionRepository,
    private val practiceRepository: PracticeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.observeCategories().collectLatest { categories ->
                val snapshots = categories.mapNotNull { category ->
                    val progress = practiceRepository.getProgress(category.id) ?: return@mapNotNull null
                    val questions = questionRepository.questionsOfCategory(category.id)
                    if (questions.isEmpty()) return@mapNotNull null
                    val index = questions.indexOfFirst { it.id == progress.lastQuestionId }.coerceAtLeast(0)
                    CategorySnapshot(category, questions, progress, index)
                }.sortedByDescending { it.progress.updatedAt }

                val top = snapshots.firstOrNull()
                _state.update {
                    it.copy(
                        loading = false,
                        totalQuestions = categories.sumOf { c -> c.questionCount },
                        categoryCount = categories.size,
                        current = top?.let { s ->
                            CurrentProgress(
                                categoryId = s.category.id,
                                name = s.category.name,
                                index = s.index + 1,
                                total = s.questions.size,
                                lastStem = s.questions.getOrNull(s.index)?.stem?.take(28),
                            )
                        },
                        recent = snapshots.take(4).map { s ->
                            RecentCategory(s.category.id, s.category.name, s.questions.size, s.index + 1)
                        },
                    )
                }
            }
        }
    }
}
