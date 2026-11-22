package com.tixi.app.ui.questionedit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tixi.app.domain.model.BuiltInQuestionType
import com.tixi.app.domain.model.Category
import com.tixi.app.ui.common.BottomActionBar
import com.tixi.app.ui.common.InfoNote
import com.tixi.app.ui.common.RichBlockEditor
import com.tixi.app.ui.common.TixiButton
import com.tixi.app.ui.common.TixiButtonSize
import com.tixi.app.ui.common.TixiButtonVariant
import com.tixi.app.ui.common.TixiChip
import com.tixi.app.ui.common.TixiIconButton
import com.tixi.app.ui.common.TixiSnackbarHost
import com.tixi.app.ui.common.TixiTextField
import com.tixi.app.ui.common.TixiTopBar
import com.tixi.app.ui.theme.TixiFontFamilies
import com.tixi.app.ui.theme.TixiRadius
import com.tixi.app.ui.theme.TixiSpacing
import com.tixi.app.ui.theme.TixiTheme
import androidx.compose.material3.SnackbarHostState

@Composable
fun QuestionEditScreen(
    onBack: () -> Unit,
    viewModel: QuestionEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = TixiTheme.colors
    val snackbarHostState = remember { SnackbarHostState() }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var pendingReplaceIndex by remember { mutableIntStateOf(-1) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            if (pendingReplaceIndex >= 0) {
                viewModel.replaceImage(pendingReplaceIndex, uri, "题目原图")
                pendingReplaceIndex = -1
            } else {
                viewModel.attachImage(uri)
            }
        }
    }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }
    LaunchedEffect(state.back) {
        if (state.back) {
            viewModel.consumeBack()
            onBack()
        }
    }

    Box(Modifier.fillMaxSize().background(colors.canvas)) {
        Column(Modifier.fillMaxSize()) {
            TixiTopBar(
                title = if (state.isNew) "新建题目" else "编辑题目",
                onBack = onBack,
                trailingText = state.numberLabel,
            )

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = TixiSpacing.pageHorizontal),
            ) {
                Spacer(Modifier.height(6.dp))

                // 题型切换器：内置三种 + 自定义题型
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val types = if (state.types.isEmpty()) {
                        BuiltInQuestionType.entries.map { it.key to it.label }
                    } else {
                        state.types.map { it.key to it.name }
                    }
                    types.take(4).forEach { (key, label) ->
                        TixiChip(
                            text = label,
                            selected = state.typeKey == key,
                            onClick = { viewModel.switchType(key) },
                        )
                    }
                }

                FieldLabel("题干")
                TixiTextField(
                    value = state.stem,
                    onValueChange = viewModel::updateStem,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "粘贴或输入题干；填空题用 ____ 标记空位",
                    singleLine = false,
                    minLines = 4,
                )

                Spacer(Modifier.height(20.dp))
                when (state.builtInType) {
                    BuiltInQuestionType.CHOICE -> {
                        FieldLabel("选项")
                        state.options.forEachIndexed { index, option ->
                            Row(
                                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(colors.surfaceVariant),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        ('A' + index).toString(),
                                        fontFamily = TixiFontFamilies.Mono,
                                        fontSize = 11.sp,
                                        color = colors.textSecondary,
                                    )
                                }
                                Spacer(Modifier.size(8.dp))
                                TixiTextField(
                                    value = option,
                                    onValueChange = { viewModel.updateOption(index, it) },
                                    modifier = Modifier.weight(1f),
                                    placeholder = "选项内容",
                                )
                                if (state.options.size > 2) {
                                    TixiIconButton(
                                        Icons.Outlined.Close,
                                        "删除选项",
                                        onClick = { viewModel.removeOption(index) },
                                        buttonSize = 36.dp,
                                        tint = colors.textTertiary,
                                    )
                                }
                            }
                        }
                        TixiButton(
                            text = "添加选项",
                            onClick = viewModel::addOption,
                            variant = TixiButtonVariant.Text,
                            size = TixiButtonSize.Small,
                            leadingIcon = Icons.Outlined.Add,
                        )
                        Spacer(Modifier.height(16.dp))
                        FieldLabel("正确答案（可多选）")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.options.indices.forEach { index ->
                                val key = ('A' + index).toString()
                                TixiChip(
                                    text = key,
                                    selected = key in state.answerKeys,
                                    onClick = { viewModel.toggleAnswerKey(key) },
                                )
                            }
                        }
                    }

                    BuiltInQuestionType.FILL -> {
                        FieldLabel("标准答案")
                        TixiTextField(
                            value = state.fillAnswer,
                            onValueChange = viewModel::updateFillAnswer,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = "多个空用逗号分隔，例如：60 日, 30 日",
                            mono = true,
                        )
                        Spacer(Modifier.height(8.dp))
                        InfoNote("填空题不自动判分：提交后展示标准答案与解析，由你自评掌握情况。")
                    }

                    else -> {
                        FieldLabel("参考答案")
                        TixiTextField(
                            value = state.essayAnswer,
                            onValueChange = viewModel::updateEssayAnswer,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = "录入参考答案全文",
                            singleLine = false,
                            minLines = 4,
                        )
                        Spacer(Modifier.height(8.dp))
                        InfoNote("大题在练习页只做展示：没有选项、提交与自评。")
                    }
                }

                Spacer(Modifier.height(20.dp))
                FieldLabel("解析")
                TixiTextField(
                    value = state.explanation,
                    onValueChange = viewModel::updateExplanation,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "解析正文（可留空）",
                    singleLine = false,
                    minLines = 3,
                )

                Spacer(Modifier.height(20.dp))
                FieldLabel("归入分类")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(TixiRadius.sm))
                        .clickable { showCategoryPicker = true }
                        .background(colors.surface)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        state.categories.firstOrNull { it.id == state.categoryId }?.name ?: "未分类",
                        fontFamily = TixiFontFamilies.Serif,
                        fontSize = 14.sp,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Text("更换", fontSize = 12.sp, color = colors.primary)
                }

                Spacer(Modifier.height(20.dp))
                FieldLabel("标签")
                TixiTextField(
                    value = state.tagsText,
                    onValueChange = viewModel::updateTags,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "用逗号分隔，例如：行政复议, 期限",
                )

                Spacer(Modifier.height(12.dp))
                FieldLabel("备注")
                TixiTextField(
                    value = state.remark,
                    onValueChange = viewModel::updateRemark,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "个人备注（可留空）",
                )

                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FieldLabel("题目素材", modifier = Modifier.weight(1f))
                    TixiButton(
                        text = "表格",
                        onClick = viewModel::addTableBlock,
                        variant = TixiButtonVariant.Text,
                        size = TixiButtonSize.Small,
                        leadingIcon = Icons.Outlined.TableChart,
                    )
                    TixiButton(
                        text = "公式",
                        onClick = viewModel::addFormulaBlock,
                        variant = TixiButtonVariant.Text,
                        size = TixiButtonSize.Small,
                        leadingIcon = Icons.Outlined.Title,
                    )
                    TixiButton(
                        text = "原图",
                        onClick = {
                            pendingReplaceIndex = -1
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        variant = TixiButtonVariant.Text,
                        size = TixiButtonSize.Small,
                        leadingIcon = Icons.Outlined.Image,
                    )
                }
                if (state.richBlocks.isEmpty()) {
                    InfoNote("可插入结构化表格、LaTeX 公式与原图。图内文字不可编辑，只能替换或删除。")
                } else {
                    RichBlockEditor(
                        blocks = state.richBlocks,
                        onChange = viewModel::updateRichBlocks,
                        onRequestReplaceImage = { index ->
                            pendingReplaceIndex = index
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                    )
                }

                Spacer(Modifier.height(28.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(TixiRadius.sm))
                        .clickable { showDeleteConfirm = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = colors.error,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("删除这道题目", fontSize = 13.sp, color = colors.error)
                }
                Spacer(Modifier.height(TixiSpacing.s8))
            }

            BottomActionBar {
                TixiButton(
                    text = "上一题",
                    onClick = viewModel::goPrevious,
                    modifier = Modifier.weight(1f),
                    variant = TixiButtonVariant.Outlined,
                    enabled = state.index > 0 && !state.isNew,
                )
                TixiButton(
                    text = "保存并下一题",
                    onClick = { viewModel.save(advance = true) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        TixiSnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp))
    }

    if (showCategoryPicker) {
        CategoryPickerDialog(
            categories = state.categories,
            selected = state.categoryId,
            onSelect = {
                viewModel.setCategory(it)
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false },
        )
    }

    if (showDeleteConfirm) {
        com.tixi.app.ui.common.TixiDialog(
            title = "删除这道题目？",
            message = "删除后不可恢复，分类内的题号会重新排列。",
            confirmLabel = "删除",
            danger = true,
            onConfirm = {
                showDeleteConfirm = false
                viewModel.delete()
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

@Composable
private fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = TixiTheme.colors.textSecondary,
        modifier = modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun CategoryPickerDialog(
    categories: List<Category>,
    selected: Long?,
    onSelect: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = TixiTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("选择分类", fontFamily = TixiFontFamilies.Serif, fontSize = 18.sp, color = colors.textPrimary) },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                (listOf<Pair<Long?, String>>(null to "未分类") + categories.map { it.id to it.name }).forEach { (id, name) ->
                    val isSelected = id == selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(id) }
                            .background(if (isSelected) colors.surfaceVariant else colors.surface)
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            name,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) Text("当前", fontSize = 11.sp, color = colors.primary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭", color = colors.primary) }
        },
    )
}
