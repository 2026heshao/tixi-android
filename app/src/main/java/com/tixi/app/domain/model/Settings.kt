package com.tixi.app.domain.model

/** 本轮练习统计：只在内存维护，展示完即弃，不写回数据库（沿用 R3） */
data class SessionStats(
    val total: Int = 0,
    val elapsedMs: Long = 0,
    val mastered: Int = 0,
    val weakStems: List<String> = emptyList(),
) {
    val masteryRate: Float get() = if (total <= 0) 0f else mastered.toFloat() / total
    val weakCount: Int get() = weakStems.size

    fun elapsedLabel(): String {
        val minutes = (elapsedMs / 60000L).toInt()
        return if (minutes < 1) "<1′" else "$minutes′"
    }
}

enum class ThemeMode(val key: String, val label: String) {
    SYSTEM("system", "跟随系统"),
    LIGHT("light", "浅色"),
    DARK("dark", "深色");

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

/** 解析参数：设置页可调，随请求下发 MinerU */
data class ParseParams(
    val language: String = "ch",
    val enableTable: Boolean = true,
    val enableFormula: Boolean = true,
    val modelVersion: String = "pipeline",
    val pollIntervalMs: Long = 3000L,
) {
    companion object {
        val Default = ParseParams()
    }
}
