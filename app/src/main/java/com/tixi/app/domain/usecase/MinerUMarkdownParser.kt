package com.tixi.app.domain.usecase

import com.tixi.app.domain.model.AnswerData
import com.tixi.app.domain.model.BuiltInQuestionType
import com.tixi.app.domain.model.ParsedQuestion
import com.tixi.app.domain.model.RichBlock

/**
 * MinerU markdown → 题目草稿。纯 Kotlin（不依赖 android.*），可直接 JVM 单测。
 * 原则：宁可少拆，不可拆错 —— 识别不出的结构退回正文，绝不抛异常。
 */
object MinerUMarkdownParser {

    private const val MAX_STEM_LENGTH = 4000

    private val questionCnRegex = Regex("""^\s*第\s*(\d{1,3})\s*题\s*[.、．:：]?\s*(.*)$""")
    private val questionHeadingRegex = Regex("""^\s*#{1,4}\s*(\d{1,3})\s*[.、．)）\s]\s*(.*)$""")
    private val questionNumberRegex = Regex("""^\s{0,4}(\d{1,3})\s*[.、．)）]\s*(.*)$""")

    private val optionRegex = Regex("""^\s*[（(]?\s*([A-Ha-h])\s*[.、．)）]\s*(\S.*)$""")
    private val answerRegex = Regex("""^\s*(?:参考答案|正确答案|答案)\s*[：:]?\s*(.*)$""")
    private val explanationRegex = Regex("""^\s*(?:解析|分析|说明|详解)\s*[：:]?\s*(.*)$""")
    private val imageRegex = Regex("""^\s*!\[([^\]]*)]\(\s*([^)\s]+)(?:\s+"[^"]*")?\s*\)\s*$""")

    /** 行内选项记号：`A.` / `（B）` / `C、`，前后不能紧贴字母数字，避免误吃正文。 */
    private val optionTokenRegex = Regex("""(?<![A-Za-z0-9])[A-Ha-h]\s*[.、．)）]\s*""")

    private val inlineMathRegex = Regex("""\$\$?([^$]+?)\$\$?""")
    private val parenMathRegex = Regex("""\\\((.+?)\\\)""")

    /** LaTeX 命令 → 可读符号。MinerU 的行内公式只有降级成纯文本才不会以源码形式出现在题干里。 */
    private val mathSymbols = mapOf(
        "times" to "×", "cdot" to "·", "div" to "÷", "pm" to "±", "mp" to "∓",
        "le" to "≤", "leq" to "≤", "ge" to "≥", "geq" to "≥", "neq" to "≠", "ne" to "≠",
        "approx" to "≈", "equiv" to "≡", "infty" to "∞", "to" to "→",
        "rightarrow" to "→", "leftarrow" to "←", "Rightarrow" to "⇒",
        "sum" to "∑", "int" to "∫", "prod" to "∏", "in" to "∈", "notin" to "∉",
        "subset" to "⊂", "subseteq" to "⊆", "cup" to "∪", "cap" to "∩",
        "angle" to "∠", "perp" to "⊥", "parallel" to "∥", "triangle" to "△",
        "degree" to "°", "circ" to "°", "because" to "∵", "therefore" to "∴",
        "alpha" to "α", "beta" to "β", "gamma" to "γ", "delta" to "δ",
        "epsilon" to "ε", "varepsilon" to "ε", "zeta" to "ζ", "eta" to "η",
        "theta" to "θ", "iota" to "ι", "kappa" to "κ", "lambda" to "λ", "mu" to "μ",
        "nu" to "ν", "xi" to "ξ", "pi" to "π", "rho" to "ρ", "sigma" to "σ",
        "tau" to "τ", "phi" to "φ", "varphi" to "φ", "chi" to "χ", "psi" to "ψ",
        "omega" to "ω", "Delta" to "Δ", "Omega" to "Ω", "Sigma" to "Σ", "Gamma" to "Γ",
        "Pi" to "Π", "Lambda" to "Λ", "Phi" to "Φ", "Theta" to "Θ",
        "ldots" to "…", "cdots" to "…", "dots" to "…", "quad" to " ", "qquad" to " ",
        "%" to "%", "{" to "{", "}" to "}", "|" to "‖",
    )

