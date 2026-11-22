package com.tixi.app.ui.questionedit

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tixi.app.data.files.ImportFileStore
import com.tixi.app.domain.model.AnswerData
import com.tixi.app.domain.model.BuiltInQuestionType
import com.tixi.app.domain.model.Category
import com.tixi.app.domain.model.Question
import com.tixi.app.domain.model.QuestionTypeEntry
import com.tixi.app.domain.model.RichBlock
import com.tixi.app.domain.repository.CategoryRepository
import com.tixi.app.domain.repository.QuestionRepository
import com.tixi.app.domain.repository.QuestionTypeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuestionEditUiState(
    val loading: Boolean = true,
    val isNew: Boolean = false,
    val questionId: Long = 0,
    val types: List<QuestionTypeEntry> = emptyList(),
    val typeKey: String = BuiltInQuestionType.CHOICE.key,
    val stem: String = "",
    val options: List<String> = emptyList(),
    val answerKeys: Set<String> = emptySet(),
    val fillAnswer: String = "",
    val essayAnswer: String = "",
    val explanation: String = "",
    val categoryId: Long? = null,
    val categories: List<Category> = emptyList(),
    val tagsText: String = "",
    val remark: String = "",
    val richBlocks: List<RichBlock> = emptyList(),
    val index: Int = 0,
    val siblingCount: Int = 0,
    val message: String? = null,
    val back: Boolean = false,
) {
    val builtInType: BuiltInQuestionType? get() = BuiltInQuestionType.fromKey(typeKey)
    val numberLabel: String
        get() = if (siblingCount <= 0) "" else "第 ${index + 1} 题 / 共 $siblingCount"
}

