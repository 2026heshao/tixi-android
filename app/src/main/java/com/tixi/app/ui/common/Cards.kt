package com.tixi.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tixi.app.ui.theme.TixiFontFamilies
import com.tixi.app.ui.theme.TixiRadius
import com.tixi.app.ui.theme.TixiSize
import com.tixi.app.ui.theme.TixiTheme

enum class BadgeTone { Neutral, Ok, Warn, Primary }
enum class NoteTone { Neutral, Warning, Error, Success }

/** 徽标：胶囊底 + 11sp 文字，无描边，靠容器色区分语义。 */
@Composable
fun Badge(
    text: String,
    tone: BadgeTone = BadgeTone.Neutral,
    modifier: Modifier = Modifier,
) {
    val colors = TixiTheme.colors
    val container = when (tone) {
        BadgeTone.Neutral -> colors.surfaceVariant
        BadgeTone.Ok -> colors.successContainer
        BadgeTone.Warn -> colors.warningContainer
        BadgeTone.Primary -> colors.primaryContainer
    }
    val content = when (tone) {
        BadgeTone.Neutral -> colors.textSecondary
        BadgeTone.Ok -> colors.success
        BadgeTone.Warn -> colors.warning
        BadgeTone.Primary -> colors.primary
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(TixiRadius.full))
            .background(container)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = content,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = TixiFontFamilies.Sans,
            maxLines = 1,
        )
    }
}

/** 进度条：4dp（thin 3dp）圆角，轨道 surfaceVariant，进度 primary。 */
@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    thin: Boolean = false,
) {
    val colors = TixiTheme.colors
    val barHeight = if (thin) 3.dp else TixiSize.progress
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .clip(RoundedCornerShape(2.dp))
            .background(colors.surfaceVariant),
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .fillMaxHeight()
                    .background(colors.primary),
            )
        }
    }
}

/** 设置页卡片：surface + 1dp outline + 12dp 圆角。 */
@Composable
fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = TixiTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TixiRadius.md))
            .background(colors.surface)
            .border(1.dp, colors.outline, RoundedCornerShape(TixiRadius.md))
            .padding(contentPadding),
    ) {
        if (title != null) {
            Text(
                text = title,
                modifier = Modifier.padding(bottom = 6.dp),
                color = colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = TixiFontFamilies.Sans,
            )
        }
        content()
    }
}

/** 分类卡：96dp、16dp 圆角；当前分类用 primary 2dp 描边而非阴影。 */
@Composable
fun CategoryCard(
    name: String,
    count: Int,
    meta: String?,
    isCurrent: Boolean = false,
    onClick: () -> Unit,
    onContinue: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = TixiTheme.colors
    val shape = RoundedCornerShape(TixiRadius.lg)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(TixiSize.categoryCard)
            .clip(shape)
            .background(colors.surface)
            .border(if (isCurrent) 2.dp else 1.dp, if (isCurrent) colors.primary else colors.outline, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = TixiFontFamilies.Serif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isCurrent) {
                    Spacer(Modifier.width(8.dp))
                    Badge(text = "当前", tone = BadgeTone.Primary)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$count 题",
                color = colors.textSecondary,
                fontSize = 12.sp,
                fontFamily = TixiFontFamilies.Mono,
            )
            if (meta != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = meta,
                    color = colors.textTertiary,
                    fontSize = 11.sp,
                    fontFamily = TixiFontFamilies.Sans,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onContinue != null) {
            Spacer(Modifier.width(10.dp))
            TixiButton(
                text = "继续练习",
                onClick = onContinue,
                variant = TixiButtonVariant.Tonal,
                size = TixiButtonSize.Small,
            )
        }
    }
}

