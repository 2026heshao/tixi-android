package com.tixi.app.ui.importflow

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PhotoCamera
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tixi.app.ui.common.InfoNote
import com.tixi.app.ui.common.StatusStrip
import com.tixi.app.ui.common.TixiButton
import com.tixi.app.ui.common.TixiButtonSize
import com.tixi.app.ui.common.TixiButtonVariant
import com.tixi.app.ui.common.TixiSnackbarHost
import com.tixi.app.ui.common.TixiTopBar
import com.tixi.app.ui.theme.TixiFontFamilies
import com.tixi.app.ui.theme.TixiRadius
import com.tixi.app.ui.theme.TixiSpacing
import com.tixi.app.ui.theme.TixiTheme

@Composable
fun ImportSourceScreen(
    onBack: () -> Unit,
    onOpenTask: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ImportSourceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = TixiTheme.colors
    val snackbarHostState = remember { SnackbarHostState() }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? -> if (uri != null) viewModel.pickDocument(uri) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? -> if (uri != null) viewModel.pickDocument(uri) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success -> viewModel.onCameraResult(success) }

    LaunchedEffect(state.started) {
        if (state.started) {
            viewModel.consumeStarted()
            onOpenTask()
        }
    }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    Box(Modifier.fillMaxSize().background(colors.canvas)) {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            TixiTopBar(title = "导入题目", onBack = onBack)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = TixiSpacing.pageHorizontal),
            ) {
                InfoNote("题目将经 MinerU 云端解析，需要联网。识别结果不会直接入库，会先进入确认页由你逐题核对。")
                Spacer(Modifier.height(10.dp))

                SourceCard(
                    icon = Icons.Outlined.PhotoCamera,
                    title = "拍照 / 相册",
                    desc = "选择题目照片，支持多张连续识别",
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TixiButton(
                        text = "直接拍照",
                        onClick = {
                            val uri = viewModel.createCameraTarget()
                            if (uri != null) cameraLauncher.launch(uri)
                        },
                        variant = TixiButtonVariant.Text,
                        size = TixiButtonSize.Small,
                        leadingIcon = Icons.Outlined.PhotoCamera,
                    )
                }
                Spacer(Modifier.height(14.dp))

                SourceCard(
                    icon = Icons.Outlined.Description,
                    title = "选择文件",
                    desc = "PDF · DOC · DOCX，统一走云端解析",
                    onClick = {
                        filePicker.launch(
                            arrayOf(
                                "application/pdf",
                                "application/msword",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "image/*",
                                "*/*",
                            ),
                        )
                    },
                )

                Spacer(Modifier.height(20.dp))
                StatusStrip(
                    text = if (state.configured) "MinerU Token 已配置" else "MinerU Token 未配置，无法发起解析",
                    ok = state.configured,
                    actionLabel = if (state.configured) "查看" else "去配置",
                    onAction = onOpenSettings,
                )
                Spacer(Modifier.height(16.dp))
                InfoNote("解析任务会保留原始文件。若识别失败或出现乱码，可在确认页重试或手工编辑。")
                if (state.preparing) {
                    Spacer(Modifier.height(8.dp))
                    Text("正在读取文件…", fontSize = 11.sp, color = colors.textSecondary)
                }
                Spacer(Modifier.height(TixiSpacing.s8))
            }
        }
        TixiSnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
    }
}

@Composable
private fun SourceCard(
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
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(TixiRadius.md))
                .background(colors.surfaceVariant),
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
