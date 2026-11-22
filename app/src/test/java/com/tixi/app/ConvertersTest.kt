package com.tixi.app

import com.tixi.app.data.local.Converters
import com.tixi.app.domain.model.AnswerData
import com.tixi.app.domain.model.RichBlock
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 类型转换器往返与容错（纯 JVM 测试，不依赖 Android 运行时） */
class ConvertersTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    private val converters = Converters(json)

    @Test
    fun answerDataRoundTrip() {
        val answer = AnswerData(correctKeys = listOf("B"), text = "60 日")
        val encoded = converters.fromAnswerData(answer)
        assertEquals(answer, converters.toAnswerData(encoded))
    }

    @Test
    fun richBlocksRoundTrip() {
        val blocks = listOf(
            RichBlock.Table(rows = listOf(listOf("项目", "数值"), listOf("期限", "60 日")), caption = "表 1"),
            RichBlock.Formula(latex = "a^2 + b^2 = c^2"),
            RichBlock.ImageBlock(path = "/data/user/0/com.tixi.app/files/imports/1.png", alt = "原题截图"),
        )
        val encoded = converters.fromRichBlocks(blocks)
        assertEquals(blocks, converters.toRichBlocks(encoded))
    }

    @Test
    fun stringListRoundTrip() {
        val tags = listOf("合同法", "第 3 章")
        val encoded = converters.fromStringList(tags)
        assertEquals(tags, converters.toStringList(encoded))
    }

    @Test
    fun emptyValuesEncodeToNull() {
        assertNull(converters.fromStringList(emptyList()))
        assertNull(converters.fromRichBlocks(emptyList()))
        assertEquals(emptyList<String>(), converters.toStringList(null))
        assertEquals(emptyList<RichBlock>(), converters.toRichBlocks(""))
        assertEquals(AnswerData.Empty, converters.toAnswerData(null))
    }

    @Test
    fun unknownFieldsAreTolerated() {
        val raw = """{"correctKeys":["A"],"legacyScore":1,"nested":{"x":true}}"""
        assertEquals(AnswerData(correctKeys = listOf("A")), converters.toAnswerData(raw))
    }

    @Test
    fun malformedJsonFallsBackToEmpty() {
        assertEquals(AnswerData.Empty, converters.toAnswerData("{ not json"))
        assertTrue(converters.toStringList("[[[").isEmpty())
        assertTrue(converters.toRichBlocks("""{"type":"unknown"}""").isEmpty())
    }
}