    private val blankRegex = Regex("""_{2,}""")
    private val headingLineRegex = Regex("""^#{1,6}\s+""")
    private val hrRegex = Regex("""^(?:[-*_]\s*){3,}$""")
    private val multiNewlineRegex = Regex("""\n{3,}""")
    private val answerPrefixRegex = Regex("""^\s*(?:正确答案|参考答案|答案|故选|应选|选|答)\s*[：:]?\s*""")
    private val choiceKeyUpperRegex = Regex("""^[（(【\[]?\s*([A-H](?:[\s,，、/]*[A-H])*)(?![A-Za-z])""")
    private val choiceKeyLowerRegex = Regex("""^[（(【\[]?\s*([a-h](?:[\s,，、/]*[a-h])*)(?![A-Za-z])""")

    fun parse(markdown: String): List<ParsedQuestion> {
        val text = markdown.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n')
        if (text.isBlank()) return emptyList()
        val lines = text.split('\n')

        val starts = mutableListOf<Int>()
        var lastNumber = Int.MIN_VALUE
        for (index in lines.indices) {
            val detected = detectQuestionStart(lines[index]) ?: continue
            // 题号必须递增，避免把题干里的「1. 2.」子项误拆成新题
            if (detected.first <= lastNumber) continue
            starts += index
            lastNumber = detected.first
        }

        if (starts.isEmpty()) {
            val single = parseSegment(lines) ?: return emptyList()
            return listOf(single)
        }

        val questions = mutableListOf<ParsedQuestion>()
        for (i in starts.indices) {
            val begin = starts[i]
            val end = if (i + 1 < starts.size) starts[i + 1] else lines.size
            val segment = ArrayList<String>(end - begin)
            detectQuestionStart(lines[begin])?.second?.takeIf { it.isNotBlank() }?.let { segment += it }
            for (j in begin + 1 until end) segment += lines[j]
            parseSegment(segment)?.let { questions += it }
        }
        return questions
    }

    private fun detectQuestionStart(line: String): Pair<Int, String>? {
        questionCnRegex.matchEntire(line)?.let { match ->
            val number = match.groupValues[1].toIntOrNull() ?: return null
            return number to match.groupValues[2].trim()
        }
        questionHeadingRegex.matchEntire(line)?.let { match ->
            val number = match.groupValues[1].toIntOrNull() ?: return null
            return number to match.groupValues[2].trim()
        }
        questionNumberRegex.matchEntire(line)?.let { match ->
            val number = match.groupValues[1].toIntOrNull() ?: return null
            return number to match.groupValues[2].trim()
        }
        return null
    }

    private fun parseSegment(lines: List<String>): ParsedQuestion? {
        val stem = StringBuilder()
        val options = mutableListOf<String>()
        var answerFirst: String? = null
        val answerExtra = StringBuilder()
        val explanation = StringBuilder()
        val rich = mutableListOf<RichBlock>()
        var stage = Stage.STEM
        var i = 0

        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) {
                i++
                continue
            }

