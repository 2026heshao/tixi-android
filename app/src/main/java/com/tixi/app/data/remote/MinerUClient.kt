package com.tixi.app.data.remote

import android.content.Context
import com.tixi.app.data.remote.dto.BatchResultData
import com.tixi.app.data.remote.dto.BatchResultResponse
import com.tixi.app.data.remote.dto.ExtractResult
import com.tixi.app.data.remote.dto.FileItem
import com.tixi.app.data.remote.dto.FileUrlRequest
import com.tixi.app.data.remote.dto.FileUrlResponse
import com.tixi.app.data.remote.dto.LiteStatusData
import com.tixi.app.data.remote.dto.LiteStatusResponse
import com.tixi.app.data.remote.dto.LiteSubmitRequest
import com.tixi.app.data.remote.dto.LiteSubmitResponse
import com.tixi.app.domain.model.ImportError
import com.tixi.app.domain.model.ParseOutcome
import com.tixi.app.domain.model.ParseParams
import com.tixi.app.domain.model.ParseProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSink

/**
 * MinerU 云端解析，两条通道共用同一套 [ParseOutcome] 出口：
 *
 * | 通道 | 触发条件 | 接口 | 限制 | 结果 |
 * |---|---|---|---|---|
 * | 精准（v4） | 已配置 Token | 申请上传地址 → PUT → 轮询批量结果 → 下载 zip 取 full.md | 200MB / 200 页 | zip + markdown |
 * | 轻量（v1 agent） | 未配置 Token | 提交任务 → PUT → 轮询单任务 → 下载 markdown | 10MB / 20 页 | markdown 直链 |
 *
 * 安全约束：token 与题目正文一律不写日志（本类不引入日志拦截器、不打印正文）。
 */
