package com.tixi.app.ui.importflow

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tixi.app.domain.model.BuiltInQuestionType
import com.tixi.app.domain.model.DraftQuestion
import com.tixi.app.domain.model.QualityFlag
import com.tixi.app.domain.model.RichBlock
import com.tixi.app.ui.common.Badge
import com.tixi.app.ui.common.BadgeTone
import com.tixi.app.ui.common.BottomActionBar
import com.tixi.app.ui.common.InfoNote
import com.tixi.app.ui.common.RichBlockEditor
import com.tixi.app.ui.common.TixiButton
import com.tixi.app.ui.common.TixiButtonSize
import com.tixi.app.ui.common.TixiButtonVariant
import com.tixi.app.ui.common.TixiChip
import com.tixi.app.ui.common.TixiDialog
import com.tixi.app.ui.common.TixiIconButton
import com.tixi.app.ui.common.TixiSnackbarHost
import com.tixi.app.ui.common.TixiTextField
import com.tixi.app.ui.common.TixiTopBar
import com.tixi.app.ui.common.WarningBanner
import com.tixi.app.ui.theme.TixiFontFamilies
import com.tixi.app.ui.theme.TixiRadius
import com.tixi.app.ui.theme.TixiSpacing
import com.tixi.app.ui.theme.TixiTheme

@Composable
fun ImportReviewScreen(
    onBack: () -> Unit,
    onImported: () -> Unit,
    viewModel: ImportReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = TixiTheme.colors
    val snackbarHostState = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<DraftQuestion?>(null) }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }
    LaunchedEffect(state.done) {
        if (state.done) {
            viewModel.consumeDone()
            onImported()
        }
    }

    Box(Modifier.fillMaxSize().background(colors.canvas)) {
        Column(Modifier.fillMaxSize()) {
            TixiTopBar(
                title = "识别确认",
                onBack = onBack,
                trailingText = if (state.total > 0) "${state.confirmed} / ${state.total} 已确认" else null,
            )

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = TixiSpacing.pageHorizontal),
            ) {
                Spacer(Modifier.height(4.dp))
                if (state.garbageCount > 0) {
                    WarningBanner(
                        title = "检测到 ${state.garbageCount} 处识别质量异常",
                        detail = "疑似乱码的题目已标记「待核对」，建议逐题核对或返回重新识别。",
                    )
                } else if (state.reviewCount > 0) {
                    WarningBanner(
                        title = "${state.reviewCount} 题需要人工确认",
                        detail = "含表格/公式/原图的草稿不会被「全部确认」批量跳过。",
                    )
                } else {
                    InfoNote("识别结果不会直接入库。核对无误后逐题确认，或一次性全部确认。")
                }

                Spacer(Modifier.height(12.dp))
                val draft = state.draft
                if (draft == null) {
                    Text("草稿不存在或已被删除", fontSize = 13.sp, color = colors.textSecondary)
                } else {
                    draft.questions.forEachIndexed { index, question ->
                        DraftCard(
                            index = index + 1,
                            question = question,
                            categoryName = state.categories.firstOrNull { it.id == question.suggestedCategoryId }?.name
                                ?: "未分类",
                            onToggle = { viewModel.toggleConfirm(question) },
                            onEdit = { editing = question },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
                Spacer(Modifier.height(TixiSpacing.s8))
            }

            BottomActionBar {
                TixiButton(
                    text = "仅入库已确认 (${state.confirmed})",
                    onClick = viewModel::importConfirmedOnly,
                    modifier = Modifier.weight(1f),
                    variant = TixiButtonVariant.Outlined,
                    size = TixiButtonSize.Medium,
                    enabled = !state.importing,
                )
                TixiButton(
                    text = "全部确认并入库",
                    onClick = viewModel::requestConfirmAll,
                    modifier = Modifier.weight(1f),
                    size = TixiButtonSize.Medium,
                    loading = state.importing,
                )
            }
        }
        TixiSnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp))
    }

    if (state.askBulkConfirm) {
        TixiDialog(
            title = "有 ${state.reviewCount} 题需人工核对",
            message = "这些草稿含表格/公式/原图或疑似乱码，按规范不应批量跳过。仍要全部确认并入库吗？",
            confirmLabel = "仍然全部入库",
            onConfirm = viewModel::confirmBulkAnyway,
            onDismiss = viewModel::dismissBulkConfirm,
        )
    }

    editing?.let { question ->
        DraftEditDialog(
            question = question,
            categories = state.categories,
            onDismiss = { editing = null },
            onSave = { updated ->
                viewModel.updateQuestion(updated)
                editing = null
            },
            onReplaceImage = { index, uri, alt ->
                viewModel.replaceDraftImage(question, index, uri, alt)
            },
        )
    }
}

