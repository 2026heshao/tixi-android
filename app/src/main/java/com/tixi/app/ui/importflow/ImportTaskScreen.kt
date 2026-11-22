package com.tixi.app.ui.importflow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tixi.app.domain.model.ParseProgress
import com.tixi.app.ui.common.Badge
import com.tixi.app.ui.common.BadgeTone
import com.tixi.app.ui.common.BottomActionBar
import com.tixi.app.ui.common.EmptyState
import com.tixi.app.ui.common.InfoNote
import com.tixi.app.ui.common.ProgressBar
import com.tixi.app.ui.common.TixiButton
import com.tixi.app.ui.common.TixiButtonSize
import com.tixi.app.ui.common.TixiButtonVariant
import com.tixi.app.ui.common.TixiTopBar
import com.tixi.app.ui.theme.TixiFontFamilies
import com.tixi.app.ui.theme.TixiRadius
import com.tixi.app.ui.theme.TixiSpacing
import com.tixi.app.ui.theme.TixiTheme

@Composable
fun ImportTaskScreen(
    onBack: () -> Unit,
    onOpenReview: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ImportTaskViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = TixiTheme.colors

    LaunchedEffect(state) {
        val ready = state as? ImportRunState.DraftReady ?: return@LaunchedEffect
        viewModel.consumeDraftReady()
        onOpenReview(ready.draftId)
    }

    Column(Modifier.fillMaxSize().background(colors.canvas)) {
        TixiTopBar(
            title = "识别任务",
            actions = {
                if (state is ImportRunState.Running) {
                    TixiButton(
                        text = "取消",
                        onClick = {
                            viewModel.cancel()
                            onBack()
                        },
                        variant = TixiButtonVariant.Text,
                        size = TixiButtonSize.Small,
                    )
                }
            },
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TixiSpacing.pageHorizontal),
        ) {
            when (val current = state) {
                is ImportRunState.Idle -> {
                    Spacer(Modifier.height(TixiSpacing.s12))
                    EmptyState(
                        icon = Icons.Outlined.FolderOpen,
                        message = "没有进行中的识别任务",
                        hint = "从题库页选择图片或文件重新导入。",
                        actionLabel = "返回",
                        onAction = onBack,
                    )
                }

                is ImportRunState.Running -> {
                    Spacer(Modifier.height(6.dp))
                    FileChip(name = current.fileName, sizeLabel = current.sizeLabel)
                    Stepper(
                        stageIndex = viewModel.stageIndex(current.progress),
                        stageText = viewModel.stageText(current.progress),
                    )
                    Spacer(Modifier.height(4.dp))
                    ProgressBar(current.progress.overallPercent() / 100f)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "${current.progress.overallPercent()}% · ${viewModel.stageText(current.progress)}",
                        fontFamily = TixiFontFamilies.Mono,
                        fontSize = 11.sp,
                        color = colors.textTertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(20.dp))
                    InfoNote("解析在后台进行，离开本页不会中断。识别结果会先生成草稿，等你在确认页核对。")
                }

                is ImportRunState.Failed -> {
                    Spacer(Modifier.height(6.dp))
                    FileChip(name = current.fileName, sizeLabel = "")
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(TixiRadius.sm))
                            .background(colors.errorContainer)
                            .padding(14.dp),
                    ) {
                        Text(
                            current.error.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.error,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(current.error.hint, fontSize = 11.sp, lineHeight = 17.sp, color = colors.textSecondary)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TixiButton(
                                text = current.error.actionLabel ?: "重试",
                                onClick = {
                                    if (current.error.actionLabel?.contains("Token") == true) {
                                        onOpenSettings()
                                    } else {
                                        viewModel.retry()
                                    }
                                },
                                variant = TixiButtonVariant.Outlined,
                                size = TixiButtonSize.Small,
                            )
                            TixiButton(
                                text = "重新选择",
                                onClick = onBack,
                                variant = TixiButtonVariant.Text,
                                size = TixiButtonSize.Small,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    InfoNote("原始文件已保留在本地，修复问题后可直接重试，不必重新选择文件。")
                }

                is ImportRunState.DraftReady -> {
                    Spacer(Modifier.height(TixiSpacing.s12))
                    EmptyState(
                        icon = Icons.Outlined.Check,
                        message = "已生成 ${current.questionCount} 道草稿",
                        hint = if (current.needsReviewCount > 0) {
                            "其中 ${current.needsReviewCount} 题需要人工核对。"
                        } else {
                            "正在打开确认页…"
                        },
                    )
                }
            }
            Spacer(Modifier.height(TixiSpacing.s8))
        }

        if (state is ImportRunState.Running) {
            BottomActionBar {
                TixiButton("后台运行", onBack, Modifier.weight(1f), variant = TixiButtonVariant.Outlined)
                TixiButton(
                    text = "查看草稿",
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    enabled = false,
                )
            }
        }
    }
}

@Composable
private fun FileChip(name: String, sizeLabel: String) {
    val colors = TixiTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TixiRadius.md))
            .background(colors.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(TixiRadius.sm)).background(colors.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Description, contentDescription = null, tint = colors.secondary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 13.sp, color = colors.textPrimary, maxLines = 2)
            if (sizeLabel.isNotBlank()) {
                Text(sizeLabel, fontFamily = TixiFontFamilies.Mono, fontSize = 11.sp, color = colors.textTertiary)
            }
        }
        Badge("v4 精准接口", BadgeTone.Neutral)
    }
}

@Composable
private fun Stepper(stageIndex: Int, stageText: String) {
    val colors = TixiTheme.colors
    val stages = listOf(
        "上传文件" to "对签名地址 PUT 上传，不带 Content-Type",
        "排队等待" to "已通过鉴权，等待解析资源",
        "云端识别" to "MinerU 正在解析题目",
        "下载结果" to "拉取结果包并生成草稿",
    )
    Column(Modifier.fillMaxWidth()) {
        stages.forEachIndexed { index, (title, desc) ->
            Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(23.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                index < stageIndex -> colors.success
                                index == stageIndex -> colors.canvas
                                else -> colors.canvas
                            },
                        )
                        .border(
                            1.dp,
                            if (index == stageIndex) colors.primary else colors.outline,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        index < stageIndex -> Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            tint = colors.onPrimary,
                            modifier = Modifier.size(14.dp),
                        )

                        index == stageIndex -> CircularProgressIndicator(
                            modifier = Modifier.size(13.dp),
                            strokeWidth = 2.dp,
                            color = colors.primary,
                        )

                        else -> Text(
                            (index + 1).toString(),
                            fontFamily = TixiFontFamilies.Mono,
                            fontSize = 11.sp,
                            color = colors.textTertiary,
                        )
                    }
                }
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        fontFamily = TixiFontFamilies.Serif,
                        fontSize = 14.sp,
                        color = if (index <= stageIndex) colors.textPrimary else colors.textTertiary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (index == stageIndex) stageText else desc,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = colors.textTertiary,
                    )
                }
            }
        }
    }
}
