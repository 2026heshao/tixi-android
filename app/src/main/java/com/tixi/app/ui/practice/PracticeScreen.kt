package com.tixi.app.ui.practice

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
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
import com.tixi.app.domain.model.BuiltInQuestionType
import com.tixi.app.domain.model.Question
import com.tixi.app.ui.common.BottomActionBar
import com.tixi.app.ui.common.FillInput
import com.tixi.app.ui.common.InfoNote
import com.tixi.app.ui.common.OptionVisual
import com.tixi.app.ui.common.ProgressBar
import com.tixi.app.ui.common.RadioOption
import com.tixi.app.ui.common.RichBlocksView
import com.tixi.app.ui.common.SectionCard
import com.tixi.app.ui.common.SelfAssessmentRow
import com.tixi.app.ui.common.StatGrid
import com.tixi.app.ui.common.TixiButton
import com.tixi.app.ui.common.TixiButtonSize
import com.tixi.app.ui.common.TixiButtonVariant
import com.tixi.app.ui.common.TixiIconButton
import com.tixi.app.ui.common.TixiTopBar
import com.tixi.app.ui.theme.TixiFontFamilies
import com.tixi.app.ui.theme.TixiRadius
import com.tixi.app.ui.theme.TixiSpacing
import com.tixi.app.ui.theme.TixiTheme

@Composable
fun PracticeScreen(
    onBack: () -> Unit,
    onFinished: () -> Unit,
    viewModel: PracticeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = TixiTheme.colors

    if (state.complete) {
        SessionCompleteContent(
            categoryName = state.categoryName,
            stats = state.stats,
            onClose = onFinished,
            onRestart = viewModel::restart,
        )
        return
    }

    Column(Modifier.fillMaxSize().background(colors.canvas)) {
        TixiTopBar(
            title = state.categoryName,
            onBack = onBack,
            trailingText = if (state.total > 0) "${state.index + 1} / ${state.total}" else null,
        )
        ProgressBar(state.progressFraction, thin = true, modifier = Modifier.padding(horizontal = 16.dp))

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TixiSpacing.pageHorizontal),
        ) {
            val question = state.current
            if (question == null) {
                Spacer(Modifier.height(TixiSpacing.s12))
                Text(
                    state.message ?: "这个分类还没有题目",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            } else {
                Spacer(Modifier.height(10.dp))
                QuestionMeta(question = question)
                Spacer(Modifier.height(16.dp))
                Text(
                    question.stem,
                    fontFamily = TixiFontFamilies.Serif,
                    fontSize = if (question.builtInType == BuiltInQuestionType.ESSAY) 16.sp else 18.sp,
                    lineHeight = if (question.builtInType == BuiltInQuestionType.ESSAY) 28.sp else 30.sp,
                    color = colors.textPrimary,
                )

                if (question.hasRichContent) {
                    Spacer(Modifier.height(12.dp))
                    RichBlocksView(question.richBlocks, eyeCareActive = state.eyeCare)
                    if (colors.isDark && question.richBlocks.any { it is com.tixi.app.domain.model.RichBlock.ImageBlock }) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            TixiButton(
                                text = if (state.eyeCare) "查看原图" else "返回护眼",
                                onClick = viewModel::toggleEyeCare,
                                variant = TixiButtonVariant.Text,
                                size = TixiButtonSize.Small,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(TixiSpacing.s6))
                when (question.builtInType) {
                    BuiltInQuestionType.CHOICE -> ChoiceBody(
                        question = question,
                        revealed = state.revealed,
                        selectedKey = state.selectedKey,
                        onSelect = viewModel::selectOption,
                    )

                    BuiltInQuestionType.FILL -> FillBody(
                        question = question,
                        revealed = state.revealed,
                        text = state.fillText,
                        onTextChange = viewModel::updateFill,
                    )

                    else -> EssayBody(question = question, answerText = viewModel.answerTextFor(question))
                }

                if (state.revealed && question.isAnswerable) {
                    AnswerPanel(
                        answerText = viewModel.answerTextFor(question),
                        explanation = question.explanation,
                        mastered = state.selfMastered,
                        onAssess = viewModel::selfAssess,
                    )
                }
            }
            Spacer(Modifier.height(TixiSpacing.s8))
        }

        BottomActionBar {
            TixiButton(
                text = "上一题",
                onClick = viewModel::previous,
                modifier = Modifier.weight(1f),
                variant = TixiButtonVariant.Outlined,
                enabled = !state.isFirst,
            )
            val answerable = state.current?.isAnswerable == true
            when {
                answerable && !state.revealed -> TixiButton(
                    text = "提交答案",
                    onClick = viewModel::submit,
                    modifier = Modifier.weight(1.6f),
                    enabled = state.canSubmit,
                )

                state.primaryIsFinish -> TixiButton(
                    text = "查看本轮结果",
                    onClick = viewModel::finish,
                    modifier = Modifier.weight(1.6f),
                )

                else -> TixiButton(
                    text = "下一题",
                    onClick = viewModel::next,
                    modifier = Modifier.weight(1.6f),
                )
            }
        }
    }
}

@Composable
private fun QuestionMeta(question: Question) {
    val colors = TixiTheme.colors
    val typeLabel = when (question.builtInType) {
        BuiltInQuestionType.CHOICE -> if (question.hasRichContent) "选择题 · 含图表" else "选择题"
        BuiltInQuestionType.FILL -> "填空题"
        BuiltInQuestionType.ESSAY -> "大题 · 材料分析"
        null -> "通用题目"
    }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            typeLabel,
            fontSize = 11.sp,
            color = colors.textSecondary,
            modifier = Modifier
                .clip(RoundedCornerShape(TixiRadius.full))
                .background(colors.surfaceVariant)
                .padding(horizontal = 9.dp, vertical = 3.dp),
        )
        question.sourceFile?.let {
            Text("来源：$it", fontSize = 11.sp, color = colors.textTertiary)
        }
    }
}

