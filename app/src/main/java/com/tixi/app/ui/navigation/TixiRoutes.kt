package com.tixi.app.ui.navigation

import com.tixi.app.domain.model.Category

/** 12 条真实路由：21 个展示屏按状态机收敛到这些路由（落地方案 §3.4） */
object TixiRoutes {
    const val ONBOARDING = "onboarding?step={step}"
    fun onboarding(step: Int = 1) = "onboarding?step=$step"
    const val HOME = "home"
    const val SETTINGS = "settings"

    const val CATEGORIES = "categories"
    const val PRACTICE = "practice/{categoryId}"
    fun practice(categoryId: Long) = "practice/$categoryId"

    const val LIBRARY = "library"
    const val QUESTION_EDIT = "question_edit/{questionId}"
    fun questionEdit(questionId: Long) = "question_edit/$questionId"

    const val CATEGORY_MANAGE = "category_manage"

    const val IMPORT_SOURCE = "import/source"
    const val IMPORT_TASK = "import/task"
    const val IMPORT_REVIEW = "import/review/{draftId}"
    fun importReview(draftId: Long) = "import/review/$draftId"
    const val IMPORT_HISTORY = "import/history"

    /** 未分类走哨兵 -1，落库时转回 null */
    fun decodeCategoryId(raw: Long?): Long? =
        if (raw == null || raw == Category.UNCATEGORIZED_ID) null else raw

    fun encodeCategoryId(id: Long?): Long = id ?: Category.UNCATEGORIZED_ID
}
