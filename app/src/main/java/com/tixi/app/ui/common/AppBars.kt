package com.tixi.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tixi.app.ui.theme.TixiFontFamilies
import com.tixi.app.ui.theme.TixiSize
import com.tixi.app.ui.theme.TixiTheme

/**
 * 顶部栏：纸面底色、无阴影。large 仅用于工作台首页（28sp 宋体标题）。
 */
@Composable
fun TixiTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    trailingText: String? = null,
    large: Boolean = false,
    titleSerif: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = TixiTheme.colors

    if (large) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .height(TixiSize.topBarLarge)
                .background(colors.canvas),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TixiSize.topBar)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    TixiIconButton(
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "返回",
                        onClick = onBack,
                        tint = colors.textPrimary,
                    )
                }
                Spacer(Modifier.weight(1f))
                actions()
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                contentAlignment = Alignment.BottomStart,
            ) {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = if (titleSerif) TixiFontFamilies.Serif else TixiFontFamilies.Sans,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(TixiSize.topBar)
            .background(colors.canvas)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            TixiIconButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "返回",
                onClick = onBack,
                tint = colors.textPrimary,
            )
        }
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(start = if (onBack != null) 4.dp else 8.dp),
            color = colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = if (titleSerif) TixiFontFamilies.Serif else TixiFontFamilies.Sans,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailingText != null) {
            Text(
                text = trailingText,
                modifier = Modifier.padding(horizontal = 8.dp),
                color = colors.textSecondary,
                fontSize = 13.sp,
                fontFamily = TixiFontFamilies.Mono,
            )
        }
        actions()
    }
}

/**
 * 底部导航：64dp + 导航栏安全区，「刷题 / 设置」两等宽 Tab。
 */
@Composable
fun TixiBottomNav(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TixiTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.canvas)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(
                    color = colors.outline,
                    start = Offset(0f, stroke / 2f),
                    end = Offset(size.width, stroke / 2f),
                    strokeWidth = stroke,
                )
            }
            .height(TixiSize.bottomNav),
    ) {
        NavTab(
            label = "刷题",
            icon = Icons.Outlined.EditNote,
            selected = selectedIndex == 0,
            onClick = { onSelect(0) },
            modifier = Modifier.weight(1f),
        )
        NavTab(
            label = "设置",
            icon = Icons.Outlined.Settings,
            selected = selectedIndex == 1,
            onClick = { onSelect(1) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun NavTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TixiTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val tint = if (selected) colors.primary else colors.textTertiary

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(21.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                color = tint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = TixiFontFamilies.Sans,
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp)
                    .width(24.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(colors.primary),
            )
        }
    }
}
