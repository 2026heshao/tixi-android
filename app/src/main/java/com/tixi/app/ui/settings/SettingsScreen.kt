package com.tixi.app.ui.settings

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tixi.app.domain.model.ThemeMode
import com.tixi.app.ui.common.Badge
import com.tixi.app.ui.common.BadgeTone
import com.tixi.app.ui.common.InfoNote
import com.tixi.app.ui.common.SectionCard
import com.tixi.app.ui.common.SegmentedControl
import com.tixi.app.ui.common.SettingRow
import com.tixi.app.ui.common.SwitchRow
import com.tixi.app.ui.common.TixiButton
import com.tixi.app.ui.common.TixiButtonSize
import com.tixi.app.ui.common.TixiButtonVariant
import com.tixi.app.ui.common.TixiChip
import com.tixi.app.ui.common.TixiDialog
import com.tixi.app.ui.common.TixiIconButton
import com.tixi.app.ui.common.TixiSnackbarHost
import com.tixi.app.ui.common.TixiTextField
import com.tixi.app.ui.common.TokenField
import com.tixi.app.ui.common.TixiTopBar
import com.tixi.app.ui.theme.TixiFontFamilies
import com.tixi.app.ui.theme.TixiSpacing
import com.tixi.app.ui.theme.TixiTheme

