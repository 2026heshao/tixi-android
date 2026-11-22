package com.tixi.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * MinerU「Agent 轻量解析」接口 DTO（v1，免 Token）。
 *
 * 真实响应（2026-11-22 实测）：
 * ```json
 * POST /api/v1/agent/parse/file  {"file_name":"sample.png","language":"ch"}
 * → {"code":0,"msg":"ok","trace_id":"...","data":{"task_id":"...","file_url":"https://mineru.oss-...aliyuncs.com/..."}}
 *
 * GET /api/v1/agent/parse/{task_id}
 * → {"code":0,"msg":"ok","trace_id":"...","data":{"task_id":"...","state":"done","markdown_url":"https://cdn-mineru.openxlab.org.cn/.../full.md"}}
 * ```
 *
 * 与精准通道（v4）的关键差异：**结果不是 zip，而是 markdown 直链**；
 * 状态只有 running / done / failed；无分页进度；限制 10MB / 20 页。
 */
@Serializable
data class LiteSubmitRequest(
    @SerialName("file_name") val fileName: String,
    val language: String = "ch",
)

@Serializable
data class LiteSubmitResponse(
    /** code 可能是数字 0 也可能是字符串错误码，统一用 JsonElement 兜住 */
    val code: JsonElement? = null,
    val msg: String = "",
    @SerialName("trace_id") val traceId: String? = null,
    val data: LiteSubmitData? = null,
)

@Serializable
data class LiteSubmitData(
    @SerialName("task_id") val taskId: String = "",
    @SerialName("file_url") val fileUrl: String = "",
)

@Serializable
data class LiteStatusResponse(
    val code: JsonElement? = null,
    val msg: String = "",
    val data: LiteStatusData? = null,
)

@Serializable
data class LiteStatusData(
    @SerialName("task_id") val taskId: String = "",
    /** waiting-file / pending / running / converting / done / failed */
    val state: String = "",
    @SerialName("markdown_url") val markdownUrl: String? = null,
    @SerialName("err_code") val errCode: JsonElement? = null,
    @SerialName("err_msg") val errMsg: String? = null,
)
