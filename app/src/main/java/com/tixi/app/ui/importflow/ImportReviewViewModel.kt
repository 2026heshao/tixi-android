package com.tixi.app.ui.importflow

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tixi.app.domain.model.Category
import com.tixi.app.domain.model.DraftQuestion
import com.tixi.app.domain.model.ImportDraft
import com.tixi.app.domain.repository.CategoryRepository
import com.tixi.app.domain.repository.ImportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImportReviewUiState(
    val loading: Boolean = true,
    val draft: ImportDraft? = null,
    val categories: List<Category> = emptyList(),
    val message: String? = null,
    val done: Boolean = false,
    val askBulkConfirm: Boolean = false,
    val importing: Boolean = false,
) {
    val total: Int get() = draft?.questions?.size ?: 0
    val confirmed: Int get() = draft?.questions?.count { it.confirmed } ?: 0
    val reviewCount: Int get() = draft?.questions?.count { it.needsReview } ?: 0
    val garbageCount: Int
        get() = draft?.questions?.count { q -> q.qualityFlags.any { it.name == "SUSPECT_GARBLED" } } ?: 0
}

@HiltViewModel
class ImportReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val importRepository: ImportRepository,
    categoryRepository: CategoryRepository,
    private val fileStore: com.tixi.app.data.files.ImportFileStore,
) : ViewModel() {

    private val draftId: Long = savedStateHandle.get<Long>("draftId") ?: 0L

    private val _state = MutableStateFlow(ImportReviewUiState())
    val state: StateFlow<ImportReviewUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { categories ->
                _state.update { it.copy(categories = categories) }
            }
        }
        viewModelScope.launch {
            importRepository.observeDraft(draftId).collect { draft ->
                _state.update { it.copy(draft = draft, loading = false) }
            }
        }
    }

    fun toggleConfirm(question: DraftQuestion) {
        viewModelScope.launch {
            importRepository.updateDraftQuestion(question.copy(confirmed = !question.confirmed))
        }
    }

    fun setCategory(question: DraftQuestion, categoryId: Long?) {
        viewModelScope.launch {
            importRepository.updateDraftQuestion(question.copy(suggestedCategoryId = categoryId))
        }
    }

    fun updateQuestion(question: DraftQuestion) {
        viewModelScope.launch { importRepository.updateDraftQuestion(question) }
    }

    fun replaceDraftImage(question: DraftQuestion, index: Int, uri: android.net.Uri, alt: String) {
        viewModelScope.launch {
            val file = fileStore.copyToPrivate(uri, "figure_${System.currentTimeMillis()}.jpg")
            val blocks = question.richBlocks.toMutableList()
            if (index in blocks.indices) {
                blocks[index] = com.tixi.app.domain.model.RichBlock.ImageBlock(path = file.absolutePath, alt = alt)
            }
            importRepository.updateDraftQuestion(question.copy(richBlocks = blocks))
        }
    }

    /** 全部确认并入库：含需人工核对的草稿时先弹二次确认 */
    fun requestConfirmAll() {
        val review = _state.value.reviewCount
        if (review > 0) {
            _state.update { it.copy(askBulkConfirm = true) }
        } else {
            doImport(all = true)
        }
    }

    fun dismissBulkConfirm() = _state.update { it.copy(askBulkConfirm = false) }

    fun confirmBulkAnyway() {
        _state.update { it.copy(askBulkConfirm = false) }
        doImport(all = true)
    }

    fun importConfirmedOnly() {
        if (_state.value.confirmed == 0) {
            _state.update { it.copy(message = "还没有已确认的题目") }
            return
        }
        doImport(all = false)
    }

    private fun doImport(all: Boolean) {
        val draft = _state.value.draft ?: return
        viewModelScope.launch {
            _state.update { it.copy(importing = true) }
            val ids = if (all) null else draft.questions.filter { it.confirmed }.map { it.id }.toSet()
            val count = importRepository.confirmDrafts(draftId, ids, null)
            _state.update {
                it.copy(
                    importing = false,
                    done = true,
                    message = "已入库 $count 题",
                )
            }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
    fun consumeDone() = _state.update { it.copy(done = false) }
}
