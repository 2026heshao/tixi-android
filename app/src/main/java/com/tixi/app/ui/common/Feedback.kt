package com.tixi.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tixi.app.ui.theme.TixiFontFamilies
import com.tixi.app.ui.theme.TixiRadius
import com.tixi.app.ui.theme.TixiTheme

/** 警告横幅：左侧 3dp 竖条 + warningContainer 底，无图标。 */
@Composable
fun WarningBanner(
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = TixiTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.warningContainer),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(colors.warning),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = TixiFontFamilies.Sans,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = detail,
                color = colors.textSecondary,
                fontSize = 11.sp,
                lineHeight = 17.sp,
                fontFamily = TixiFontFamilies.Sans,
            )
        }
        if (actionLabel != null && onAction != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .minimumInteractiveComponentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAction,
                    )
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = actionLabel,
                    color = colors.warning,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = TixiFontFamilies.Sans,
                    textDecoration = TextDecoration.Underline,
                )
            }
        }
    }
}

/** 空状态：图标 + 主句 + 提示 + 可选动作。 */
@Composable
fun EmptyState(
    icon: ImageVector,
    message: String,
    hint: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = TixiTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            color = colors.textSecondary,
            fontSize = 15.sp,
            lineHeight = 23.sp,
            fontFamily = TixiFontFamilies.Serif,
            textAlign = TextAlign.Center,
        )
        if (hint != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = hint,
                color = colors.textTertiary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontFamily = TixiFontFamilies.Sans,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAction,
                    )
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = actionLabel,
                    color = colors.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = TixiFontFamilies.Sans,
                    textDecoration = TextDecoration.Underline,
                )
            }
        }
    }
}

/** 对话框：320dp 宽、12dp 圆角；遮罩与返回键均不关闭（必须显式选择）。 */
@Composable
fun TixiDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "取消",
    danger: Boolean = false,
) {
    val colors = TixiTheme.colors
    val shape = RoundedCornerShape(TixiRadius.md)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.outline, shape)
                .padding(20.dp),
        ) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = TixiFontFamilies.Serif,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                color = colors.textSecondary,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                fontFamily = TixiFontFamilies.Sans,
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss,
                        )
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = dismissLabel,
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontFamily = TixiFontFamilies.Sans,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onConfirm,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (danger) colors.error else colors.primary)
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = confirmLabel,
                            color = colors.onPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = TixiFontFamilies.Sans,
                        )
                    }
                }
            }
        }
    }
}

/** 提示条宿主：纸面风（深底浅字），与阴影无关。 */
@Composable
fun TixiSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val colors = TixiTheme.colors
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.textPrimary)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = data.visuals.message,
                color = colors.canvas,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontFamily = TixiFontFamilies.Sans,
            )
        }
    }
}

/** 底部操作区：surface 底 + 顶部 1dp 分隔 + 导航栏安全区。 */
@Composable
fun BottomActionBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = TixiTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(
                    color = colors.outline,
                    start = Offset(0f, stroke / 2f),
                    end = Offset(size.width, stroke / 2f),
                    strokeWidth = stroke,
                )
            }
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 12.dp, top = 12.dp, end = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
