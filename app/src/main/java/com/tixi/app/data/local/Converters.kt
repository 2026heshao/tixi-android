package com.tixi.app.data.local

import androidx.room.TypeConverter
import com.tixi.app.domain.model.AnswerData
import com.tixi.app.domain.model.RichBlock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * 富内容 / 答案的 JSON 编解码工具。
 *
 * ⚠️ 当前**不参与 Room 的转换器机制**：所有实体列在库里都是 TEXT，
 * Room 侧的编解码由 `Mappers` 完成（生成的 `getRequiredConverters()` 全为空）。
 * 因此 `TixiDatabase.create()` 里不能再 `addTypeConverter(this)` —— 多传实例会让
 * `RoomDatabase.init()` 抛 `Unexpected type converter` 并导致首次查库崩溃。
 *
 * 将来若把某列改成 `List<String>` / `AnswerData` / `List<RichBlock>` 这类自定义类型，
 * 才需要把它重新标成 `@ProvidedTypeConverter` 并在建库时注入。
 *
 * 容错约定：反序列化失败绝不抛异常，返回空集合 / [AnswerData.Empty]，脏数据只会丢内容不会崩。
 */
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