            // 代码围栏：内容当正文，保留信息
            if (line.startsWith("```")) {
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    appendText(lines[i], stage, stem, answerExtra, explanation)
                    i++
                }
                i++
                continue
            }

            // HTML 表格
            if (line.contains("<table", ignoreCase = true)) {
                val html = StringBuilder(lines[i])
                var closed = line.contains("</table", ignoreCase = true)
                i++
                while (!closed && i < lines.size) {
                    html.append('\n').append(lines[i])
                    if (lines[i].contains("</table", ignoreCase = true)) closed = true
                    i++
                }
                val rows = parseHtmlTable(html.toString())
                if (rows.isNotEmpty()) {
                    rich += RichBlock.Table(rows)
                }
                continue
            }

            // Markdown 表格：当前行有 |，下一行是 --- 分隔
            if (line.contains('|') && i + 1 < lines.size && isTableSeparator(lines[i + 1])) {
                val rows = mutableListOf<List<String>>()
                rows += splitTableRow(line)
                i += 2
                while (i < lines.size && lines[i].contains('|') && lines[i].isNotBlank()) {
                    rows += splitTableRow(lines[i])
                    i++
                }
                rich += RichBlock.Table(rows)
                continue
            }

            // 独立公式块 $$...$$ / \[...\]
            if (line.startsWith("$$") || line.startsWith("\\[")) {
                val next = consumeFormula(lines, i, rich)
                if (next != null) {
                    i = next
                    continue
                }
            }

            // 图片
            val image = imageRegex.matchEntire(line)
            if (image != null) {
                val path = image.groupValues[2].trim()
                if (path.isNotEmpty()) {
                    rich += RichBlock.ImageBlock(path = path, alt = image.groupValues[1].trim())
                }
                i++
                continue
            }

            // 答案
            val answer = answerRegex.matchEntire(line)
            if (answer != null) {
                answerFirst = answer.groupValues[1].trim()
                answerExtra.clear()
                stage = Stage.ANSWER
                i++
                continue
            }

            // 解析（题干为空时不抢先判定，避免「分析下列……」这类题干被误吞）
            val explanationLine = explanationRegex.matchEntire(line)
            if (explanationLine != null && (stage == Stage.ANSWER || stem.isNotEmpty() || explanation.isNotEmpty())) {
                val first = explanationLine.groupValues[1].trim()
                if (first.isNotEmpty()) {
                    if (explanation.isNotEmpty()) explanation.append('\n')
                    explanation.append(first)
                }
                stage = Stage.EXPLANATION
                i++
                continue
            }

            // 选项 A：题干与选项挤在同一行（MinerU OCR 的常态，如「…（）A.5 B.6 C.7 D.8」）
            if (stage == Stage.STEM) {
                val inline = splitOptionsInLine(line)
                if (inline != null) {
                    if (inline.first.isNotBlank()) appendText(inline.first, stage, stem, answerExtra, explanation)
                    options += inline.second
                    i++
                    continue
                }
            }

            // 选项 B：每行一个选项、且连续出现 ≥2 个
            val option = optionRegex.matchEntire(line)
            if (option != null) {
                val run = mutableListOf<String>()
                run += option.groupValues[2].trim()
                var j = i + 1
                while (j < lines.size) {
                    val next = lines[j].trim()
                    if (next.isEmpty()) {
                        j++
                        continue
                    }
                    val nextOption = optionRegex.matchEntire(next) ?: break
                    run += nextOption.groupValues[2].trim()
                    j++
                }
                if (run.size >= 2) {
                    options += run
                    stage = Stage.STEM
                    i = j
                    continue
                }
            }

            appendText(line, stage, stem, answerExtra, explanation)
            i++
        }

        val stemText = cleanText(flattenInlineMath(stem.toString()))
        if (stemText.isEmpty()) return null

        val cleanOptions = options
            .map { flattenInlineMath(it).trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        val typeKey = when {
            cleanOptions.size >= 2 -> BuiltInQuestionType.CHOICE.key
            blankRegex.containsMatchIn(stemText) -> BuiltInQuestionType.FILL.key
            else -> BuiltInQuestionType.ESSAY.key
        }

        val answerText = flattenInlineMath(
            buildString {
                append(answerFirst.orEmpty().trim())
                val extra = answerExtra.toString().trim()
                if (extra.isNotEmpty()) {
                    if (isNotEmpty()) append('\n')
                    append(extra)
                }
            },
        ).trim()

        return ParsedQuestion(
            stem = stemText,
            options = cleanOptions,
            answer = buildAnswer(typeKey, answerText, stemText),
            explanation = flattenInlineMath(explanation.toString()).trim().ifEmpty { null },
            typeKey = typeKey,
            richBlocks = rich.toList(),
        )
    }

    /**
     * 把一行里的 A./B./C./D. 选项全部切出来。
     * 只认「从 A 开始、字母连续递增」的最长序列，避免把正文里的「A.」误当选项。
     * 返回 (选项前的题干, 选项文本列表)；不足 2 个选项时返回 null。
     */
    private fun splitOptionsInLine(line: String): Pair<String, List<String>>? {
        val tokens = optionTokenRegex.findAll(line).toList()
        if (tokens.size < 2) return null

        var best: List<MatchResult> = emptyList()
        for (start in tokens.indices) {
            if (tokens[start].value.trimStart().firstOrNull()?.uppercaseChar() != 'A') continue
            val run = mutableListOf(tokens[start])
            var expect = 'B'
            for (k in start + 1 until tokens.size) {
                val letter = tokens[k].value.trimStart().firstOrNull()?.uppercaseChar() ?: break
                if (letter == expect) {
                    run += tokens[k]
                    expect += 1
                } else if (letter > expect) {
                    break
                }
            }
            if (run.size > best.size) best = run
        }
        if (best.size < 2) return null

        val stemPart = line.substring(0, best.first().range.first).trim()
        val opts = best.indices.map { index ->
            val end = if (index + 1 < best.size) best[index + 1].range.first else line.length
            line.substring(best[index].range.last + 1, end).trim().trimEnd('，', ',', '；', ';', '、')
        }
        return stemPart to opts
    }

    /** 行内 `$...$` / `\(...\)` 公式降级成纯文本，避免把 LaTeX 源码当正文展示。 */
    private fun flattenInlineMath(text: String): String {
        if (text.isEmpty()) return text
        var out = text
        out = inlineMathRegex.replace(out) { match -> flattenMath(match.groupValues[1]) }
        out = parenMathRegex.replace(out) { match -> flattenMath(match.groupValues[1]) }
        return out
    }

    /** 纯 Kotlin 的轻量 LaTeX 降级：分式、根号、上下标去壳，符号表查不到的的命令直接丢弃命令名。 */
    private fun flattenMath(raw: String): String {
        var s = raw
        // OCR 常把句号识别成 \circ，作为上下标时直接去掉
        s = s.replace(Regex("""_\s*\{\s*\\circ\s*}\s*"""), "")
        s = s.replace(Regex("""\^\s*\{\s*\\circ\s*}\s*"""), "°")
        repeat(3) {
            s = s.replace(Regex("""\\frac\s*\{([^{}]*)\}\s*\{([^{}]*)\}""")) { m ->
                "(${m.groupValues[1]})/(${m.groupValues[2]})"
            }
        }
        s = s.replace(Regex("""\\sqrt\s*\[([^\]]*)]\s*\{([^{}]*)\}""")) { m ->
            "√[${m.groupValues[1]}](${m.groupValues[2]})"
        }
        s = s.replace(Regex("""\\sqrt\s*\{([^{}]*)\}""")) { m -> "√(${m.groupValues[1]})" }
        repeat(2) {
            s = s.replace(
                Regex("""\\(?:mathrm|mathbf|mathit|mathsf|mathtt|text|textrm|textnormal|operatorname|boldsymbol|mathcal)\s*\{([^{}]*)\}"""),
            ) { m -> m.groupValues[1] }
        }
        s = s.replace(Regex("""\\(?:left|right|big|Big|bigg|Bigg)\b"""), "")
        s = s.replace(Regex("""\\(?:,|;|!|:|\s)"""), "")
        s = s.replace(Regex("""\\([a-zA-Z]+)""")) { m -> mathSymbols[m.groupValues[1]].orEmpty() }
        s = s.replace(Regex("""[{}]"""), "")
        s = s.replace(Regex("""\s+"""), " ")
        s = s.replace(Regex("""\s+([)\],.。，、;；:：])"""), "$1")
        s = s.replace(Regex("""([(\[])\s+"""), "$1")
        s = s.replace(Regex("""\s+([(\[])"""), "$1")
        s = s.replace(Regex("""\s*([=+\-<>±×÷·/≤≥≠≈])\s*"""), "$1")
        // 数字与相邻字母/根号之间不留空格（`2 x` → `2x`），但保留 `sin x` 这类函数名间距
        s = s.replace(Regex("""(?<=\d)\s+(?=[A-Za-zα-ωΑ-Ω√])"""), "")
        s = s.replace(Regex("""(?<=[A-Za-zα-ωΑ-Ω√])\s+(?=\d)"""), "")
        return s.trim()
    }

    private fun cleanText(raw: String): String =
        raw.replace(multiNewlineRegex, "\n\n").trim().take(MAX_STEM_LENGTH)

    private fun appendText(
        line: String,
        stage: Stage,
        stem: StringBuilder,
        answer: StringBuilder,
        explanation: StringBuilder,
    ) {
        val text = line.trim()
        if (text.isEmpty()) return
        if (headingLineRegex.containsMatchIn(text)) return // 章节标题不进题干
        if (hrRegex.matches(text)) return
        val target = when (stage) {
            Stage.STEM -> stem
            Stage.ANSWER -> answer
            Stage.EXPLANATION -> explanation
        }
        if (target.isNotEmpty()) target.append('\n')
        target.append(text)
    }

    // ---------------------------------------------------------------- 富内容

    private fun consumeFormula(lines: List<String>, start: Int, rich: MutableList<RichBlock>): Int? {
        val first = lines[start].trim()
        val isDollar = first.startsWith("$$")
        val open = if (isDollar) "$$" else "\\["
        val close = if (isDollar) "$$" else "\\]"
        val buffer = StringBuilder(first.removePrefix(open))
        if (buffer.contains(close)) {
            val latex = buffer.toString().substringBefore(close).trim()
            if (latex.isNotEmpty()) rich += RichBlock.Formula(latex)
            return start + 1
        }
        val closingIndex = (start + 1 until lines.size).firstOrNull { lines[it].contains(close) } ?: return null
        for (k in start + 1..closingIndex) {
            buffer.append('\n').append(lines[k].substringBefore(close))
        }
        val latex = buffer.toString().trim()
        if (latex.isNotEmpty()) rich += RichBlock.Formula(latex)
        return closingIndex + 1
    }

    private fun isTableSeparator(line: String): Boolean {
        val text = line.trim()
        if (!text.contains('|')) return false
        var dashes = 0
        for (ch in text) {
            when (ch) {
                '|', ':', ' ', '\t' -> Unit
                '-' -> dashes++
                else -> return false
            }
        }
        return dashes >= 3
    }

    private fun splitTableRow(line: String): List<String> {
        var text = line.trim()
        if (text.startsWith("|")) text = text.substring(1)
        if (text.endsWith("|")) text = text.dropLast(1)
        return text.split('|').map { it.trim() }
    }

    private fun parseHtmlTable(html: String): List<List<String>> {
        val rowRegex = Regex("""<tr[^>]*>([\s\S]*?)</tr>""", RegexOption.IGNORE_CASE)
        val cellRegex = Regex("""<t[dh][^>]*>([\s\S]*?)</t[dh]>""", RegexOption.IGNORE_CASE)
        val rows = mutableListOf<List<String>>()
        for (row in rowRegex.findAll(html)) {
            val cells = cellRegex.findAll(row.groupValues[1]).map { cleanCell(it.groupValues[1]) }.toList()
            if (cells.isNotEmpty()) rows += cells
        }
        return rows
    }

    private fun cleanCell(raw: String): String = raw
        .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""<[^>]+>"""), "")
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace(Regex("""\s+"""), " ")
        .trim()

    // ---------------------------------------------------------------- 答案

    private fun buildAnswer(typeKey: String, answerText: String, stem: String): AnswerData = when (typeKey) {
        BuiltInQuestionType.CHOICE.key ->
            if (answerText.isEmpty()) AnswerData.Empty else AnswerData(correctKeys = extractChoiceKeys(answerText))

        BuiltInQuestionType.FILL.key -> buildFillAnswer(answerText, stem)

        else -> if (answerText.isEmpty()) AnswerData.Empty else AnswerData(text = answerText)
    }

    private fun extractChoiceKeys(raw: String): List<String> {
        val cleaned = raw.replace(answerPrefixRegex, "").trim()
        if (cleaned.isEmpty()) return emptyList()
        val group = choiceKeyUpperRegex.find(cleaned)?.groupValues?.get(1).orEmpty()
            .ifEmpty { choiceKeyLowerRegex.find(cleaned)?.groupValues?.get(1).orEmpty() }
        if (group.isEmpty()) return emptyList()
        return group.uppercase().filter { it in 'A'..'H' }.map { it.toString() }.distinct()
    }

    private fun buildFillAnswer(raw: String, stem: String): AnswerData {
        if (raw.isEmpty()) return AnswerData.Empty
        val blankCount = blankRegex.findAll(stem).count()
        val parts = raw.split('、', '/', ',', '，').map { it.trim() }.filter { it.isNotEmpty() }
        return if (blankCount >= 2 && parts.size >= 2) {
            AnswerData(correctKeys = parts)
        } else {
            AnswerData(text = raw)
        }
    }

    private enum class Stage { STEM, ANSWER, EXPLANATION }
}
