package com.tixi.app.ui.categorymanage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tixi.app.domain.model.Category
import com.tixi.app.domain.repository.CategoryRepository
import com.tixi.app.domain.repository.QuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryManageUiState(
    val categories: List<Category> = emptyList(),
    val uncategorizedCount: Int = 0,
    val message: String? = null,
)

@HiltViewModel
class CategoryManageViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val questionRepository: QuestionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CategoryManageUiState())
    val state: StateFlow<CategoryManageUiState> = _state.asStateFlow()

    init {
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

    fun create(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            categoryRepository.create(trimmed)
            _state.update { it.copy(message = "已新建「$trimmed」") }
        }
    }

    fun rename(id: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            categoryRepository.rename(id, trimmed)
            _state.update { it.copy(message = "已改名") }
        }
    }

    fun delete(category: Category) {
        viewModelScope.launch {
            categoryRepository.delete(category.id)
            _state.update {
                it.copy(message = "已删除「${category.name}」，题目的分类已移入未分类")
            }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
