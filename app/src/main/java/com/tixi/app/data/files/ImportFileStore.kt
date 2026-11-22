package com.tixi.app.data.files

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 导入文件落盘：外部 Uri 拷进私有目录，相机拍照写 cache，
 * 需要把私有文件交给相机/分享时用 [uriFor] 走 FileProvider。
 */
@Singleton
class ImportFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val resolver: ContentResolver
        get() = context.contentResolver

    /** 流式拷贝到 filesDir/imports/<时间戳>_<清洗名>；失败会删掉半截文件再抛错 */
    suspend fun copyToPrivate(uri: Uri, displayName: String): File = withContext(Dispatchers.IO) {
        val dir = importsDir()
        val target = uniqueTarget(dir, "${System.currentTimeMillis()}_${sanitize(displayName)}")
        val input = resolver.openInputStream(uri) ?: throw IOException("无法读取所选文件，请重新选择")
        try {
            input.use { source ->
                target.outputStream().use { sink -> source.copyTo(sink) }
            }
        } catch (error: IOException) {
            deleteQuietly(target)
            throw error
        }
        target
    }

    /** 相机拍照的目标文件（cache 目录，7 天清理由系统负责） */
    fun newCameraFile(): File {
        val dir = File(context.cacheDir, "camera")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "IMG_${System.currentTimeMillis()}.jpg")
    }

    /** 与 AndroidManifest 中 `${applicationId}.fileprovider` 保持一致 */
    fun uriFor(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** 读取内容提供者的展示名，取不到时退回 Uri 末段 */
    fun displayName(uri: Uri): String {
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            runCatching {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (index >= 0) cursor.getString(index)?.takeIf { it.isNotBlank() }?.let { return it }
                        }
                    }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "未命名文件"
    }

    fun deleteQuietly(file: File) {
        runCatching { if (file.exists()) file.delete() }
    }

    /** 从数据库/草稿里存的路径还原 File；仅用于读本应用私有文件 */
    fun fileFor(path: String): File = File(path)

    private fun importsDir(): File {
        val dir = File(context.filesDir, "imports")
        if (!dir.exists() && !dir.mkdirs()) throw IOException("无法创建导入目录")
        return dir
    }

    private fun uniqueTarget(dir: File, candidate: String): File {
        var file = File(dir, candidate)
        if (!file.exists()) return file
        val dot = candidate.lastIndexOf('.')
        val stem = if (dot > 0) candidate.substring(0, dot) else candidate
        val ext = if (dot > 0) candidate.substring(dot) else ""
        var index = 1
        while (file.exists()) {
            file = File(dir, "${stem}_$index$ext")
            index += 1
        }
        return file
    }

    /** 去掉路径分隔符与非法字符，限制长度以免超出文件系统限制 */
    private fun sanitize(name: String): String {
        val flattened = name.substringAfterLast('/').substringAfterLast('\\')
        val cleaned = flattened
            .replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
            .trim('_', '.')
        val base = cleaned.ifBlank { "file" }
        if (base.length <= MAX_NAME_LENGTH) return base
        val dot = base.lastIndexOf('.')
        return if (dot > 0 && base.length - dot <= 12) {
            base.take(MAX_NAME_LENGTH - (base.length - dot)) + base.substring(dot)
        } else {
            base.take(MAX_NAME_LENGTH)
        }
    }

    private companion object {
        const val MAX_NAME_LENGTH = 80
    }
}
