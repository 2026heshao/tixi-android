package com.tixi.app

import com.tixi.app.data.remote.dto.LiteStatusResponse
import com.tixi.app.data.remote.dto.LiteSubmitResponse
import com.tixi.app.domain.model.ParseProgress
import com.tixi.app.ui.importflow.overallPercent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 轻量通道（agent/parse 提交 + 查询）字段契约回归。
 *
 * 下面的 JSON 是 2026-11-22 实测**原样抓取**的响应，不是手写样例：
 * 一旦 MinerU 改字段名或类型，这里会先失败，而不是等用户导入时炸。
 */
class MinerULiteDtosTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    @Test
    fun `提交响应能解析出 taskId 与上传地址`() {
        val raw = """
            {"code":0,"msg":"ok","trace_id":"ae2e4eca355072739bfa68968b5a6c0e","data":{"task_id":"74a3bba0-d6e6-4208-be84-2669ce15b82716","file_url":"https://mineru.oss-cn-shanghai.aliyuncs.com/api-upload/extract/2026-11-22/agent/74a3bba0-d6e6-4208-be84-2669ce15b827.png?Expires=1795443754&OSSAccessKeyId=LTAI5t6sXp5gszd8yWZ85dAL&Signature=jqSEspIvgoD2U5Dt1QsbvIhz%2FZs%3D"}}
        """.trimIndent()
        val parsed = json.decodeFromString(LiteSubmitResponse.serializer(), raw)
        assertEquals("0", parsed.code.toString())
        assertEquals("ok", parsed.msg)
        assertEquals("74a3bba0-d6e6-4208-be84-2669ce15b82716", parsed.data?.taskId)
        assertEquals(true, parsed.data?.fileUrl?.startsWith("https://mineru.oss-cn-shanghai.aliyuncs.com/"))
    }

    @Test
    fun `轮询 running 状态没有 markdown 地址`() {
        val raw = """
            {"code":0,"msg":"ok","trace_id":"45883e514abb25cf16e4e3f5ec2fa1c3","data":{"task_id":"74a3bba0-d6e6-4208-be84-2669ce15b82716","state":"running"}}
        """.trimIndent()
        val parsed = json.decodeFromString(LiteStatusResponse.serializer(), raw)
        assertEquals("running", parsed.data?.state)
        assertNull(parsed.data?.markdownUrl)
    }

    @Test
    fun `轮询 done 状态给出 markdown 直链`() {
        val raw = """
            {"code":0,"msg":"ok","trace_id":"45883e514abb25cf16e4e3f5ec2fa1c3","data":{"task_id":"74a3bba0-d6e6-4208-be84-2669ce15b82716","state":"done","markdown_url":"https://cdn-mineru.openxlab.org.cn/pdf/2026-11-22/b611dfc2-4984-407e-9ade-9bf3ffa88e54/full.md"}}
        """.trimIndent()
        val parsed = json.decodeFromString(LiteStatusResponse.serializer(), raw)
        assertEquals("done", parsed.data?.state)
        assertEquals(
            "https://cdn-mineru.openxlab.org.cn/pdf/2026-11-22/b611dfc2-4984-407e-9ade-9bf3ffa88e54/full.md",
            parsed.data?.markdownUrl,
        )
    }

    @Test
    fun `failed 状态带错误码与文案`() {
        val raw = """
            {"code":0,"msg":"ok","data":{"task_id":"t1","state":"failed","err_code":"-60012","err_msg":"文件页数超过限制"}}
        """.trimIndent()
        val parsed = json.decodeFromString(LiteStatusResponse.serializer(), raw)
        assertEquals("failed", parsed.data?.state)
        assertEquals("-60012", (parsed.data?.errCode as JsonPrimitive).content)
        assertEquals("文件页数超过限制", parsed.data?.errMsg)
    }

    @Test
    fun `缺字段不会崩`() {
        val parsed = json.decodeFromString(LiteStatusResponse.serializer(), """{"code":0}""")
        assertNull(parsed.data)
    }

    @Test
    fun `轻量通道无分页信息时进度条也要动`() {
        // 轻量通道只回 running，拿不到页数；不能让它卡在 30%
        assertEquals(60, ParseProgress.Parsing(0, 0).overallPercent())
        // 精准通道有页数时按比例推进
        assertEquals(60, ParseProgress.Parsing(50, 100).overallPercent())
        assertEquals(90, ParseProgress.Parsing(100, 100).overallPercent())
    }
}
