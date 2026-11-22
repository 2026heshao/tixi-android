package com.tixi.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tixi.app.ui.theme.TixiFontFamilies
import com.tixi.app.ui.theme.TixiTheme

enum class OptionVisual { Idle, Selected, Correct, Wrong }

/**
 * 选项行：56dp 起，8dp 圆角，左侧等宽关键字圆标。
 * Selected 用 outlineStrong（赭石）2dp 描边，Correct/Wrong 用语义色，不依赖阴影。
 */
@Composable
fun RadioOption(
    optionKey: String,
    text: String,
    visual: OptionVisual,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = TixiTheme.colors

    val container = when (visual) {
        OptionVisual.Idle -> colors.surface
        OptionVisual.Selected -> colors.surfaceVariant
        OptionVisual.Correct -> colors.successContainer
        OptionVisual.Wrong -> colors.errorContainer
    }
    val borderColor = when (visual) {
        OptionVisual.Idle -> colors.outline
        OptionVisual.Selected -> colors.outlineStrong
        OptionVisual.Correct -> colors.success
        OptionVisual.Wrong -> colors.error
    }
    val borderWidth = if (visual == OptionVisual.Selected) 2.dp else 1.dp
    val keyFill = when (visual) {
        OptionVisual.Idle -> Color.Transparent
        OptionVisual.Selected -> colors.primary
        OptionVisual.Correct -> colors.success
        OptionVisual.Wrong -> colors.error
    }
    val keyTextColor = when (visual) {
        OptionVisual.Idle -> colors.textSecondary
        else -> colors.onPrimary
    }
    val textColor = when (visual) {
        OptionVisual.Idle -> colors.textPrimary
        OptionVisual.Selected -> colors.textPrimary
        OptionVisual.Correct -> colors.success
        OptionVisual.Wrong -> colors.error
    }
    val shape = RoundedCornerShape(8.dp)

    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.55f)
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(container)
            .border(borderWidth, borderColor, shape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(keyFill)
                .then(
                    if (visual == OptionVisual.Idle) {
                        Modifier.border(1.dp, colors.outline, CircleShape)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = optionKey,
                color = keyTextColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = TixiFontFamilies.Mono,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = textColor,
            fontSize = 15.sp,
            lineHeight = 23.sp,
            fontFamily = TixiFontFamilies.Sans,
        )
    }
}

/** 自评行：无判分机制，仅记录「会了 / 再看一遍」。 */
@Composable
fun SelfAssessmentRow(
    mastered: Boolean?,
    onSelect: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TixiTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "这次掌握了吗？",
            color = colors.textSecondary,
            fontSize = 13.sp,
            fontFamily = TixiFontFamilies.Sans,
        )
        Spacer(Modifier.weight(1f))
        AssessmentChip(
            text = "会了",
            selected = mastered == true,
            onClick = { onSelect(true) },
        )
        Spacer(Modifier.width(8.dp))
        AssessmentChip(
            text = "再看一遍",
            selected = mastered == false,
            onClick = { onSelect(false) },
        )
    }
}

@Composable
private fun AssessmentChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = TixiTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
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
                .heightIn(min = 32.dp)
                .clip(shape)
                .background(if (selected) colors.primary else colors.surface)
                .border(1.dp, if (selected) colors.primary else colors.outline, shape)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = if (selected) colors.onPrimary else colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = TixiFontFamilies.Sans,
            )
        }
    }
}