@Singleton
class MinerUClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.MINUTES)
        .callTimeout(15, TimeUnit.MINUTES)
        .followRedirects(false)
        .followSslRedirects(false)
        .retryOnConnectionFailure(false)
        .build()

    /**
     * @param token 为 null / 空白时自动走**免 Token 轻量通道**，不再要求用户先去注册。
     *              已配置 Token 则走精准通道（支持表格、公式与整本教辅）。
     */
    suspend fun parse(
        token: String?,
        file: File,
        params: ParseParams,
        onProgress: (ParseProgress) -> Unit = {},
    ): ParseOutcome = withContext(Dispatchers.IO) {
        validate(token, file)?.let { return@withContext ParseOutcome.Failure(it) }
        val lite = token.isNullOrBlank()
        try {
            onProgress(ParseProgress.Preparing)
            if (lite) doParseLite(file, params, onProgress) else doParse(token!!.trim(), file, params, onProgress)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ParseFailure) {
            ParseOutcome.Failure(e.error)
        } catch (e: SocketTimeoutException) {
            ParseOutcome.Failure(ImportError.Timeout(if (lite) LITE_TIMEOUT_MINUTES else TOTAL_TIMEOUT_MINUTES))
        } catch (e: InterruptedIOException) {
            ParseOutcome.Failure(ImportError.Timeout(if (lite) LITE_TIMEOUT_MINUTES else TOTAL_TIMEOUT_MINUTES))
        } catch (e: IOException) {
            ParseOutcome.Failure(ImportError.Offline)
        } catch (e: Exception) {
            ParseOutcome.Failure(ImportError.Unknown(e.message))
        }
    }

    // ---------------------------------------------------------------- 主流程

    private suspend fun doParse(
        token: String,
        file: File,
        params: ParseParams,
        onProgress: (ParseProgress) -> Unit,
    ): ParseOutcome {
        val dataId = UUID.randomUUID().toString()
        val (batchId, uploadUrl) = requestUploadUrl(token, file, params, dataId)
        uploadFile(uploadUrl, file, onProgress)
        val result = awaitResult(token, batchId, dataId, params, onProgress)
        val zipUrl = result.fullZipUrl
        if (zipUrl.isNullOrBlank()) {
            throw ParseFailure(ImportError.Server(result.errMsg))
        }
        val zip = downloadZip(batchId, zipUrl, onProgress)
        val markdown = readMarkdown(zip)
        onProgress(ParseProgress.Done)
        return ParseOutcome.Success(markdown = markdown, rawZipPath = zip.absolutePath, fileName = file.name)
    }

    // ---------------------------------------------------------------- 轻量通道（免 Token）

    /**
     * 轻量通道：无鉴权、单文件、10MB / 20 页，只出 markdown（表格与公式不保证）。
     * 好处是装完 App 就能导入第一道题，不必先去外部注册。
     */
    private suspend fun doParseLite(
        file: File,
        params: ParseParams,
        onProgress: (ParseProgress) -> Unit,
    ): ParseOutcome {
        val (taskId, uploadUrl) = requestLiteUpload(file, params)
        uploadFile(uploadUrl, file, onProgress)
        val markdownUrl = awaitLiteResult(taskId, params, onProgress)
        val markdown = downloadMarkdown(taskId, markdownUrl, onProgress)
        onProgress(ParseProgress.Done)
        // 轻量通道没有 zip 结果包，rawZipPath 留空
        return ParseOutcome.Success(markdown = markdown, rawZipPath = null, fileName = file.name)
    }

    private fun requestLiteUpload(file: File, params: ParseParams): Pair<String, String> {
        val payload = LiteSubmitRequest(fileName = file.name, language = params.language)
        val body = json.encodeToString(LiteSubmitRequest.serializer(), payload)
            .toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(LITE_SUBMIT_ENDPOINT)
            .header("Accept", "application/json")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            ensureHttpOk(response)
            val text = response.body?.string().orEmpty()
            val parsed = decode(text) { json.decodeFromString(LiteSubmitResponse.serializer(), text) }
            throwIfApiError(codeTextOf(parsed.code), parsed.msg)
            val data = parsed.data ?: throw ParseFailure(ImportError.Server(parsed.msg))
            val taskId = data.taskId
            val url = data.fileUrl
            if (taskId.isBlank() || url.isBlank()) {
                throw ParseFailure(ImportError.Server("轻量通道未返回任务编号，请稍后重试。"))
            }
            return taskId to url
        }
    }

    /** 轮询单任务；轻量通道只有 running / done / failed，没有分页进度 */
    private suspend fun awaitLiteResult(
        taskId: String,
        params: ParseParams,
        onProgress: (ParseProgress) -> Unit,
    ): String {
        val interval = params.pollIntervalMs.coerceAtLeast(MIN_POLL_INTERVAL_MS)
        val deadline = System.currentTimeMillis() + LITE_TIMEOUT_MINUTES * 60_000L
        while (true) {
            currentCoroutineContext().ensureActive()
            if (System.currentTimeMillis() > deadline) {
                throw ParseFailure(ImportError.Timeout(LITE_TIMEOUT_MINUTES))
            }
            val data = fetchLiteStatus(taskId)
            when (data.state.lowercase(Locale.ROOT)) {
                "waiting-file", "pending" -> onProgress(ParseProgress.Queued)
                "running", "converting", "parsing" -> onProgress(ParseProgress.Parsing(0, 0))
                "done" -> {
                    val url = data.markdownUrl
                    if (url.isNullOrBlank()) {
                        throw ParseFailure(ImportError.Server("轻量通道未返回 Markdown 地址，请稍后重试。"))
                    }
                    return url
                }
                "failed" -> {
                    val msg = data.errMsg?.takeIf { it.isNotBlank() }
                    val code = codeTextOf(data.errCode)
                    throw ParseFailure(ImportError.Server(msg ?: "轻量解析失败${code?.let { "（$it）" }.orEmpty()}"))
                }
                else -> Unit // 未知状态继续等待，由总超时兜底
            }
            delay(interval)
        }
    }

    private fun fetchLiteStatus(taskId: String): LiteStatusData {
        val request = Request.Builder()
            .url("$LITE_STATUS_ENDPOINT/$taskId")
            .header("Accept", "application/json")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            ensureHttpOk(response)
            val text = response.body?.string().orEmpty()
            val parsed = decode(text) { json.decodeFromString(LiteStatusResponse.serializer(), text) }
            throwIfApiError(codeTextOf(parsed.code), parsed.msg)
            return parsed.data ?: throw ParseFailure(ImportError.Server(parsed.msg))
        }
    }

    /** 轻量通道直接给 .md 直链，不需要解压 */
    private suspend fun downloadMarkdown(
        taskId: String,
        markdownUrl: String,
        onProgress: (ParseProgress) -> Unit,
    ): String {
        if (!SafeUrl.isAllowed(markdownUrl)) {
            throw ParseFailure(ImportError.Server("解析结果下载地址不合法，已阻止下载。"))
        }
        onProgress(ParseProgress.Downloading)
        val request = Request.Builder().url(markdownUrl).get().build()
        client.newCall(request).execute().use { response ->
            ensureHttpOk(response)
            val body = response.body ?: throw ParseFailure(ImportError.Server("下载内容为空。"))
            if (body.contentLength() > MAX_MARKDOWN_BYTES) {
                throw ParseFailure(ImportError.Server("识别结果超过 20MB，请拆分文件后重试。"))
            }
            val bytes = body.byteStream().use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(BUFFER_SIZE)
                var total = 0L
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = input.read(buffer)
                    if (read == -1) break
                    total += read
                    if (total > MAX_MARKDOWN_BYTES) {
                        throw ParseFailure(ImportError.Server("识别结果超过 20MB，请拆分文件后重试。"))
                    }
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            }
            return String(bytes, Charsets.UTF_8).removePrefix("\uFEFF")
        }
    }

    /** 步骤 a：申请上传地址 */
    private fun requestUploadUrl(
        token: String,
        file: File,
        params: ParseParams,
        dataId: String,
    ): Pair<String, String> {
        val payload = FileUrlRequest(
            files = listOf(FileItem(name = file.name, dataId = dataId)),
            modelVersion = params.modelVersion,
            language = params.language,
            enableTable = params.enableTable,
            enableFormula = params.enableFormula,
        )
        val body = json.encodeToString(FileUrlRequest.serializer(), payload)
            .toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(FILE_URLS_ENDPOINT)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            ensureHttpOk(response)
            val text = response.body?.string().orEmpty()
            val parsed = decode(text) { json.decodeFromString(FileUrlResponse.serializer(), text) }
            throwIfApiError(codeTextOf(parsed.code), parsed.msg)
            val data = parsed.data ?: throw ParseFailure(ImportError.Server(parsed.msg))
            val url = data.fileUrls.firstOrNull { it.isNotBlank() }
                ?: throw ParseFailure(ImportError.Server("未获取到上传地址，请稍后重试。"))
            val batch = data.batchId
            if (batch.isBlank()) throw ParseFailure(ImportError.Server("未获取到任务编号，请稍后重试。"))
            return batch to url
        }
    }

    /** 步骤 b：PUT 直传（不设置 Content-Type，签名 URL 自己带鉴权） */
    private fun uploadFile(
        uploadUrl: String,
        file: File,
        onProgress: (ParseProgress) -> Unit,
    ) {
        onProgress(ParseProgress.Uploading)
        val body = object : RequestBody() {
            override fun contentType(): MediaType? = null
            override fun contentLength(): Long = file.length()
            override fun writeTo(sink: BufferedSink) {
                val total = file.length().coerceAtLeast(1L)
                var written = 0L
                var lastPercent = -1
                file.inputStream().use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        sink.write(buffer, 0, read)
                        written += read
                        val percent = ((written * 100) / total).toInt().coerceIn(0, 100)
                        if (percent != lastPercent) {
                            lastPercent = percent
                            onProgress(ParseProgress.Uploaded(percent))
                        }
                    }
                }
                sink.flush()
            }
        }
        val request = Request.Builder().url(uploadUrl).put(body).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw ParseFailure(ImportError.Server("文件上传失败（HTTP ${response.code}），请稍后重试。"))
            }
        }
        onProgress(ParseProgress.Uploaded(100))
    }

    /** 步骤 c：轮询识别结果，总超时 15 分钟 */
    private suspend fun awaitResult(
        token: String,
        batchId: String,
        dataId: String,
        params: ParseParams,
        onProgress: (ParseProgress) -> Unit,
    ): ExtractResult {
        val interval = params.pollIntervalMs.coerceAtLeast(MIN_POLL_INTERVAL_MS)
        val deadline = System.currentTimeMillis() + TOTAL_TIMEOUT_MINUTES * 60_000L
        while (true) {
            currentCoroutineContext().ensureActive()
            if (System.currentTimeMillis() > deadline) {
                throw ParseFailure(ImportError.Timeout(TOTAL_TIMEOUT_MINUTES))
            }
            val data = fetchBatchResults(token, batchId)
            val item = data.extractResult.firstOrNull { it.dataId == dataId }
                ?: data.extractResult.firstOrNull()
            if (item != null) {
                when (item.state.lowercase(Locale.ROOT)) {
                    "waiting-file" -> onProgress(ParseProgress.Uploaded(90))
                    "pending" -> onProgress(ParseProgress.Queued)
                    "running", "converting" -> onProgress(
                        ParseProgress.Parsing(
                            extractedPages = item.extractProgress?.extractedPages ?: 0,
                            totalPages = item.extractProgress?.totalPages ?: 0,
                        )
                    )
                    "done" -> return item
                    "failed" -> throw ParseFailure(ImportError.Server(item.errMsg))
                    else -> Unit // 未知状态继续等待，由总超时兜底
                }
            }
            delay(interval)
        }
    }

    private fun fetchBatchResults(token: String, batchId: String): BatchResultData {
        val request = Request.Builder()
            .url("$EXTRACT_RESULTS_ENDPOINT/$batchId")
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            ensureHttpOk(response)
            val text = response.body?.string().orEmpty()
            val parsed = decode(text) { json.decodeFromString(BatchResultResponse.serializer(), text) }
            throwIfApiError(codeTextOf(parsed.code), parsed.msg)
            return parsed.data ?: throw ParseFailure(ImportError.Server(parsed.msg))
        }
    }

    /** 步骤 d：下载 zip（校验 URL + 体积上限），返回本地缓存文件 */
    private suspend fun downloadZip(
        batchId: String,
        zipUrl: String,
        onProgress: (ParseProgress) -> Unit,
    ): File {
        if (!SafeUrl.isAllowed(zipUrl)) {
            throw ParseFailure(ImportError.Server("解析结果下载地址不合法，已阻止下载。"))
        }
        onProgress(ParseProgress.Downloading)
        val dir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
        val safeName = batchId.ifBlank { UUID.randomUUID().toString() }.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val target = File(dir, "$safeName.zip")

        // 签名 URL 自带鉴权，不附带 Authorization，避免 token 泄露给第三方存储
        val request = Request.Builder()
            .url(zipUrl)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            ensureHttpOk(response)
            val body = response.body ?: throw ParseFailure(ImportError.Server("下载内容为空。"))
            if (body.contentLength() > MAX_ZIP_BYTES) {
                throw ParseFailure(ImportError.Server("解析结果超过 100MB，已终止下载。"))
            }
            body.byteStream().use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        total += read
                        if (total > MAX_ZIP_BYTES) {
                            throw ParseFailure(ImportError.Server("解析结果超过 100MB，已终止下载。"))
                        }
                        output.write(buffer, 0, read)
                    }
                }
            }
        }
        return target
    }

    /** 从 zip 中取出 full.md（找不到则取任意 .md），上限 20MB，UTF-8 解码 */
    private fun readMarkdown(zip: File): String {
        ZipFile(zip).use { zipFile ->
            val entries = mutableListOf<ZipEntry>()
            val enumeration = zipFile.entries()
            while (enumeration.hasMoreElements()) {
                val entry = enumeration.nextElement()
                if (!entry.isDirectory) entries += entry
            }
            val entry = entries.firstOrNull { it.name.endsWith("full.md", ignoreCase = true) }
                ?: entries.firstOrNull { it.name.endsWith(".md", ignoreCase = true) }
                ?: throw ParseFailure(ImportError.Server("解析结果中未找到 Markdown 文件。"))
            if (entry.size > MAX_MARKDOWN_BYTES) {
                throw ParseFailure(ImportError.Server("Markdown 超过 20MB，请拆分文件后重试。"))
            }
            val bytes = zipFile.getInputStream(entry).use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    total += read
                    if (total > MAX_MARKDOWN_BYTES) {
                        throw ParseFailure(ImportError.Server("Markdown 超过 20MB，请拆分文件后重试。"))
                    }
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            }
            return String(bytes, Charsets.UTF_8).removePrefix("\uFEFF")
        }
    }

    // ---------------------------------------------------------------- 校验与错误映射

    private fun validate(token: String?, file: File): ImportError? {
        val lite = token.isNullOrBlank()
        if (!file.exists() || !file.isFile) return ImportError.BadFile("找不到原始文件，请重新选择。")
        if (file.length() <= 0L) return ImportError.BadFile("文件内容为空，请重新选择。")
        if (file.length() > (if (lite) MAX_LITE_FILE_BYTES else MAX_FILE_BYTES)) {
            return ImportError.BadFile(
                if (lite) {
                    "轻量通道单文件上限 10MB。请拆分文件，或在设置中配置 Token 改用精准通道（上限 200MB）。"
                } else {
                    "文件超过 200MB，请压缩或拆分后再导入。"
                },
            )
        }
        val ext = file.extension.lowercase(Locale.ROOT)
        if (ext.isBlank()) {
            return ImportError.BadFile("无法识别文件格式，请选择带后缀的 PDF/Word/PPT/Excel/图片/HTML。")
        }
        if (ext !in SUPPORTED_EXTENSIONS) {
            return ImportError.BadFile("暂不支持 .$ext 格式，请使用 PDF/Word/PPT/Excel/图片/HTML。")
        }
        return null
    }

    private fun ensureHttpOk(response: Response) {
        if (response.isSuccessful) return
        val code = response.code
        val body = runCatching { response.body?.string() }.getOrNull().orEmpty()
        val error = when {
            code == 401 || code == 403 -> ImportError.TokenInvalid
            code == 429 -> ImportError.RateLimited
            code in 500..599 -> ImportError.Server("识别服务异常（HTTP $code），请稍后重试。")
            body.contains("A0202") -> ImportError.TokenInvalid
            body.contains("A0211") -> ImportError.TokenExpired
            else -> ImportError.Server("请求失败（HTTP $code），请稍后重试。")
        }
        throw ParseFailure(error)
    }

    private fun throwIfApiError(codeText: String?, msg: String) {
        if (codeText == null || codeText == "0" || codeText == "200") return
        val error = when (codeText) {
            "A0202" -> ImportError.TokenInvalid
            "A0211" -> ImportError.TokenExpired
            "A0212" -> ImportError.RateLimited
            else -> ImportError.Server(msg.ifBlank { "接口返回错误码 $codeText，请稍后重试。" })
        }
        throw ParseFailure(error)
    }

    /** code 兼容 `0`（数字）与 `A0202`（字符串）两种形态 */
    private fun codeTextOf(element: JsonElement?): String? = when (element) {
        null, JsonNull -> null
        is JsonPrimitive -> element.content
        else -> null
    }

    private fun <T> decode(text: String, block: () -> T): T {
        if (text.isBlank()) throw ParseFailure(ImportError.Server("接口返回内容为空。"))
        return try {
            block()
        } catch (e: ParseFailure) {
            throw e
        } catch (e: Exception) {
            throw ParseFailure(ImportError.Unknown("响应格式无法识别。"))
        }
    }

    private class ParseFailure(val error: ImportError) : Exception()

    private companion object {
        const val FILE_URLS_ENDPOINT = "https://mineru.net/api/v4/file-urls/batch"
        const val EXTRACT_RESULTS_ENDPOINT = "https://mineru.net/api/v4/extract-results/batch"
        /** 轻量通道：提交走 .../parse/file，查询走 .../parse/{task_id} */
        const val LITE_SUBMIT_ENDPOINT = "https://mineru.net/api/v1/agent/parse/file"
        const val LITE_STATUS_ENDPOINT = "https://mineru.net/api/v1/agent/parse"
        const val CACHE_DIR = "mineru"
        const val TOTAL_TIMEOUT_MINUTES = 15
        const val LITE_TIMEOUT_MINUTES = 10
        const val MIN_POLL_INTERVAL_MS = 500L
        const val BUFFER_SIZE = 64 * 1024
        const val MAX_FILE_BYTES = 200L * 1024 * 1024
        const val MAX_LITE_FILE_BYTES = 10L * 1024 * 1024
        const val MAX_ZIP_BYTES = 100L * 1024 * 1024
        const val MAX_MARKDOWN_BYTES = 20L * 1024 * 1024

        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        val SUPPORTED_EXTENSIONS = setOf(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx",
            "png", "jpg", "jpeg", "jp2", "webp", "gif", "bmp", "html", "htm",
        )
    }
}
