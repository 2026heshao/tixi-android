package com.tixi.app

import com.tixi.app.domain.model.BuiltInQuestionType
import com.tixi.app.domain.model.RichBlock
import com.tixi.app.domain.usecase.MinerUMarkdownParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MinerUMarkdownParserTest {

    @Test
    fun `标准选择题带答案解析`() {
        val markdown = """
            1. 根据《民法典》，下列关于合同成立与生效的说法，正确的是（ ）
            A. 合同成立即生效
            B. 合同成立后可能无效
            C. 合同生效须经登记
            D. 合同成立与生效完全相同
            答案：B
            解析：合同成立是事实判断，生效是价值判断。
            2. 下列关于代理的说法，正确的是（ ）
            A. 代理必须书面授权
            B. 代理包括委托代理与法定代理
            答案：B
        """.trimIndent()

        val questions = MinerUMarkdownParser.parse(markdown)

        assertEquals(2, questions.size)
        val first = questions[0]
        assertEquals(BuiltInQuestionType.CHOICE.key, first.typeKey)
        assertTrue(first.stem.contains("合同成立与生效"))
        assertTrue(!first.stem.contains("1."))
        assertEquals(4, first.options.size)
        assertEquals("合同成立即生效", first.options[0])
        assertEquals(listOf("B"), first.answer.correctKeys)
        assertNotNull(first.explanation)
        assertTrue(first.explanation!!.contains("事实判断"))

        val second = questions[1]
        assertEquals(2, second.options.size)
        assertEquals(listOf("B"), second.answer.correctKeys)
    }

    @Test
    fun `填空题按空位切分答案`() {
        val markdown = """
            3. 普通诉讼时效期间一般为____年，最长不超过____年。
            答案：3、20
        """.trimIndent()

        val questions = MinerUMarkdownParser.parse(markdown)

        assertEquals(1, questions.size)
        val question = questions.single()
        assertEquals(BuiltInQuestionType.FILL.key, question.typeKey)
        assertEquals(listOf("3", "20"), question.answer.correctKeys)
        assertTrue(question.stem.contains("____"))
    }

    @Test
    fun `表格公式图片进入富内容块`() {
        val markdown = """
            4. 根据下表与公式计算总价。
            | 项目 | 金额 |
            | --- | --- |
            | 甲 | 100 |
            | 乙 | 200 |

            $$
            E = mc^2
            $$

            ![示意图](images/fig1.jpg)

            答案：300
        """.trimIndent()

        val question = MinerUMarkdownParser.parse(markdown).single()

        assertEquals(3, question.richBlocks.size)

        val table = question.richBlocks.filterIsInstance<RichBlock.Table>().single()
        assertEquals(3, table.rows.size)
        assertEquals(listOf("项目", "金额"), table.rows[0])
        assertEquals(listOf("甲", "100"), table.rows[1])

        val formula = question.richBlocks.filterIsInstance<RichBlock.Formula>().single()
        assertEquals("E = mc^2", formula.latex)

        val image = question.richBlocks.filterIsInstance<RichBlock.ImageBlock>().single()
        assertEquals("images/fig1.jpg", image.path)
        assertEquals("示意图", image.alt)

        assertEquals("300", question.answer.text)
    }

    @Test
    fun `MinerU 真实输出：选项与题干同行且行内公式降级`() {
        // 取自 2026-11-22 对 sample.png 的真实解析结果（full.md），未做手工清洗
        val markdown = """
            1.已知函数 ${'$'}f ( \mathrm { x } ) { = } 2 \mathrm { x } + 3${'$'} ，则f(2)的值为（）A.5 B.6 C.7 D.8

            答案：C

            解析：代入x=2，f(2)=2×2+3=7，故选择 ${'$'}\mathrm { C } _ { \circ }${'$'}

            2.下列物理量中，属于矢量的是（）A.质量 B.时间C.速度 D.温度

            答案：C
        """.trimIndent()

        val questions = MinerUMarkdownParser.parse(markdown)

        assertEquals(2, questions.size)
        val first = questions[0]
        assertEquals(BuiltInQuestionType.CHOICE.key, first.typeKey)
        assertEquals(listOf("5", "6", "7", "8"), first.options)
        assertTrue(first.stem.contains("f(x)=2x+3"))
        assertTrue(!first.stem.contains("\\mathrm"))
        assertTrue(!first.stem.contains('$'))
        assertEquals(listOf("C"), first.answer.correctKeys)
        assertNotNull(first.explanation)
        assertTrue(!first.explanation!!.contains('$'))
        assertTrue(!first.explanation.contains("circ"))

        val second = questions[1]
        assertEquals(BuiltInQuestionType.CHOICE.key, second.typeKey)
        assertEquals(listOf("质量", "时间", "速度", "温度"), second.options)
        assertEquals(listOf("C"), second.answer.correctKeys)
    }

    @Test
    fun `空输入返回空列表`() {
        assertTrue(MinerUMarkdownParser.parse("").isEmpty())
        assertTrue(MinerUMarkdownParser.parse("   \n\n\t ").isEmpty())
    }
}
