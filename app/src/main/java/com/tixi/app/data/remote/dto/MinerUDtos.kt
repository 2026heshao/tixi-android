package com.tixi.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * MinerU v4 接口 DTO。
 * 约定：所有字段都给默认值 —— 服务端增删字段时解析不崩，由调用方按 code 判定成败。
 */

/** POST /api/v4/file-urls/batch 请求体 */
@Serializable
data class FileUrlRequest(
    val files: List<FileItem> = emptyList(),
    @SerialName("model_version") val modelVersion: String = "pipeline",
    val language: String = "ch",
    @SerialName("enable_table") val enableTable: Boolean = true,
    @SerialName("enable_formula") val enableFormula: Boolean = true,
)

@Serializable
data class FileItem(
    val name: String = "",
    @SerialName("data_id") val dataId: String = "",
)

/** code 可能是 0（数字）或 A0202（字符串），故用 JsonElement 兜底 */
@Serializable
data class FileUrlResponse(
    val code: JsonElement? = null,
    val msg: String = "",
    @SerialName("trace_id") val traceId: String = "",
    val data: FileUrlData? = null,
)

@Serializable
data class FileUrlData(
    @SerialName("batch_id") val batchId: String = "",
    @SerialName("file_urls") val fileUrls: List<String> = emptyList(),
)

/** GET /api/v4/extract-results/batch/{batch_id} */
@Serializable
data class BatchResultResponse(
    val code: JsonElement? = null,
    val msg: String = "",
    val data: BatchResultData? = null,
)

@Serializable
data class BatchResultData(
    @SerialName("batch_id") val batchId: String = "",
    @SerialName("extract_result") val extractResult: List<ExtractResult> = emptyList(),
)

@Serializable
data class ExtractResult(
    @SerialName("file_name") val fileName: String = "",
    @SerialName("state") val state: String = "",
    @SerialName("full_zip_url") val fullZipUrl: String? = null,
    @SerialName("err_msg") val errMsg: String? = null,
    @SerialName("data_id") val dataId: String = "",
    @SerialName("extract_progress") val extractProgress: ExtractProgress? = null,
)

@Serializable
data class ExtractProgress(
    @SerialName("extracted_pages") val extractedPages: Int = 0,
    @SerialName("total_pages") val totalPages: Int = 0,
)
