package com.tixi.app.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tixi.app.domain.model.Category
import com.tixi.app.ui.common.CategoryCard
import com.tixi.app.ui.common.EmptyState
import com.tixi.app.ui.common.TixiChip
import com.tixi.app.ui.common.TixiTopBar
import com.tixi.app.ui.theme.TixiSpacing
import com.tixi.app.ui.theme.TixiTheme

@Composable
fun CategoryScreen(
    onBack: () -> Unit,
    onStartPractice: (Long) -> Unit,
    onOpenImport: () -> Unit,
    viewModel: CategoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = TixiTheme.colors

    Column(Modifier.fillMaxSize().background(colors.canvas).navigationBarsPadding()) {
        TixiTopBar(title = "选择分类", onBack = onBack)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TixiSpacing.pageHorizontal),
        ) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryFilter.entries.forEach { filter ->
                    TixiChip(
                        text = filter.label,
                        selected = state.filter == filter,
                        onClick = { viewModel.setFilter(filter) },
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            val visible = state.visible
            when {
                visible.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.FolderOpen,
                    message = if (state.rows.isEmpty()) "还没有分类" else "这个筛选下没有分类",
                    hint = "去题库导入题目，或先建一个分类。",
                    actionLabel = "去导入题目",
                    onAction = onOpenImport,
                )

                else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    visible.forEach { row ->
                        CategoryCard(
                            name = row.name,
                            count = row.count,
                            meta = when {
                                row.id == null -> "删除分类的题目移入此处"
                                row.hasProgress -> "上次练到第 ${row.lastIndex} 题"
                                else -> if (row.count == 0) "还没有题目" else "尚未开始"
                            },
                            isCurrent = row.hasProgress,
                            onClick = { row.id?.let(onStartPractice) ?: onStartPractice(Category.UNCATEGORIZED_ID) },
                            onContinue = if (row.count > 0) {
                                { row.id?.let(onStartPractice) ?: onStartPractice(Category.UNCATEGORIZED_ID) }
                            } else null,
                        )
                    }
                }
            }

            Spacer(Modifier.height(TixiSpacing.s8))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("题目按分类内顺序练习", fontSize = 11.sp, color = colors.textTertiary)
            }
            Spacer(Modifier.height(TixiSpacing.s12))
        }
    }
}
