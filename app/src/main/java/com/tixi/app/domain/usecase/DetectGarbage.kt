package com.tixi.app.domain.usecase

import com.tixi.app.domain.model.AnswerData
import com.tixi.app.domain.model.BuiltInQuestionType
import com.tixi.app.domain.model.QualityFlag
import com.tixi.app.domain.model.RichBlock

/**
 * 识别质量闸门：把「疑似不可用」的草稿题标记出来，UI 据此强制人工确认。
 * 纯 Kotlin，可 JVM 单测。
 */
object DetectGarbage {

    private const val MIN_STEM_LENGTH = 8
    private const val GARBLED_RATIO = 0.02

    fun evaluate(
        stem: String,
        options: List<String>,
        answer: AnswerData,
        richBlocks: List<RichBlock>,
        typeKey: String,
    ): List<QualityFlag> {
        val flags = mutableListOf<QualityFlag>()

        if (garbledRatio(stem) > GARBLED_RATIO) flags += QualityFlag.SUSPECT_GARBLED
        if (stem.trim().length < MIN_STEM_LENGTH) flags += QualityFlag.TOO_SHORT

        val isChoice = typeKey == BuiltInQuestionType.CHOICE.key
        if (isChoice && (options.size < 2 || options.any { it.isBlank() })) {
            flags += QualityFlag.BAD_OPTIONS
        }

        if (answer.isEmpty()) flags += QualityFlag.NO_ANSWER
        if (richBlocks.isNotEmpty()) flags += QualityFlag.HAS_RICH_CONTENT

        return flags
    }

    fun requiresManualReview(flags: List<QualityFlag>): Boolean = flags.any { it.requiresManualReview }

    /** 不可读字符（替换符 / 空心方框 / 私用区）占非空白字符的比例 */
    private fun garbledRatio(text: String): Double {
        if (text.isEmpty()) return 0.0
        var total = 0
        var unreadable = 0
        for (ch in text) {
            if (ch.isWhitespace()) continue
            total++
            if (isUnreadable(ch)) unreadable++
        }
        return if (total == 0) 0.0 else unreadable.toDouble() / total
    }

    private fun isUnreadable(ch: Char): Boolean =
        ch == '\uFFFD' || ch == '\u25A1' || ch in '\uE000'..'\uF8FF'
}
