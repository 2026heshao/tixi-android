package com.tixi.app.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tixi.app.domain.model.Category
import com.tixi.app.domain.repository.CategoryRepository
import com.tixi.app.domain.repository.PracticeRepository
import com.tixi.app.domain.repository.QuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryRow(
    val id: Long?,
    val name: String,
    val count: Int,
    val hasProgress: Boolean,
    val lastIndex: Int,
    val total: Int,
)

enum class CategoryFilter(val label: String) { ALL("全部"), WITH_PROGRESS("有进度"), UNCATEGORIZED("未分类") }

data class CategoryUiState(
    val rows: List<CategoryRow> = emptyList(),
    val uncategorizedCount: Int = 0,
    val filter: CategoryFilter = CategoryFilter.ALL,
    val loading: Boolean = true,
) {
    val visible: List<CategoryRow>
        get() = when (filter) {
            CategoryFilter.ALL -> rows
            CategoryFilter.WITH_PROGRESS -> rows.filter { it.hasProgress }
            CategoryFilter.UNCATEGORIZED -> rows.filter { it.id == null }
        }
}

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val questionRepository: QuestionRepository,
    private val practiceRepository: PracticeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CategoryUiState())
    val state: StateFlow<CategoryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.observeCategories().collectLatest { categories ->
                val counts = questionRepository.countByCategory()
                val rows = categories.map { category ->
                    val total = counts[category.id] ?: 0
                    val progress = practiceRepository.getProgress(category.id)
                    val questions = if (progress?.lastQuestionId != null) {
                        questionRepository.questionsOfCategory(category.id)
                    } else {
                        emptyList()
                    }
                    val index = questions.indexOfFirst { it.id == progress?.lastQuestionId }.coerceAtLeast(0)
                    CategoryRow(
                        id = category.id,
                        name = category.name,
                        count = total,
                        hasProgress = progress?.lastQuestionId != null,
                        lastIndex = if (progress?.lastQuestionId != null) index + 1 else 0,
                        total = questions.size,
                    )
                }
                val uncategorizedCount = counts[null] ?: 0
                val allRows = if (uncategorizedCount > 0) {
                    rows + CategoryRow(
                        id = null,
                        name = "未分类",
                        count = uncategorizedCount,
                        hasProgress = false,
                        lastIndex = 0,
                        total = uncategorizedCount,
                    )
                } else {
                    rows
                }
                _state.update {
                    it.copy(
                        rows = allRows,
                        uncategorizedCount = uncategorizedCount,
                        loading = false,
                    )
                }
            }
        }
    }

    fun setFilter(filter: CategoryFilter) = _state.update { it.copy(filter = filter) }
}