@Composable
private fun DraftCard(
    index: Int,
    question: DraftQuestion,
    categoryName: String,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
) {
    val colors = TixiTheme.colors
    val flagged = question.qualityFlags.any { it == QualityFlag.SUSPECT_GARBLED }

    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .border(
                1.dp,
                when {
                    flagged -> colors.secondary
                    question.confirmed -> colors.success
                    else -> colors.outline
                },
                RoundedCornerShape(TixiRadius.md),
            )
            .padding(13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                index.toString().padStart(2, '0'),
                fontFamily = TixiFontFamilies.Mono,
                fontSize = 11.sp,
                color = colors.textTertiary,
            )
            Spacer(Modifier.size(8.dp))
            when {
                flagged -> Badge("待核对", BadgeTone.Warn)
                question.confirmed -> Badge("已确认", BadgeTone.Ok)
                else -> Badge("待确认", BadgeTone.Neutral)
            }
            Spacer(Modifier.weight(1f))
            TixiIconButton(
                icon = if (question.confirmed) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = if (question.confirmed) "取消确认" else "确认这道题",
                onClick = onToggle,
                tint = if (question.confirmed) colors.success else colors.textTertiary,
                buttonSize = 36.dp,
            )
            TixiIconButton(Icons.Outlined.Edit, "编辑这道题", onEdit, buttonSize = 36.dp)
        }
        Spacer(Modifier.height(8.dp))
        Text(question.stem, fontFamily = TixiFontFamilies.Serif, fontSize = 14.sp, lineHeight = 22.sp, color = colors.textPrimary)

        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            val facts = buildList {
                if (question.options.isNotEmpty()) add("${question.options.size} 个选项")
                val answer = question.answer.simpleDisplay()
                if (answer != "—") add("答案 $answer")
                add(categoryName)
                if (question.richBlocks.isNotEmpty()) add("含" + question.richBlocks.joinToString("/") { block ->
                    when (block) {
                        is RichBlock.Table -> "表格"
                        is RichBlock.Formula -> "公式"
                        is RichBlock.ImageBlock -> "原图"
                    }
                })
            }
            facts.forEachIndexed { i, fact ->
                if (i > 0) Spacer(Modifier.size(12.dp))
                Text(
                    fact,
                    fontSize = 11.sp,
                    color = if (fact.startsWith("含")) colors.primary else colors.textTertiary,
                )
            }
        }
        val flags = question.qualityFlags.filter { it != QualityFlag.HAS_RICH_CONTENT }
        if (flags.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                flags.joinToString(" · ") { it.label } + " —— " + flags.first().detail,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = colors.warning,
            )
        }
    }
}