@Composable
private fun ChoiceBody(
    question: Question,
    revealed: Boolean,
    selectedKey: String?,
    onSelect: (String) -> Unit,
) {
    val keys = "ABCDEFGH"
    val correctKeys = question.answer.correctKeys.map { it.trim().uppercase() }.toSet()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        question.options.forEachIndexed { index, option ->
            val key = keys.getOrNull(index)?.toString() ?: (index + 1).toString()
            val visual = when {
                !revealed && selectedKey == key -> OptionVisual.Selected
                revealed && key in correctKeys -> OptionVisual.Correct
                revealed && selectedKey == key -> OptionVisual.Wrong
                else -> OptionVisual.Idle
            }
            RadioOption(
                optionKey = key,
                text = option,
                visual = visual,
                onClick = { onSelect(key) },
                enabled = !revealed,
            )
        }
    }
}

@Composable
private fun FillBody(
    question: Question,
    revealed: Boolean,
    text: String,
    onTextChange: (String) -> Unit,
) {
    val colors = TixiTheme.colors
    Column {
        Text(
            if (revealed) "你输入的答案" else "请输入答案（多空用逗号分隔）",
            fontSize = 12.sp,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        FillInput(
            value = text,
            onValueChange = onTextChange,
            placeholder = if (question.blankCount > 1) "例如：60, 30" else "填写答案",
            revealed = revealed,
            enabled = !revealed,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (revealed) "已显示标准答案，请对照后自评。" else "提交后显示标准答案与解析，由你自评掌握情况，不自动判分。",
            fontSize = 11.sp,
            lineHeight = 17.sp,
            color = colors.textTertiary,
        )
    }
}

@Composable
private fun EssayBody(question: Question, answerText: String) {
    val colors = TixiTheme.colors
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = colors.success,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.size(7.dp))
            Text("参考答案", fontSize = 12.sp, color = colors.success)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            answerText.ifBlank { "（未录入参考答案）" },
            fontFamily = TixiFontFamilies.Serif,
            fontSize = 14.sp,
            lineHeight = 28.sp,
            color = colors.textPrimary,
        )
        if (!question.explanation.isNullOrBlank()) {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(7.dp))
                Text("解析", fontSize = 12.sp, color = colors.textSecondary)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                question.explanation,
                fontSize = 13.sp,
                lineHeight = 22.sp,
                color = colors.textSecondary,
            )
        }
        Spacer(Modifier.height(TixiSpacing.s4))
        Text(
            "大题仅展示题干与答案，不提供选项、提交与自评。",
            fontSize = 11.sp,
            color = colors.textTertiary,
        )
    }
}

@Composable
private fun AnswerPanel(
    answerText: String,
    explanation: String?,
    mastered: Boolean?,
    onAssess: (Boolean) -> Unit,
) {
    val colors = TixiTheme.colors
    Column(Modifier.padding(top = 20.dp)) {
        HorizontalDivider(thickness = 1.dp, color = colors.outline)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = colors.success,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text("正确答案", fontSize = 13.sp, color = colors.success)
            Spacer(Modifier.size(8.dp))
            Text(
                answerText,
                fontFamily = TixiFontFamilies.Mono,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
            )
        }
        if (!explanation.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(explanation, fontSize = 13.sp, lineHeight = 23.sp, color = colors.textSecondary)
        }
        Spacer(Modifier.height(16.dp))
        SelfAssessmentRow(mastered = mastered, onSelect = onAssess)
    }
}

@Composable
private fun SessionCompleteContent(
    categoryName: String,
    stats: com.tixi.app.domain.model.SessionStats,
    onClose: () -> Unit,
    onRestart: () -> Unit,
) {
    val colors = TixiTheme.colors
    Column(Modifier.fillMaxSize().background(colors.canvas)) {
        TixiTopBar(
            title = categoryName.ifBlank { "本轮练习" },
            actions = { TixiIconButton(Icons.Outlined.Close, "关闭", onClose) },
        )
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
                    Modifier
                        .size(56.dp)
                        .border(2.dp, colors.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("完", fontFamily = TixiFontFamilies.Serif, fontSize = 26.sp, color = colors.primary)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "这一轮，刷完了",
                    fontFamily = TixiFontFamilies.Serif,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "共 ${stats.total} 题 · 用时 ${stats.elapsedLabel()}",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                )
            }

            SectionCard {
                StatGrid(
                    listOf(
                        stats.total.toString() to "本轮题数",
                        stats.elapsedLabel() to "用时",
                        stats.mastered.toString() to "自评掌握",
                    ),
                )
            }
            Spacer(Modifier.height(12.dp))
            SectionCard(title = "掌握情况") {
                ProgressBar(stats.masteryRate)
                Spacer(Modifier.height(8.dp))
                Text(
                    "${(stats.masteryRate * 100).toInt()}% · ${stats.weakCount} 题建议再看一遍",
                    fontFamily = TixiFontFamilies.Mono,
                    fontSize = 11.sp,
                    color = colors.textTertiary,
                )
            }
            if (stats.weakStems.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                SectionCard(title = "本轮提醒") {
                    InfoNote(
                        "自评「再看一遍」的题目：${stats.weakStems.joinToString("；")}。建议回到分类选择重刷这一组。",
                    )
                }
            }
            Spacer(Modifier.height(TixiSpacing.s8))
        }
        BottomActionBar {
            TixiButton("返回工作台", onClose, Modifier.weight(1f), variant = TixiButtonVariant.Outlined)
            TixiButton("再来一轮", onRestart, Modifier.weight(1f))
        }
    }
}
