package com.tixi.app

import com.tixi.app.domain.model.AnswerData
import com.tixi.app.domain.model.BuiltInQuestionType
import com.tixi.app.domain.model.QualityFlag
import com.tixi.app.domain.model.RichBlock
import com.tixi.app.domain.usecase.DetectGarbage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectGarbageTest {

    private val normalStem = "关于合同成立与生效的下列表述，正确的是（ ）"
    private val normalOptions = listOf("合同成立即生效", "合同成立后可能无效")
    private val normalAnswer = AnswerData(correctKeys = listOf("B"))

    @Test
    fun `乱码字符占比过高标记乱码并要求人工确认`() {
        val stem = "合同效力\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD"
        val flags = DetectGarbage.evaluate(
            stem = stem,
            options = normalOptions,
            answer = normalAnswer,
            richBlocks = emptyList(),
            typeKey = BuiltInQuestionType.CHOICE.key,
        )
        assertTrue(flags.contains(QualityFlag.SUSPECT_GARBLED))
        assertTrue(DetectGarbage.requiresManualReview(flags))
    }

    @Test
    fun `题干过短标记内容过短`() {
        val flags = DetectGarbage.evaluate(
            stem = "合同效力",
            options = normalOptions,
            answer = normalAnswer,
            richBlocks = emptyList(),
            typeKey = BuiltInQuestionType.CHOICE.key,
        )
        assertTrue(flags.contains(QualityFlag.TOO_SHORT))
        assertFalse(DetectGarbage.requiresManualReview(flags))
    }

    @Test
    fun `选择题选项不足或为空标记选项异常`() {
        val flags = DetectGarbage.evaluate(
            stem = normalStem,
            options = listOf("唯一选项"),
            answer = normalAnswer,
            richBlocks = emptyList(),
            typeKey = BuiltInQuestionType.CHOICE.key,
        )
        assertTrue(flags.contains(QualityFlag.BAD_OPTIONS))

        val blankOptionFlags = DetectGarbage.evaluate(
            stem = normalStem,
            options = listOf("甲", "   "),
            answer = normalAnswer,
            richBlocks = emptyList(),
            typeKey = BuiltInQuestionType.CHOICE.key,
        )
        assertTrue(blankOptionFlags.contains(QualityFlag.BAD_OPTIONS))
    }

    @Test
    fun `缺少答案标记答案缺失`() {
        val flags = DetectGarbage.evaluate(
            stem = normalStem,
            options = normalOptions,
            answer = AnswerData.Empty,
            richBlocks = emptyList(),
            typeKey = BuiltInQuestionType.CHOICE.key,
        )
        assertTrue(flags.contains(QualityFlag.NO_ANSWER))
    }

    @Test
    fun `含富内容必须人工确认`() {
        val flags = DetectGarbage.evaluate(
            stem = normalStem,
            options = normalOptions,
            answer = normalAnswer,
            richBlocks = listOf(RichBlock.Table(rows = listOf(listOf("项目", "金额"), listOf("甲", "100")))),
            typeKey = BuiltInQuestionType.CHOICE.key,
        )
        assertTrue(flags.contains(QualityFlag.HAS_RICH_CONTENT))
        assertTrue(DetectGarbage.requiresManualReview(flags))
    }
}
