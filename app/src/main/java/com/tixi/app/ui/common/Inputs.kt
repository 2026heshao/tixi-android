package com.tixi.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tixi.app.ui.theme.TixiFontFamilies
import com.tixi.app.ui.theme.TixiSize
import com.tixi.app.ui.theme.TixiTheme

/** 文本输入：56dp 高、8dp 圆角；聚焦切 primary 2dp 描边，出错切 error 描边。 */
@Composable
fun TixiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    label: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    isError: Boolean = false,
    supporting: String? = null,
    mono: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    TixiTextFieldImpl(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        label = label,
        singleLine = singleLine,
        minLines = minLines,
        enabled = enabled,
        isError = isError,
        supporting = supporting,
        mono = mono,
        keyboardOptions = keyboardOptions,
    )
}

@Composable
private fun TixiTextFieldImpl(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    placeholder: String?,
    label: String?,
    singleLine: Boolean,
    minLines: Int,
    enabled: Boolean,
    isError: Boolean,
    supporting: String?,
    mono: Boolean,
    keyboardOptions: KeyboardOptions,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    readOnly: Boolean = false,
    successTone: Boolean = false,
    containerColor: Color? = null,
    textStyleOverride: TextStyle? = null,
) {
    val colors = TixiTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val textStyle = textStyleOverride ?: TextStyle(
        fontFamily = if (mono) TixiFontFamilies.Mono else TixiFontFamilies.Sans,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = colors.textPrimary,
    )
    val background = containerColor ?: colors.surface

    FieldContainer(
        interactionSource = interactionSource,
        modifier = modifier,
        label = label,
        supporting = supporting,
        isError = isError,
        successTone = successTone,
        enabled = enabled,
        minHeight = TixiSize.field,
        background = background,
        verticalPadding = if (singleLine) 0.dp else 10.dp,
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty() && placeholder != null) {
                Text(
                    text = placeholder,
                    color = colors.textTertiary,
                    fontSize = textStyle.fontSize,
                    fontFamily = textStyle.fontFamily,
                    maxLines = if (singleLine) 1 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                readOnly = readOnly,
                textStyle = textStyle.copy(color = if (enabled) textStyle.color else textStyle.color.copy(alpha = 0.38f)),
                keyboardOptions = keyboardOptions,
                singleLine = singleLine,
                minLines = minLines,
                maxLines = if (singleLine) 1 else Int.MAX_VALUE,
                visualTransformation = visualTransformation,
                interactionSource = interactionSource,
                cursorBrush = SolidColor(colors.primary),
            )
        }
    }
}

@Composable
private fun FieldContainer(
    interactionSource: InteractionSource,
    modifier: Modifier,
    label: String?,
    supporting: String?,
    isError: Boolean,
    successTone: Boolean,
    enabled: Boolean,
    minHeight: Dp,
    background: Color,
    verticalPadding: Dp,
    content: @Composable () -> Unit,
) {
    val colors = TixiTheme.colors
    val focused by interactionSource.collectIsFocusedAsState()
    val borderColor = when {
        isError -> colors.error
        successTone -> colors.success
        focused -> colors.primary
        else -> colors.outline
    }
    val borderWidth = if (focused && !isError && !successTone) 2.dp else 1.dp
    val shape = RoundedCornerShape(8.dp)

    Column(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.55f),
    ) {
        if (label != null) {
            Text(
                text = label,
                modifier = Modifier.padding(bottom = 6.dp),
                color = colors.textSecondary,
                fontSize = 12.sp,
                fontFamily = TixiFontFamilies.Sans,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .clip(shape)
                .background(background)
                .border(borderWidth, borderColor, shape)
                .padding(horizontal = 12.dp, vertical = verticalPadding),
            contentAlignment = Alignment.CenterStart,
        ) {
            content()
        }
        if (supporting != null) {
            Text(
                text = supporting,
                modifier = Modifier.padding(top = 6.dp, start = 2.dp),
                color = if (isError) colors.error else colors.textTertiary,
                fontSize = 12.sp,
                fontFamily = TixiFontFamilies.Sans,
            )
        }
    }
}

/**
 * 访问 Token 输入：默认掩码，可临时显隐；说明文字固定，不参与任何日志输出。
 */
@Composable
fun TokenField(
    token: String,
    onTokenChange: (String) -> Unit,
    isConfigured: Boolean,
    onSave: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TixiTheme.colors
    var revealed by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                TixiTextFieldImpl(
                    value = token,
                    onValueChange = onTokenChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "粘贴访问 Token",
                    label = null,
                    singleLine = true,
                    minLines = 1,
                    enabled = true,
                    isError = false,
                    supporting = null,
                    mono = true,
                    keyboardOptions = KeyboardOptions.Default,
                    visualTransformation = if (revealed) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                )
            }
            Spacer(Modifier.width(4.dp))
            TixiIconButton(
                icon = if (revealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription = if (revealed) "隐藏 Token" else "显示 Token",
                onClick = { revealed = !revealed },
                tint = colors.textSecondary,
            )
        }
        Text(
            text = "Keystore + AES-GCM 加密，仅存本机，不参与云备份",
            modifier = Modifier.padding(top = 6.dp, start = 2.dp),
            color = colors.textTertiary,
            fontSize = 12.sp,
            fontFamily = TixiFontFamilies.Sans,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TixiButton(
                text = "保存 Token",
                onClick = onSave,
                variant = TixiButtonVariant.Tonal,
                size = TixiButtonSize.Small,
            )
            Spacer(Modifier.width(4.dp))
            DangerTextAction(
                text = "删除 Token",
                enabled = isConfigured,
                onClick = onClear,
            )
            Spacer(Modifier.weight(1f))
            Badge(
                text = if (isConfigured) "已配置" else "未配置",
                tone = if (isConfigured) BadgeTone.Ok else BadgeTone.Warn,
            )
        }
    }
}

/** 填空作答输入：52dp 宋体 16sp；revealed 时只读，success 描边 + successContainer 底。 */
@Composable
fun FillInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    revealed: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = TixiTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(8.dp)
    val focused by interactionSource.collectIsFocusedAsState()

    val borderColor = when {
        revealed -> colors.success
        focused -> colors.primary
        else -> colors.outline
    }
    val borderWidth = if (focused && !revealed) 2.dp else 1.dp
    val textColor = if (revealed) colors.success else colors.textPrimary

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.55f)
            .fillMaxWidth()
            .heightIn(min = TixiSize.fillField)
            .clip(shape)
            .background(if (revealed) colors.successContainer else colors.surface)
            .border(borderWidth, borderColor, shape)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty() && placeholder != null) {
            Text(
                text = placeholder,
                color = colors.textTertiary,
                fontSize = 16.sp,
                fontFamily = TixiFontFamilies.Serif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            readOnly = revealed,
            textStyle = TextStyle(
                fontFamily = TixiFontFamilies.Serif,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = textColor,
            ),
            singleLine = true,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(colors.primary),
        )
    }
}

/** 小号文字动作（错误色），用于「删除 Token」这类破坏性但非主操作的入口。 */
@Composable
internal fun DangerTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = TixiTheme.colors
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) colors.error else colors.error.copy(alpha = 0.38f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = TixiFontFamilies.Sans,
        )
    }
}
