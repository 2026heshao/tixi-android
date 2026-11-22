package com.tixi.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 设计 v1.4 的三种内置题型；题型表可新增自定义题型（按通用展示渲染） */
enum class BuiltInQuestionType(val key: String, val label: String) {
    CHOICE("choice", "选择题"),
    FILL("fill", "填空题"),
    ESSAY("essay", "大题");

    companion object {
        fun fromKey(key: String?): BuiltInQuestionType? = entries.firstOrNull { it.key == key }
        fun labelOf(key: String?, fallback: String? = null): String =
            fromKey(key)?.label ?: fallback ?: "通用题目"
    }
}

/**
 * 答案统一结构（不自动判分，仅用于展示标准答案）：
 * - choice：correctKeys = ["B"]
 * - fill：correctKeys = ["60 日", "30 日"]，空位顺序与题干中 `____` 一致
 * - essay：text = 参考答案全文
 */
@Serializable
data class AnswerData(
    val correctKeys: List<String> = emptyList(),
    val text: String? = null,
) {
    fun isEmpty(): Boolean = correctKeys.isEmpty() && text.isNullOrBlank()

    /** 答案区展示文本：选择题 "B · 60 日" 无法反查选项文本，故由 UI 传入 options 处理 */
    fun simpleDisplay(): String = when {
        !text.isNullOrBlank() -> text
        correctKeys.isNotEmpty() -> correctKeys.joinToString(" / ")
        else -> "—"
    }

    companion object {
        val Empty = AnswerData()
    }
}

/** 富内容块：题面素材，与答案解耦，不参与判分 */
@Serializable
sealed interface RichBlock {
    val caption: String?

    @Serializable
    @SerialName("table")
    data class Table(
        val rows: List<List<String>>,
        override val caption: String? = null,
    ) : RichBlock

    @Serializable
    @SerialName("formula")
    data class Formula(
        val latex: String,
        override val caption: String? = null,
    ) : RichBlock

    @Serializable
    @SerialName("image")
    data class ImageBlock(
        val path: String,
        val alt: String = "",
        override val caption: String? = null,
    ) : RichBlock
}

/** 质量标记：识别闸门的判定结果（R1） */
enum class QualityFlag(val label: String, val detail: String) {
    SUSPECT_GARBLED("疑似乱码", "含较多不可读字符，建议核对或重试识别"),
    TOO_SHORT("内容过短", "题干少于 8 字，可能未识别完整"),
    BAD_OPTIONS("选项异常", "选择题选项少于 2 个或存在空选项"),
    NO_ANSWER("答案缺失", "未识别到标准答案"),
    HAS_RICH_CONTENT("含富内容", "含表格/公式/原图，必须人工确认");

    /** 是否必须人工逐题确认，不允许批量跳过 */
    val requiresManualReview: Boolean get() = this == HAS_RICH_CONTENT || this == SUSPECT_GARBLED
}