@HiltViewModel
class QuestionEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val questionRepository: QuestionRepository,
    private val categoryRepository: CategoryRepository,
    private val typeRepository: QuestionTypeRepository,
    private val fileStore: ImportFileStore,
) : ViewModel() {

    private val startId: Long = savedStateHandle.get<Long>("questionId") ?: NEW_QUESTION_ID

    private val _state = MutableStateFlow(QuestionEditUiState())
    val state: StateFlow<QuestionEditUiState> = _state.asStateFlow()

    private var original: Question? = null
    private var siblingIds: List<Long> = emptyList()

    init {
        viewModelScope.launch { boot() }
    }

    private suspend fun boot() {
        val types = typeRepository.types()
        val categories = categoryRepository.categories()
        _state.update { it.copy(types = types, categories = categories) }

        if (startId == NEW_QUESTION_ID) {
            _state.update {
                it.copy(
                    loading = false,
                    isNew = true,
                    typeKey = types.firstOrNull { t -> t.builtIn }?.key ?: BuiltInQuestionType.CHOICE.key,
                    options = listOf("", "", "", ""),
                )
            }
            return
        }
        loadQuestion(startId)
    }

    private suspend fun loadQuestion(id: Long) {
        val question = questionRepository.getQuestion(id)
        if (question == null) {
            _state.update { it.copy(loading = false, back = true) }
            return
        }
        original = question
        siblingIds = questionRepository.questionsOfCategory(question.categoryId).map { it.id }
        val index = siblingIds.indexOf(question.id).coerceAtLeast(0)
        val builtIn = question.builtInType ?: BuiltInQuestionType.ESSAY
        _state.update {
            it.copy(
                loading = false,
                isNew = false,
                questionId = question.id,
                typeKey = question.typeKey,
                stem = question.stem,
                options = if (builtIn == BuiltInQuestionType.CHOICE) question.options else emptyList(),
                answerKeys = if (builtIn == BuiltInQuestionType.CHOICE) {
                    question.answer.correctKeys.map { k -> k.trim().uppercase() }.toSet()
                } else emptySet(),
                fillAnswer = if (builtIn == BuiltInQuestionType.FILL) question.answer.correctKeys.joinToString(", ") else "",
                essayAnswer = when (builtIn) {
                    BuiltInQuestionType.ESSAY -> question.answer.text ?: question.answer.correctKeys.joinToString("\n")
                    else -> ""
                },
                explanation = question.explanation.orEmpty(),
                categoryId = question.categoryId,
                tagsText = question.tags.joinToString(", "),
                remark = question.remark.orEmpty(),
                richBlocks = question.richBlocks,
                index = index,
                siblingCount = siblingIds.size,
                back = false,
            )
        }
    }

    fun updateStem(value: String) = _state.update { it.copy(stem = value) }
    fun updateExplanation(value: String) = _state.update { it.copy(explanation = value) }
    fun updateFillAnswer(value: String) = _state.update { it.copy(fillAnswer = value) }
    fun updateEssayAnswer(value: String) = _state.update { it.copy(essayAnswer = value) }
    fun updateTags(value: String) = _state.update { it.copy(tagsText = value) }
    fun updateRemark(value: String) = _state.update { it.copy(remark = value) }
    fun updateRichBlocks(blocks: List<RichBlock>) = _state.update { it.copy(richBlocks = blocks) }

    fun switchType(key: String) {
        _state.update { current ->
            val target = BuiltInQuestionType.fromKey(key)
            current.copy(
                typeKey = key,
                options = when {
                    target == BuiltInQuestionType.CHOICE && current.options.isEmpty() ->
                        listOf("", "", "", "")
                    target == BuiltInQuestionType.CHOICE -> current.options
                    else -> current.options
                },
            )
        }
    }

    fun updateOption(index: Int, value: String) {
        _state.update { s ->
            if (index !in s.options.indices) return@update s
            s.copy(options = s.options.toMutableList().also { it[index] = value })
        }
    }

    fun addOption() {
        _state.update { s ->
            if (s.options.size >= 8) s else s.copy(options = s.options + "")
        }
    }

    fun removeOption(index: Int) {
        _state.update { s ->
            if (s.options.size <= 2 || index !in s.options.indices) return@update s
            val key = ('A' + index).toString()
            s.copy(
                options = s.options.toMutableList().also { it.removeAt(index) },
                answerKeys = s.answerKeys - key,
            )
        }
    }

    fun toggleAnswerKey(key: String) {
        _state.update { s ->
            val keys = s.answerKeys.toMutableSet()
            if (!keys.add(key)) keys.remove(key)
            s.copy(answerKeys = keys)
        }
    }

    fun setCategory(categoryId: Long?) = _state.update { it.copy(categoryId = categoryId) }

    fun addTableBlock() = _state.update {
        it.copy(richBlocks = it.richBlocks + RichBlock.Table(rows = listOf(listOf("", ""), listOf("", ""))))
    }

    fun addFormulaBlock() = _state.update {
        it.copy(richBlocks = it.richBlocks + RichBlock.Formula(latex = "f(x) = \\frac{1}{\\sqrt{2\\pi}}"))
    }

    fun removeRichBlock(index: Int) = _state.update { s ->
        s.copy(richBlocks = s.richBlocks.filterIndexed { i, _ -> i != index })
    }

    fun replaceImage(index: Int, uri: Uri, alt: String) {
        viewModelScope.launch {
            val file = fileStore.copyToPrivate(uri, "figure_${System.currentTimeMillis()}.jpg")
            _state.update { s ->
                val blocks = s.richBlocks.toMutableList()
                if (index in blocks.indices) {
                    blocks[index] = RichBlock.ImageBlock(path = file.absolutePath, alt = alt)
                }
                s.copy(richBlocks = blocks)
            }
        }
    }

    fun attachImage(uri: Uri) {
        viewModelScope.launch {
            val file = fileStore.copyToPrivate(uri, "figure_${System.currentTimeMillis()}.jpg")
            _state.update {
                it.copy(richBlocks = it.richBlocks + RichBlock.ImageBlock(path = file.absolutePath, alt = "题目原图"))
            }
        }
    }

    private fun parseList(text: String): List<String> =
        text.split(',', '，', '、', '/', ';', '；')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    /**
     * 保存当前题。advance=true 时保存后切到下一题（无下一题则提示已到末尾）。
     * 返回是否保存成功。
     */
    fun save(advance: Boolean) {
        val s = _state.value
        if (s.stem.isBlank()) {
            _state.update { it.copy(message = "题干不能为空，未保存") }
            return
        }
        viewModelScope.launch {
            val builtIn = s.builtInType
            val answer = when (builtIn) {
                BuiltInQuestionType.CHOICE -> AnswerData(correctKeys = s.answerKeys.sorted())
                BuiltInQuestionType.FILL -> AnswerData(correctKeys = parseList(s.fillAnswer))
                BuiltInQuestionType.ESSAY -> AnswerData(text = s.essayAnswer.trim())
                null -> AnswerData(text = s.essayAnswer.trim())
            }
            val options = if (builtIn == BuiltInQuestionType.CHOICE) {
                s.options.map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                emptyList()
            }
            val prev = original
            val categoryChanged = prev != null && prev.categoryId != s.categoryId
            val sortOrder = when {
                prev == null || categoryChanged -> questionRepository.nextSortOrder(s.categoryId)
                else -> prev.sortOrder
            }
            val entity = Question(
                id = if (s.isNew) 0 else s.questionId,
                categoryId = s.categoryId,
                stem = s.stem.trim(),
                options = options,
                answer = answer,
                explanation = s.explanation.trim().ifBlank { null },
                typeKey = s.typeKey,
                richBlocks = s.richBlocks,
                sortOrder = sortOrder,
                sourceFile = prev?.sourceFile,
                sourceImagePath = prev?.sourceImagePath,
                tags = parseList(s.tagsText),
                remark = s.remark.trim().ifBlank { null },
                createdAt = prev?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
            val newId = questionRepository.upsert(entity)
            val saved = entity.copy(id = if (s.isNew) newId else s.questionId)
            original = saved

            siblingIds = questionRepository.questionsOfCategory(saved.categoryId).map { it.id }
            val index = siblingIds.indexOf(saved.id).coerceAtLeast(0)

            if (advance) {
                val nextId = siblingIds.getOrNull(index + 1)
                if (nextId != null) {
                    loadQuestion(nextId)
                    _state.update { it.copy(message = "已保存 · 当前第 ${index + 2} 题") }
                    return@launch
                }
            }

            _state.update {
                it.copy(
                    isNew = false,
                    questionId = saved.id,
                    index = index,
                    siblingCount = siblingIds.size,
                    message = if (advance) "已保存 · 已是本分类最后一题" else "已保存 · 当前第 ${index + 1} 题",
                )
            }
        }
    }

    fun goPrevious() {
        val s = _state.value
        val prevId = siblingIds.getOrNull(s.index - 1) ?: return
        viewModelScope.launch { loadQuestion(prevId) }
    }

    fun delete() {
        val id = _state.value.questionId
        if (_state.value.isNew || id == 0L) {
            _state.update { it.copy(back = true) }
            return
        }
        viewModelScope.launch {
            questionRepository.delete(id)
            _state.update { it.copy(back = true, message = "已删除") }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
    fun consumeBack() = _state.update { it.copy(back = false) }

    companion object {
        const val NEW_QUESTION_ID = -1L
    }
}