@Composable
private fun DraftEditDialog(
    question: DraftQuestion,
    categories: List<com.tixi.app.domain.model.Category>,
    onDismiss: () -> Unit,
    onSave: (DraftQuestion) -> Unit,
    onReplaceImage: (Int, Uri, String) -> Unit,
) {
    val colors = TixiTheme.colors
    var stem by remember { mutableStateOf(question.stem) }
    var explanation by remember { mutableStateOf(question.explanation.orEmpty()) }
    var typeKey by remember { mutableStateOf(question.typeKey) }
    var options by remember { mutableStateOf(question.options) }
    var answerKeys by remember { mutableStateOf(question.answer.correctKeys.toSet()) }
    var fillAnswer by remember { mutableStateOf(question.answer.correctKeys.joinToString(", ")) }
    var answerText by remember { mutableStateOf(question.answer.text.orEmpty()) }
    var categoryId by remember { mutableStateOf(question.suggestedCategoryId) }
    var richBlocks by remember { mutableStateOf(question.richBlocks) }
    var pendingIndex by remember { mutableIntStateOf(-1) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null && pendingIndex >= 0) onReplaceImage(pendingIndex, uri, "题目原图")
    }

    val builtIn = BuiltInQuestionType.fromKey(typeKey)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("编辑草稿", fontFamily = TixiFontFamilies.Serif, fontSize = 18.sp, color = colors.textPrimary) },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BuiltInQuestionType.entries.forEach { type ->
                        TixiChip(
                            text = type.label,
                            selected = typeKey == type.key,
                            onClick = { typeKey = type.key },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("题干", fontSize = 12.sp, color = colors.textSecondary)
                Spacer(Modifier.height(6.dp))
                TixiTextField(stem, { stem = it }, Modifier.fillMaxWidth(), singleLine = false, minLines = 3)

                if (builtIn == BuiltInQuestionType.CHOICE) {
                    Spacer(Modifier.height(12.dp))
                    Text("选项（每行一个）", fontSize = 12.sp, color = colors.textSecondary)
                    options.forEachIndexed { index, option ->
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                ('A' + index).toString(),
                                fontFamily = TixiFontFamilies.Mono,
                                fontSize = 11.sp,
                                color = colors.textTertiary,
                                modifier = Modifier.size(18.dp),
                            )
                            TixiTextField(
                                option,
                                { value -> options = options.toMutableList().also { it[index] = value } },
                                Modifier.weight(1f),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("正确答案", fontSize = 12.sp, color = colors.textSecondary)
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.indices.forEach { index ->
                            val key = ('A' + index).toString()
                            TixiChip(key, key in answerKeys, {
                                answerKeys = if (key in answerKeys) answerKeys - key else answerKeys + key
                            })
                        }
                    }
                } else {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (builtIn == BuiltInQuestionType.FILL) "标准答案（多空用逗号分隔）" else "参考答案",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                    )
                    Spacer(Modifier.height(6.dp))
                    TixiTextField(
                        if (builtIn == BuiltInQuestionType.FILL) fillAnswer else answerText,
                        { value ->
                            if (builtIn == BuiltInQuestionType.FILL) {
                                fillAnswer = value
                            } else {
                                answerText = value
                            }
                        },
                        Modifier.fillMaxWidth(),
                        singleLine = builtIn == BuiltInQuestionType.FILL,
                        minLines = if (builtIn == BuiltInQuestionType.FILL) 1 else 3,
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text("解析", fontSize = 12.sp, color = colors.textSecondary)
                Spacer(Modifier.height(6.dp))
                TixiTextField(explanation, { explanation = it }, Modifier.fillMaxWidth(), singleLine = false, minLines = 2)

                Spacer(Modifier.height(12.dp))
                Text("分类", fontSize = 12.sp, color = colors.textSecondary)
                Row(Modifier.padding(top = 6.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TixiChip("未分类", categoryId == null, { categoryId = null })
                    categories.forEach { category ->
                        TixiChip(category.name, categoryId == category.id, { categoryId = category.id })
                    }
                }

                if (richBlocks.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text("题目素材", fontSize = 12.sp, color = colors.textSecondary)
                    RichBlockEditor(
                        blocks = richBlocks,
                        onChange = { richBlocks = it },
                        onRequestReplaceImage = { index ->
                            pendingIndex = index
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val answer = when (builtIn) {
                    BuiltInQuestionType.CHOICE -> question.answer.copy(correctKeys = answerKeys.toList().sorted())
                    BuiltInQuestionType.FILL -> question.answer.copy(
                        correctKeys = fillAnswer.split(',', '，', '、', '/')
                            .map { it.trim() }
                            .filter { it.isNotEmpty() },
                    )
                    else -> question.answer.copy(correctKeys = emptyList(), text = answerText)
                }
                onSave(
                    question.copy(
                        stem = stem,
                        options = if (builtIn == BuiltInQuestionType.CHOICE) options else emptyList(),
                        answer = answer,
                        explanation = explanation.ifBlank { null },
                        typeKey = typeKey,
                        suggestedCategoryId = categoryId,
                        richBlocks = richBlocks,
                        needsReview = question.needsReview,
                    ),
                )
            }) { Text("保存", color = colors.primary, fontWeight = FontWeight.Medium) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = colors.textSecondary) }
        },
    )
}