private data class ManageItem(
    val key: String,
    val name: String,
    val count: Int,
    val deletable: Boolean,
    val system: Boolean = false,
)

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = TixiTheme.colors
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    Box(Modifier.fillMaxSize().background(colors.canvas)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TixiSpacing.pageHorizontal),
        ) {
            TixiTopBar(title = "设置", large = true, titleSerif = true)

            SectionCard(title = "MinerU Token") {
                TokenField(
                    token = state.tokenInput,
                    onTokenChange = viewModel::updateToken,
                    isConfigured = state.configured,
                    onSave = viewModel::saveToken,
                    onClear = viewModel::clearToken,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                InfoNote(
                    if (state.configured) {
                        "已启用精准通道：支持 200MB 大文件与表格、公式识别。清空 Token 会自动退回轻量通道。"
                    } else {
                        "未配置时自动使用免注册的轻量通道：单文件 10MB / 20 页，表格与公式识别不保证。填入 Token 即可升级。"
                    },
                )
            }
            Spacer(Modifier.height(12.dp))

            // 位置固定：凭证 → 内容 → 外观 → 参数 → 数据（设计 §11.5）
            ManageCard(
                title = "题型管理",
                note = "编辑题目时选择题型，可在此增删。删除题型不会丢失已有题目。",
                rows = state.types.map { ManageItem(it.key, it.name, it.questionCount, deletable = !it.builtIn) },
                addPlaceholder = "题型名称，如 判断题",
                addLabel = "新增题型",
                onRename = viewModel::renameType,
                onDelete = viewModel::deleteType,
                onAdd = viewModel::addType,
            )
            Spacer(Modifier.height(12.dp))

            ManageCard(
                title = "分类管理",
                note = "编辑题目时选择分类，可在此增删与改名。删除后题目移入「未分类」。",
                rows = state.categories.map { ManageItem(it.id.toString(), it.name, it.questionCount, deletable = true) } +
                    if (state.uncategorizedCount > 0) {
                        listOf(ManageItem("uncategorized", "未分类", state.uncategorizedCount, deletable = false, system = true))
                    } else {
                        emptyList()
                    },
                addPlaceholder = "分类名称",
                addLabel = "新建分类",
                onRename = { key, name -> key.toLongOrNull()?.let { viewModel.renameCategory(it, name) } },
                onDelete = { key -> key.toLongOrNull()?.let { viewModel.deleteCategory(it) } },
                onAdd = viewModel::addCategory,
            )
            Spacer(Modifier.height(12.dp))

            SectionCard(title = "外观主题") {
                SegmentedControl(
                    options = ThemeMode.entries.map { it.label },
                    selectedIndex = ThemeMode.entries.indexOf(state.themeMode).coerceAtLeast(0),
                    onSelect = { index -> viewModel.setTheme(ThemeMode.entries[index]) },
                )
            }
            Spacer(Modifier.height(12.dp))

            SectionCard(title = "解析参数") {
                SettingRow(
                    title = "识别语言",
                    subtitle = "中文题目",
                    value = state.language,
                )
                SwitchRow(
                    title = "启用表格解析",
                    subtitle = "识别题目中的表格结构",
                    checked = state.enableTable,
                    onCheckedChange = viewModel::setEnableTable,
                )
                SwitchRow(
                    title = "启用公式解析",
                    subtitle = "识别 LaTeX 公式",
                    checked = state.enableFormula,
                    onCheckedChange = viewModel::setEnableFormula,
                )
                Spacer(Modifier.height(10.dp))
                Text("轮询间隔", fontSize = 13.sp, color = colors.textPrimary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3, 5, 10).forEach { seconds ->
                        TixiChip(
                            text = "$seconds s",
                            selected = state.pollSeconds == seconds,
                            onClick = { viewModel.setPollSeconds(seconds) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            SectionCard(title = "数据") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { showClearConfirm = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = colors.error,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        if (state.busy) "正在清空…" else "清空所有题库数据",
                        fontSize = 13.sp,
                        color = colors.error,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "题隙 Android v0.1.0 · 本地存储 · 不联网也能刷",
                fontFamily = TixiFontFamilies.Mono,
                fontSize = 11.sp,
                color = colors.textTertiary,
            )
            Spacer(Modifier.height(TixiSpacing.s8))
        }

        TixiSnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
    }

    if (showClearConfirm) {
        TixiDialog(
            title = "清空所有题库数据？",
            message = "题目、分类、导入记录与草稿都会被删除，且不可恢复。Token 与设置保留。",
            confirmLabel = "全部清空",
            danger = true,
            onConfirm = {
                showClearConfirm = false
                viewModel.clearAllData()
            },
            onDismiss = { showClearConfirm = false },
        )
    }
}

@Composable
private fun ManageCard(
    title: String,
    note: String,
    rows: List<ManageItem>,
    addPlaceholder: String,
    addLabel: String,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onAdd: (String) -> Unit,
) {
    val colors = TixiTheme.colors
    var adding by remember { mutableStateOf(false) }
    var addText by remember { mutableStateOf("") }
    var editingKey by remember { mutableStateOf<String?>(null) }
    var editingText by remember { mutableStateOf("") }

    SectionCard(title = title) {
        Text(note, fontSize = 11.sp, lineHeight = 17.sp, color = colors.textTertiary)
        Spacer(Modifier.height(10.dp))

        rows.forEachIndexed { index, item ->
            if (!item.system && editingKey == item.key) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TixiTextField(
                        value = editingText,
                        onValueChange = { editingText = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Spacer(Modifier.size(8.dp))
                    TixiButton(
                        text = "保存",
                        onClick = {
                            onRename(item.key, editingText)
                            editingKey = null
                        },
                        variant = TixiButtonVariant.Tonal,
                        size = TixiButtonSize.Small,
                    )
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        item.name,
                        fontFamily = TixiFontFamilies.Serif,
                        fontSize = 13.sp,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${item.count} 题",
                        fontFamily = TixiFontFamilies.Mono,
                        fontSize = 11.sp,
                        color = colors.textTertiary,
                    )
                    if (item.system) {
                        Spacer(Modifier.size(8.dp))
                        Badge("系统", BadgeTone.Neutral)
                    } else {
                        TixiIconButton(
                            Icons.Outlined.Edit,
                            "改名",
                            onClick = {
                                editingKey = item.key
                                editingText = item.name
                            },
                            buttonSize = 32.dp,
                        )
                        if (item.deletable) {
                            TixiIconButton(
                                Icons.Outlined.DeleteOutline,
                                "删除",
                                onClick = { onDelete(item.key) },
                                buttonSize = 32.dp,
                            )
                        }
                    }
                }
            }
            if (index != rows.lastIndex) {
                HorizontalDivider(thickness = 1.dp, color = colors.outline)
            }
        }

        if (adding) {
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TixiTextField(
                    value = addText,
                    onValueChange = { addText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = addPlaceholder,
                    singleLine = true,
                )
                Spacer(Modifier.size(8.dp))
                TixiButton(
                    text = "添加",
                    onClick = {
                        if (addText.isNotBlank()) {
                            onAdd(addText)
                            addText = ""
                            adding = false
                        }
                    },
                    variant = TixiButtonVariant.Tonal,
                    size = TixiButtonSize.Small,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        TixiButton(
            text = addLabel,
            onClick = { adding = !adding },
            modifier = Modifier.fillMaxWidth(),
            variant = TixiButtonVariant.Tonal,
            size = TixiButtonSize.Small,
            leadingIcon = Icons.Outlined.Add,
        )
    }
}
