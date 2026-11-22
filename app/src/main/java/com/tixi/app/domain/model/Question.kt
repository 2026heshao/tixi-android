package com.tixi.app.domain.model

/** 题目（分类内顺序由 sortOrder 决定，与 id 组成稳定排序） */
data class Question(
    val id: Long = 0,
    val categoryId: Long? = null,
    val stem: String,
    val options: List<String> = emptyList(),
    val answer: AnswerData = AnswerData.Empty,
    val explanation: String? = null,
    val typeKey: String = BuiltInQuestionType.CHOICE.key,
    val richBlocks: List<RichBlock> = emptyList(),
    val sortOrder: Int = 0,
    val sourceFile: String? = null,
    val sourceImagePath: String? = null,
    val tags: List<String> = emptyList(),
    val remark: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
) {
    val builtInType: BuiltInQuestionType? get() = BuiltInQuestionType.fromKey(typeKey)
    val hasRichContent: Boolean get() = richBlocks.isNotEmpty()

    /** 选择题/填空题需要提交与自评；大题与自定义题型只展示 */
    val isAnswerable: Boolean
        get() = builtInType == BuiltInQuestionType.CHOICE || builtInType == BuiltInQuestionType.FILL

    /** 题干中的空位数量，用于填空题提示 */
    val blankCount: Int get() = if (builtInType == BuiltInQuestionType.FILL) {
        Regex("_{2,}").findAll(stem).count().coerceAtLeast(1)
    } else 0
}

/** 单层分类；null 分类即「未分类」（哨兵值 [UNCATEGORIZED_ID] 用于进度与路由） */
data class Category(
    val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0,
    val questionCount: Int = 0,
) {
    companion object {
        const val UNCATEGORIZED_ID = -1L
    }
}

/** 题型项（设置页题型管理 + 编辑页切换器共用数据源） */
data class QuestionTypeEntry(
    val id: Long = 0,
    val key: String,
    val name: String,
    val builtIn: Boolean = false,
    val sortOrder: Int = 0,
    val questionCount: Int = 0,
)

/** 练习进度：按分类记录「上次练到第几题」 */
data class PracticeProgress(
    val categoryId: Long,
    val lastQuestionId: Long? = null,
    val updatedAt: Long = 0,
)
