package com.tixi.app.ui.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tixi.app.ui.common.BottomActionBar
import com.tixi.app.ui.common.EmptyState
import com.tixi.app.ui.common.ProgressBar
import com.tixi.app.ui.common.TixiIconButton
import com.tixi.app.ui.common.TixiButton
import com.tixi.app.ui.common.TixiButtonSize
import com.tixi.app.ui.common.TixiButtonVariant
import com.tixi.app.ui.theme.TixiFontFamilies
import com.tixi.app.ui.theme.TixiRadius
import com.tixi.app.ui.theme.TixiSpacing
import com.tixi.app.ui.theme.TixiTheme

@Composable
fun HomeScreen(
    onOpenCategoryList: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenCategory: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = TixiTheme.colors

    Column(Modifier.fillMaxSize().background(colors.canvas)) {
        // 品牌行：只留「题隙」文字，不设方形字标（设计 §10 #5）
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = TixiSpacing.s4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "题隙",
                fontFamily = TixiFontFamilies.Serif,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            TixiIconButton(Icons.Outlined.History, "导入记录", onOpenHistory)
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TixiSpacing.s4),
        ) {
            Spacer(Modifier.height(TixiSpacing.s2))
            Text(
                "今天，刷点什么？",
                fontFamily = TixiFontFamilies.Serif,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "本机题库 · 共 ${state.totalQuestions} 题 · ${state.categoryCount} 个分类",
                fontSize = 12.sp,
                color = colors.textSecondary,
            )

            if (state.totalQuestions == 0 && !state.loading) {
                Spacer(Modifier.height(TixiSpacing.s8))
                EmptyState(
                    icon = Icons.Outlined.UploadFile,
                    message = "题库还是空的",
                    hint = "先配置解析 Token，再导入第一份题目。",
                )
                Spacer(Modifier.height(TixiSpacing.s4))
                TixiButton(
                    text = "去导入题目",
                    onClick = onOpenImport,
                    modifier = Modifier.fillMaxWidth(),
                    size = TixiButtonSize.Large,
                    leadingIcon = Icons.Outlined.UploadFile,
                )
            } else {
                SectionLabel("当前进度")
                val current = state.current
                if (current == null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(TixiRadius.md))
                            .background(colors.surface)
                            .padding(14.dp),
                    ) {
                        Text("还没有练习进度，从第一个分类开始。", fontSize = 12.sp, color = colors.textSecondary)
                    }
                } else {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(TixiRadius.md))
                            .background(colors.surface)
                            .padding(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                current.name,
                                fontFamily = TixiFontFamilies.Serif,
                                fontSize = 16.sp,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${current.index} / ${current.total}",
                                fontFamily = TixiFontFamilies.Mono,
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        ProgressBar(current.fraction)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            buildString {
                                append("上次练到第 ${current.index} 题")
                                current.lastStem?.let { append(" · $it") }
                            },
                            fontSize = 11.sp,
                            color = colors.textTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                SectionLabel("快捷入口")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeNavCard(
                        title = "开始练习",
                        desc = "按分类顺序\n接着上次刷",
                        icon = Icons.Outlined.PlayArrow,
                        filled = true,
                        onClick = onOpenCategoryList,
                        modifier = Modifier.weight(1f),
                    )
                    HomeNavCard(
                        title = "题库管理",
                        desc = "导入 · 分类\n编辑题目",
                        icon = Icons.Outlined.FolderOpen,
                        filled = false,
                        onClick = onOpenLibrary,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (state.recent.isNotEmpty()) {
                    SectionLabel("最近练习")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.recent.forEach { recent ->
                            Row(
                                Modifier
                                    .clip(RoundedCornerShape(TixiRadius.full))
                                    .background(colors.surface)
                                    .clickable { onOpenCategory(recent.categoryId) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .heightIn(min = 24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(recent.name, fontSize = 12.sp, color = colors.textPrimary)
                                Text(
                                    "第${recent.index}题",
                                    fontFamily = TixiFontFamilies.Mono,
                                    fontSize = 11.sp,
                                    color = colors.textTertiary,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(TixiSpacing.s12))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = TixiTheme.colors.textTertiary,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(top = 18.dp, bottom = 10.dp),
    )
}

@Composable
private fun HomeNavCard(
    title: String,
    desc: String,
    icon: ImageVector,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TixiTheme.colors
    val bg = if (filled) colors.primary else colors.surface
    val fg = if (filled) colors.onPrimary else colors.textPrimary
    Column(
        modifier
            .heightIn(min = 128.dp)
            .clip(RoundedCornerShape(TixiRadius.lg))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (filled) colors.onPrimary.copy(alpha = 0.16f) else colors.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (filled) colors.onPrimary else colors.primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(
            title,
            fontFamily = TixiFontFamilies.Serif,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = fg,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            desc,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = if (filled) fg.copy(alpha = 0.72f) else colors.textSecondary,
        )
    }
}
