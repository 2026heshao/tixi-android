package com.tixi.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tixi.app.domain.model.Category
import com.tixi.app.domain.model.Question
import com.tixi.app.domain.repository.CategoryRepository
import com.tixi.app.domain.repository.QuestionRepository
import com.tixi.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val questions: List<Question> = emptyList(),
    val categories: List<Category> = emptyList(),
    val query: String = "",
    val filterCategoryId: Long? = null,
    val filterActive: Boolean = false,
    val hasToken: Boolean = false,
    val loading: Boolean = true,
) {
    private fun matchesCategory(q: Question): Boolean =
        !filterActive || q.categoryId == filterCategoryId

    val visible: List<Question>
        get() {
            val keyword = query.trim()
            return questions.filter { q ->
                matchesCategory(q) && (
                    keyword.isEmpty() ||
                        q.stem.contains(keyword, ignoreCase = true) ||
                        q.tags.any { it.contains(keyword, ignoreCase = true) }
                    )
            }
        }

    fun categoryName(id: Long?): String =
        if (id == null) "未分类" else categories.firstOrNull { it.id == id }?.name ?: "未分类"

    fun countOf(id: Long?, all: Boolean = false): Int =
        if (all) questions.size else questions.count { it.categoryId == id }
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow<Pair<Boolean, Long?>>(false to null)

    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                questionRepository.observeAll(),
                categoryRepository.observeCategories(),
                query,
                filter,
                settingsRepository.observeTokenConfigured(),
            ) { questions, categories, q, f, hasToken ->
                LibraryUiState(
                    questions = questions,
                    categories = categories,
                    query = q,
                    filterCategoryId = f.second,
                    filterActive = f.first,
                    hasToken = hasToken,
                    loading = false,
                )
            }.collect { _state.value = it }
        }
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun setFilter(categoryId: Long?) {
        filter.value = true to categoryId
    }

    fun clearFilter() {
        filter.value = false to null
    }

    fun clearSearch() {
        query.value = ""
    }

    fun delete(question: Question) {
        viewModelScope.launch { questionRepository.delete(question.id) }
    }
}
