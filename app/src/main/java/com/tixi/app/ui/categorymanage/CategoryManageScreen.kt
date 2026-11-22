package com.tixi.app.ui.categorymanage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tixi.app.domain.model.Category
import com.tixi.app.ui.common.Badge
import com.tixi.app.ui.common.BadgeTone
import com.tixi.app.ui.common.InfoNote
import com.tixi.app.ui.common.TixiButton
import com.tixi.app.ui.common.TixiButtonSize
import com.tixi.app.ui.common.TixiButtonVariant
import com.tixi.app.ui.common.TixiDialog
import com.tixi.app.ui.common.TixiIconButton
import com.tixi.app.ui.common.TixiSnackbarHost
import com.tixi.app.ui.common.TixiTextField
import com.tixi.app.ui.common.TixiTopBar
import com.tixi.app.ui.theme.TixiFontFamilies
import com.tixi.app.ui.theme.TixiSpacing
import com.tixi.app.ui.theme.TixiTheme

@Composable
fun CategoryManageScreen(
    onBack: () -> Unit,
    viewModel: CategoryManageViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = TixiTheme.colors
    val snackbarHostState = remember { SnackbarHostState() }
    var newName by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Category?>(null) }
    var editingName by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    Box(Modifier.fillMaxSize().background(colors.canvas)) {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {            TixiTopBar(title = "分类管理", onBack = onBack)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = TixiSpacing.pageHorizontal),
            ) {
                Spacer(Modifier.height(4.dp))
                InfoNote("删除分类后，该分类下的题目会移入「未分类」，不会丢失。")

                state.categories.forEachIndexed { index, category ->
                    if (editing?.id == category.id) {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TixiTextField(
                                value = editingName,
                                onValueChange = { editingName = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                            )
                            Spacer(Modifier.size(8.dp))
                            TixiButton(
                                text = "保存",
                                onClick = {
                                    viewModel.rename(category.id, editingName)
                                    editing = null
                                },
                                variant = TixiButtonVariant.Tonal,
                                size = TixiButtonSize.Small,
                            )
                        }
                    } else {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                category.name,
                                fontFamily = TixiFontFamilies.Serif,
                                fontSize = 14.sp,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${category.questionCount} 题",
                                fontFamily = TixiFontFamilies.Mono,
                                fontSize = 12.sp,
                                color = colors.textTertiary,
                            )
                            TixiIconButton(
                                Icons.Outlined.Edit,
                                "改名",
                                onClick = {
                                    editing = category
                                    editingName = category.name
                                },
                                buttonSize = 36.dp,
                            )
                            TixiIconButton(
                                Icons.Outlined.DeleteOutline,
                                "删除分类",
                                onClick = { deleting = category },
                                buttonSize = 36.dp,
                                tint = colors.error,
                            )
                        }
                    }
                    if (index != state.categories.lastIndex || state.uncategorizedCount > 0) {
                        HorizontalDivider(thickness = 1.dp, color = colors.outline)
                    }
                }

                if (state.uncategorizedCount > 0) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "未分类",
                            fontFamily = TixiFontFamilies.Serif,
                            fontSize = 14.sp,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${state.uncategorizedCount} 题",
                            fontFamily = TixiFontFamilies.Mono,
                            fontSize = 12.sp,
                            color = colors.textTertiary,
                        )
                        Spacer(Modifier.size(8.dp))
                        Badge("系统", BadgeTone.Neutral)
                    }
                }

                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TixiTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        modifier = Modifier.weight(1f),
                        placeholder = "分类名称",
                        singleLine = true,
                    )
                    Spacer(Modifier.size(8.dp))
                    TixiButton(
                        text = "新建",
                        onClick = {
                            viewModel.create(newName)
                            newName = ""
                        },
                        variant = TixiButtonVariant.Tonal,
                        size = TixiButtonSize.Small,
                        leadingIcon = Icons.Outlined.Add,
                    )
                }
                Spacer(Modifier.height(TixiSpacing.s8))
            }
        }
        TixiSnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
    }

    deleting?.let { category ->
        TixiDialog(
            title = "删除「${category.name}」？",
            message = "该分类下的 ${category.questionCount} 道题目会移入「未分类」，题目本身不会删除。",
            confirmLabel = "删除分类",
            danger = true,
            onConfirm = {
                viewModel.delete(category)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}
