package com.tixi.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tixi.app.ui.common.Badge
import com.tixi.app.ui.common.BadgeTone
import com.tixi.app.ui.common.BottomActionBar
import com.tixi.app.ui.common.EmptyState
import com.tixi.app.ui.common.InfoNote
import com.tixi.app.ui.common.SectionCard
import com.tixi.app.ui.common.StatusStrip
import com.tixi.app.ui.common.TixiButton
import com.tixi.app.ui.common.TixiButtonSize
import com.tixi.app.ui.common.TixiButtonVariant
import com.tixi.app.ui.common.TixiSnackbarHost
import com.tixi.app.ui.common.TixiTopBar
import com.tixi.app.ui.common.TokenField
import com.tixi.app.ui.theme.TixiFontFamilies
import com.tixi.app.ui.theme.TixiRadius
import com.tixi.app.ui.theme.TixiSpacing
import com.tixi.app.ui.theme.TixiTheme

@Composable
fun OnboardingScreen(
    onOpenImport: () -> Unit,
    onOpenLibrary: () -> Unit,
    onStartPractice: () -> Unit,
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = TixiTheme.colors
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.finished) {
        if (state.finished) onFinished()
    }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    Box(Modifier.fillMaxSize().background(colors.canvas)) {
        Column(Modifier.fillMaxSize()) {
            when (state.step) {
                1 -> StepEmpty(
                    configured = state.configured,
                    onStart = { viewModel.goTo(2) },
                    onConfigure = { viewModel.goTo(2) },
                )

                2 -> {
                    TixiTopBar(
                        title = "第 1 步 · 配置 Token",
                        onBack = { viewModel.goTo(1) },
                        trailingText = "1 / 3",
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = TixiSpacing.pageHorizontal),
                    ) {
                        InfoNote("题隙用你自己的 MinerU Token 解析题目。密钥经 Keystore + AES-GCM 加密，仅存本机，不参与云备份。")
                        SectionCard(title = "MinerU Token") {
                            TokenField(
                                token = state.tokenInput,
                                onTokenChange = viewModel::updateToken,
                                isConfigured = state.configured,
                                onSave = viewModel::saveToken,
                                onClear = {
                                    viewModel.updateToken("")
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        SectionCard(title = "申请步骤") {
                            listOf(
                                "注册 MinerU 账号" to "外部",
                                "控制台创建 API Token" to "外部",
                                "复制并粘贴到上方" to "完成",
                            ).forEachIndexed { index, (title, tag) ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "${index + 1}. $title",
                                        fontSize = 13.sp,
                                        color = colors.textPrimary,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(tag, fontFamily = TixiFontFamilies.Mono, fontSize = 11.sp, color = colors.textTertiary)
                                }
                            }
                        }
                        Spacer(Modifier.height(TixiSpacing.s8))
                    }
                    BottomActionBar {
                        TixiButton("稍后再说", { viewModel.skipToken() }, Modifier.weight(1f), variant = TixiButtonVariant.Outlined)
                        TixiButton("保存并继续", { viewModel.saveToken() }, Modifier.weight(1.4f))
                    }
                }

                3 -> {
                    TixiTopBar(
                        title = "第 2 步 · 导入题目",
                        onBack = { viewModel.goTo(2) },
                        trailingText = "2 / 3",
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = TixiSpacing.pageHorizontal),
                    ) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "导入第一份题目",
                            fontFamily = TixiFontFamilies.Serif,
                            fontSize = 28.sp,
                            lineHeight = 36.sp,
                            color = colors.textPrimary,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("支持拍照、相册、PDF 与 Word", fontSize = 12.sp, color = colors.textSecondary)
                        Spacer(Modifier.height(18.dp))

                        OnbSourceCard(
                            icon = Icons.Outlined.PhotoCamera,
                            title = "拍照 / 相册",
                            desc = "对准题目，支持多张连续识别",
                            onClick = onOpenImport,
                        )
                        Spacer(Modifier.height(14.dp))
                        OnbSourceCard(
                            icon = Icons.Outlined.Description,
                            title = "选择文件",
                            desc = "PDF · DOC · DOCX",
                            onClick = onOpenImport,
                        )
                        Spacer(Modifier.height(20.dp))
                        StatusStrip(
                            text = if (state.configured) "MinerU Token 已配置" else "MinerU Token 未配置",
                            ok = state.configured,
                            actionLabel = "去配置",
                            onAction = { viewModel.goTo(2) },
                        )
                        Spacer(Modifier.height(16.dp))
                        InfoNote("识别结果不会直接入库，会先进入确认页由你逐题核对。")
                        Spacer(Modifier.height(TixiSpacing.s8))
                    }
                    BottomActionBar {
                        TixiButton("先去工作台", { viewModel.finish() }, Modifier.weight(1f), variant = TixiButtonVariant.Outlined)
                        TixiButton("去导入", onOpenImport, Modifier.weight(1f))
                    }
                }

                else -> {
                    TixiTopBar(title = "题隙")
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = TixiSpacing.pageHorizontal),
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier.size(56.dp).border(2.dp, colors.primary, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("入", fontFamily = TixiFontFamilies.Serif, fontSize = 26.sp, color = colors.primary)
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(
                                "第一题，入库了",
                                fontFamily = TixiFontFamilies.Serif,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "题库现有 ${state.totalQuestions} 题",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                            )
                        }
                        state.firstStem?.let { stem ->
                            SectionCard(title = "已入库题目") {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("01", fontFamily = TixiFontFamilies.Mono, fontSize = 11.sp, color = colors.textTertiary)
                                    Spacer(Modifier.size(8.dp))
                                    Badge("已确认", BadgeTone.Ok)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(stem, fontFamily = TixiFontFamilies.Serif, fontSize = 14.sp, lineHeight = 22.sp, color = colors.textPrimary)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        InfoNote("接下来按分类顺序练习。答完提交后显示答案与解析，由你自评掌握情况。")
                        Spacer(Modifier.height(TixiSpacing.s8))
                    }
                    BottomActionBar {
                        TixiButton(
                            text = "去题库看看",
                            onClick = {
                                viewModel.finish()
                                onOpenLibrary()
                            },
                            modifier = Modifier.weight(1f),
                            variant = TixiButtonVariant.Outlined,
                        )
                        TixiButton(
                            text = "开始练习",
                            onClick = {
                                viewModel.finish()
                                onStartPractice()
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        TixiSnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp))
    }
}

@Composable
private fun StepEmpty(
    configured: Boolean,
    onStart: () -> Unit,
    onConfigure: () -> Unit,
) {
    val colors = TixiTheme.colors
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().height(112.dp).padding(horizontal = TixiSpacing.s4),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                "题隙",
                fontFamily = TixiFontFamilies.Serif,
                fontSize = 15.sp,
                letterSpacing = 2.sp,
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TixiSpacing.pageHorizontal),
        ) {
            Text(
                "从一页题开始",
                fontFamily = TixiFontFamilies.Serif,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text("本地题库 · 顺序练习 · 导入后可离线刷", fontSize = 12.sp, color = colors.textSecondary)

            Spacer(Modifier.height(8.dp))
            EmptyState(
                icon = Icons.Outlined.UploadFile,
                message = "题库还是空的",
                hint = "先配置解析 Token，再导入第一份题目。",
            )
            OnbStep(index = 1, title = "配置 MinerU Token", desc = "用于云端解析题目，密钥仅存本机", tag = if (configured) "已完成" else "待配置")
            OnbStep(index = 2, title = "导入第一份题目", desc = "拍照或从 PDF / Word 导入", tag = "未开始")
            OnbStep(index = 3, title = "开始顺序练习", desc = "按分类一题一题往下刷", tag = "未开始")
            Spacer(Modifier.height(20.dp))
            StatusStrip(
                text = if (configured) "MinerU Token 已配置" else "MinerU Token 未配置",
                ok = configured,
                actionLabel = if (configured) "查看" else "去配置",
                onAction = onConfigure,
            )
            Spacer(Modifier.height(TixiSpacing.s8))
        }
        BottomActionBar {
            TixiButton("开始设置", onStart, Modifier.fillMaxWidth(), size = TixiButtonSize.Medium)
        }
    }
}

@Composable
private fun OnbStep(index: Int, title: String, desc: String, tag: String) {
    val colors = TixiTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(24.dp).border(1.dp, colors.outlineStrong, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$index", fontFamily = TixiFontFamilies.Mono, fontSize = 11.sp, color = colors.primary)
        }
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = TixiFontFamilies.Serif, fontSize = 13.sp, color = colors.textPrimary)
            Spacer(Modifier.height(3.dp))
            Text(desc, fontSize = 11.sp, color = colors.textTertiary)
        }
        Badge(tag)
    }
}

@Composable
private fun OnbSourceCard(
    icon: ImageVector,
    title: String,
    desc: String,
    onClick: () -> Unit,
) {
    val colors = TixiTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TixiRadius.lg))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(TixiRadius.md)).background(colors.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.size(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = TixiFontFamilies.Serif, fontSize = 16.sp, color = colors.textPrimary)
            Spacer(Modifier.height(5.dp))
            Text(desc, fontSize = 11.sp, lineHeight = 16.sp, color = colors.textSecondary)
        }
    }
}
