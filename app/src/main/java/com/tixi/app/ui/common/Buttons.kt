package com.tixi.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tixi.app.ui.theme.TixiFontFamilies
import com.tixi.app.ui.theme.TixiMotion
import com.tixi.app.ui.theme.TixiTheme
import com.tixi.app.ui.theme.rememberReducedMotion

enum class TixiButtonVariant { Filled, Tonal, Outlined, Text }
enum class TixiButtonSize { Large, Medium, Small }

private fun TixiButtonSize.height(): Dp = when (this) {
    TixiButtonSize.Large -> 48.dp
    TixiButtonSize.Medium -> 40.dp
    TixiButtonSize.Small -> 32.dp
}

private fun TixiButtonSize.horizontalPadding(): Dp = when (this) {
    TixiButtonSize.Large -> 22.dp
    TixiButtonSize.Medium -> 16.dp
    TixiButtonSize.Small -> 12.dp
}

private fun TixiButtonSize.fontSize() = when (this) {
    TixiButtonSize.Small -> 11.sp
    else -> 13.sp
}

/**
 * 纸本按钮：无阴影、无渐变；层次由底色与 1dp 描边表达。
 * 视觉高度 48/40/32，热区始终 ≥48dp；loading 时保留原宽度，避免布局跳动。
 */
@Composable
fun TixiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: TixiButtonVariant = TixiButtonVariant.Filled,
    size: TixiButtonSize = TixiButtonSize.Medium,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    val colors = TixiTheme.colors
    val reducedMotion = rememberReducedMotion()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !reducedMotion) 0.98f else 1f,
        animationSpec = tween(durationMillis = TixiMotion.Instant),
        label = "TixiButtonPressScale",
    )

    val container = when (variant) {
        TixiButtonVariant.Filled -> colors.primary
        TixiButtonVariant.Tonal -> colors.surfaceVariant
        TixiButtonVariant.Outlined, TixiButtonVariant.Text -> Color.Transparent
    }
    val contentColor = if (variant == TixiButtonVariant.Filled) colors.onPrimary else colors.primary
    val shape = RoundedCornerShape(8.dp)

    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .alpha(if (enabled) 1f else 0.38f)
            .clickable(
                enabled = enabled && !loading,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(size.height())
            .clip(shape)
            .background(container)
            .then(
                if (variant == TixiButtonVariant.Outlined) {
                    Modifier.border(1.dp, colors.outlineStrong, shape)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = size.horizontalPadding()),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Row(
                modifier = Modifier.alpha(if (loading) 0f else 1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(if (size == TixiButtonSize.Small) 14.dp else 16.dp),
                    )
                }
                Text(
                    text = text,
                    color = contentColor,
                    fontSize = size.fontSize(),
                    fontWeight = FontWeight.Medium,
                    fontFamily = TixiFontFamilies.Sans,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (trailingIcon != null) {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(if (size == TixiButtonSize.Small) 14.dp else 16.dp),
                    )
                }
            }
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = contentColor,
                    strokeWidth = 1.5.dp,
                )
            }
        }
    }
}

/** 图标按钮：视觉 40dp，热区 ≥48dp，无涟漪，靠按下缩放给出反馈。 */
@Composable
fun TixiIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    buttonSize: Dp = 40.dp,
    enabled: Boolean = true,
) {
    val colors = TixiTheme.colors
    val reducedMotion = rememberReducedMotion()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !reducedMotion) 0.94f else 1f,
        animationSpec = tween(durationMillis = TixiMotion.Instant),
        label = "TixiIconButtonPressScale",
    )

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .alpha(if (enabled) 1f else 0.38f)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(buttonSize)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint ?: colors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
