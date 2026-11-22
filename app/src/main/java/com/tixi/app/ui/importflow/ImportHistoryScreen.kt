package com.tixi.app.ui.importflow

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tixi.app.domain.model.ImportRecord
import com.tixi.app.domain.model.ImportStatus
import com.tixi.app.ui.common.EmptyState
import com.tixi.app.ui.common.TixiIconButton
import com.tixi.app.ui.common.TixiTopBar
import com.tixi.app.ui.theme.TixiFontFamilies
import com.tixi.app.ui.theme.TixiSpacing
import com.tixi.app.ui.theme.TixiTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ImportHistoryScreen(
    onBack: () -> Unit,
    viewModel: ImportHistoryViewModel = hiltViewModel(),
) {
    val records by viewModel.records.collectAsStateWithLifecycle()
    val colors = TixiTheme.colors

    Column(Modifier.fillMaxSize().background(colors.canvas).navigationBarsPadding()) {
        TixiTopBar(title = "导入记录", onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TixiSpacing.pageHorizontal),
        ) {
            if (records.isEmpty()) {
                Spacer(Modifier.height(TixiSpacing.s8))
                EmptyState(
                    icon = Icons.Outlined.History,
                    message = "还没有导入记录",
                    hint = "每次导入都会记录来源、数量与成败原因。",
                )
            } else {
                records.forEach { record ->
                    RecordRow(record = record, onDelete = { viewModel.delete(record) })
                    HorizontalDivider(thickness = 1.dp, color = colors.outline)
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "导入记录仅保留来源、数量与成败原因；去重与导出功能后置。",
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    color = colors.textTertiary,
                )
            }
            Spacer(Modifier.height(TixiSpacing.s12))
        }
    }
}

@Composable
private fun RecordRow(record: ImportRecord, onDelete: () -> Unit) {
    val colors = TixiTheme.colors
    val statusColor = when (record.status) {
        ImportStatus.SUCCESS -> colors.success
        ImportStatus.PARTIAL -> colors.warning
        ImportStatus.FAILED -> colors.error
    }
    val time = remember(record.createdAt) {
        SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(record.createdAt))
    }

    Row(
        Modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                record.sourceFileName,
                fontSize = 13.sp,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$time · ${record.importedCount} 题 · ",
                    fontFamily = TixiFontFamilies.Mono,
                    fontSize = 11.sp,
                    color = colors.textTertiary,
                )
                Text(record.status.label, fontSize = 11.sp, color = statusColor)
            }
            record.errorMessage?.let {
                Spacer(Modifier.height(4.dp))
                Text("失败原因：$it", fontSize = 11.sp, lineHeight = 16.sp, color = colors.textSecondary)
            }
        }
        Spacer(Modifier.size(8.dp))
        TixiIconButton(Icons.Outlined.DeleteOutline, "删除记录", onDelete, buttonSize = 36.dp)
    }
}
