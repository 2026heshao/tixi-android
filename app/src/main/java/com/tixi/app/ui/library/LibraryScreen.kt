package com.tixi.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tixi.app.ui.common.Badge
import com.tixi.app.ui.common.EmptyState
import com.tixi.app.ui.common.ImportCard
import com.tixi.app.ui.common.TixiButton
import com.tixi.app.ui.common.TixiButtonSize
import com.tixi.app.ui.common.TixiButtonVariant
import com.tixi.app.ui.common.TixiChip
import com.tixi.app.ui.common.TixiIconButton
import com.tixi.app.ui.common.TixiTextField
import com.tixi.app.ui.common.TixiTopBar
import com.tixi.app.ui.theme.TixiFontFamilies
import com.tixi.app.ui.theme.TixiSpacing
import com.tixi.app.ui.theme.TixiTheme

@Composable
fun LibraryScreen(
    onOpenImport: () -> Unit,
    onOpenCategoryManage: () -> Unit,
    onEditQuestion: (Long) -> Unit,
    onCreateQuestion: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = TixiTheme.colors

    Column(Modifier.fillMaxSize().background(colors.canvas).navigationBarsPadding()) {
        TixiTopBar(
            title = "题库",
            actions = {
                TixiIconButton(Icons.Outlined.CreateNewFolder, "分类管理", onOpenCategoryManage)
                TixiIconButton(Icons.Outlined.Add, "新建题目", onCreateQuestion)
            },
        )

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 28.dp),
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                ImportCard(hasToken = state.hasToken, onClick = onOpenImport)
                TixiTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "搜索题干或标签",
                    singleLine = true,
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TixiChip(
                        text = "全部 ${state.questions.size}",
                        selected = !state.filterActive,
                        onClick = viewModel::clearFilter,
                    )
                    state.categories.forEach { category ->
                        TixiChip(
                            text = "${category.name} ${state.countOf(category.id)}",
                            selected = state.filterActive && state.filterCategoryId == category.id,
                            onClick = { viewModel.setFilter(category.id) },
                        )
                    }
                    val uncategorized = state.countOf(null)
                    if (uncategorized > 0) {
                        TixiChip(
                            text = "未分类 $uncategorized",
                            selected = state.filterActive && state.filterCategoryId == null,
                            onClick = { viewModel.setFilter(null) },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            val visible = state.visible
            if (visible.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        icon = if (state.query.isBlank()) Icons.Outlined.FolderOpen else Icons.Outlined.Search,
                        message = if (state.query.isBlank()) "还没有题目" else "没有匹配的题目",
                        hint = if (state.query.isBlank()) "导入一份题目，或手工新建一题。" else "换个关键词，或清空搜索。",
                        actionLabel = if (state.query.isBlank()) "去导入" else "清空搜索",
                        onAction = { if (state.query.isBlank()) onOpenImport else viewModel.clearSearch() },
                    )
                }
            }

            itemsIndexed(visible, key = { _, question -> question.id }) { index, question ->
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onEditQuestion(question.id) }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            (index + 1).toString().padStart(2, '0'),
                            fontFamily = TixiFontFamilies.Mono,
                            fontSize = 12.sp,
                            color = colors.textTertiary,
                            modifier = Modifier.width(26.dp).padding(top = 2.dp),
                        )
                        Text(
                            question.stem,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = colors.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(10.dp))
                        Badge(text = state.categoryName(question.categoryId))
                    }
                    HorizontalDivider(thickness = 1.dp, color = colors.outline)
                }
            }

            if (visible.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    TixiButton(
                        text = "新建题目",
                        onClick = onCreateQuestion,
                        modifier = Modifier.fillMaxWidth(),
                        variant = TixiButtonVariant.Tonal,
                        size = TixiButtonSize.Medium,
                        leadingIcon = Icons.Outlined.Add,
                    )
                }
            }
        }
    }
}
