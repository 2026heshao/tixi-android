package com.tixi.app.data.local

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.tixi.app.domain.model.AnswerData
import com.tixi.app.domain.model.RichBlock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Room 类型转换器。因为需要注入统一配置的 [Json]（多态判别键 type），
 * 所以标记 [ProvidedTypeConverter]，实例由 DataModule 在 build 数据库时传入。
 *
 * 容错约定：反序列化失败绝不抛异常，返回空集合 / [AnswerData.Empty]，脏数据只会丢内容不会崩。
 */
@ProvidedTypeConverter
class Converters @Inject constructor(
    private val json: Json,
) {

    // ---- List<String>：标签 / 质量标记 ----

    @TypeConverter
    fun fromStringList(value: List<String>?): String? =
        if (value.isNullOrEmpty()) null else runCatching { json.encodeToString(value) }.getOrNull()

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<String>>(value) }.getOrDefault(emptyList())
    }

    // ---- AnswerData：标准答案 ----

    @TypeConverter
    fun fromAnswerData(value: AnswerData?): String? =
        if (value == null) null else runCatching { json.encodeToString(value) }.getOrNull()

    @TypeConverter
    fun toAnswerData(value: String?): AnswerData {
        if (value.isNullOrBlank()) return AnswerData.Empty
        return runCatching { json.decodeFromString<AnswerData>(value) }.getOrDefault(AnswerData.Empty)
    }

    // ---- List<RichBlock>：表格 / 公式 / 原图 ----

    @TypeConverter
    fun fromRichBlocks(value: List<RichBlock>?): String? =
        if (value.isNullOrEmpty()) null else runCatching { json.encodeToString(value) }.getOrNull()

    @TypeConverter
    fun toRichBlocks(value: String?): List<RichBlock> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<RichBlock>>(value) }.getOrDefault(emptyList())
    }
}