/** 导入入口卡：88dp、12dp 圆角、surfaceVariant 底。 */
@Composable
fun ImportCard(
    hasToken: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TixiTheme.colors
    val shape = RoundedCornerShape(TixiRadius.md)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(TixiSize.importCard)
            .clip(shape)
            .background(colors.surfaceVariant)
            .border(1.dp, colors.outline, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.UploadFile,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "导入题目",
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = TixiFontFamilies.Sans,
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (hasToken) "支持图片 · PDF · DOC · DOCX" else "轻量通道 · 单文件 10MB 以内",
                color = colors.textTertiary,
                fontSize = 11.sp,
                fontFamily = TixiFontFamilies.Sans,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** 页内说明条：语义浅底 + 同义色文字，无图标、无阴影。 */
@Composable
fun InfoNote(
    text: String,
    modifier: Modifier = Modifier,
    tone: NoteTone = NoteTone.Neutral,
) {
    val colors = TixiTheme.colors
    val container = when (tone) {
        NoteTone.Neutral -> colors.surfaceVariant
        NoteTone.Warning -> colors.warningContainer
        NoteTone.Error -> colors.errorContainer
        NoteTone.Success -> colors.successContainer
    }
    val content = when (tone) {
        NoteTone.Neutral -> colors.textSecondary
        NoteTone.Warning -> colors.warning
        NoteTone.Error -> colors.error
        NoteTone.Success -> colors.success
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            color = content,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontFamily = TixiFontFamilies.Sans,
        )
    }
}

/** 状态条：小圆点 + 说明 + 可选行内动作，用于网络/识别状态。 */
@Composable
fun StatusStrip(
    text: String,
    ok: Boolean,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = TixiTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, colors.outline, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (ok) colors.success else colors.warning),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = colors.textSecondary,
            fontSize = 12.sp,
            fontFamily = TixiFontFamilies.Sans,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAction,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = actionLabel,
                    color = colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = TixiFontFamilies.Sans,
                )
            }
        }
    }
}

/** 三栏数据：数字等宽 primary，标签 11sp。 */
@Composable
fun StatGrid(
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    val colors = TixiTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { (value, label) ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = value,
                    color = colors.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = TixiFontFamilies.Mono,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = label,
                    color = colors.textTertiary,
                    fontSize = 11.sp,
                    fontFamily = TixiFontFamilies.Sans,
                    maxLines = 1,
                )
            }
        }
    }
}

/** 筛选小标签：默认描边，选中填充 primary（accent 时填充赭石 secondary）。 */
@Composable
fun TixiChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    val colors = TixiTheme.colors
    val shape = RoundedCornerShape(TixiRadius.full)
    val fill = when {
        !selected -> Color.Transparent
        accent -> colors.secondary
        else -> colors.primary
    }
    val outline = when {
        selected -> Color.Transparent
        else -> colors.outline
    }
    val content = when {
        !selected -> colors.textSecondary
        accent -> colors.onSecondary
        else -> colors.onPrimary
    }
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .height(32.dp)
                .clip(shape)
                .background(if (selected) fill else colors.surface)
                .border(1.dp, outline, shape)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = content,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = TixiFontFamilies.Sans,
                maxLines = 1,
            )
        }
    }
}

/** 分段控件：surfaceVariant 轨道，选中片为 surface 底片。 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TixiTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceVariant)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) colors.surface else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(index) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (selected) colors.primary else colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    fontFamily = TixiFontFamilies.Sans,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 设置开关行。 */
@Composable
fun SwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TixiTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = TixiFontFamilies.Sans,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = colors.textTertiary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontFamily = TixiFontFamilies.Sans,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onPrimary,
                checkedTrackColor = colors.primary,
                checkedBorderColor = colors.primary,
                checkedIconColor = colors.primary,
                uncheckedThumbColor = colors.surface,
                uncheckedTrackColor = colors.surfaceVariant,
                uncheckedBorderColor = colors.outline,
                uncheckedIconColor = Color.Transparent,
            ),
        )
    }
}

/** 设置项行：56dp 高，底部 1dp 分隔线由本组件绘制（调用方控制最后一行是否隐藏）。 */
@Composable
fun SettingRow(
    title: String,
    subtitle: String? = null,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = TixiTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = TixiSize.settingRow)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(
                    color = colors.outline,
                    start = Offset(0f, size.height - stroke / 2f),
                    end = Offset(size.width, size.height - stroke / 2f),
                    strokeWidth = stroke,
                )
            }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontFamily = TixiFontFamilies.Sans,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = colors.textTertiary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontFamily = TixiFontFamilies.Sans,
                )
            }
        }
        if (value != null) {
            Spacer(Modifier.width(10.dp))
            Text(
                text = value,
                color = colors.textSecondary,
                fontSize = 13.sp,
                fontFamily = TixiFontFamilies.Mono,
                maxLines = 1,
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}
