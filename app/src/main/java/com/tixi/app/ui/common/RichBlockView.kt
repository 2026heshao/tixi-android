package com.tixi.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tixi.app.domain.model.RichBlock
import com.tixi.app.ui.theme.TixiColors
import com.tixi.app.ui.theme.TixiFontFamilies
import com.tixi.app.ui.theme.TixiTheme
import java.io.File

// ---------------------------------------------------------------------------
// LaTeX 轻量子集渲染器（无 WebView / 无第三方库）
// ---------------------------------------------------------------------------

private val TEX_SYMBOLS: Map<String, String> = mapOf(
    "cdot" to "·",
    "times" to "×",
    "div" to "÷",
    "pm" to "±",
    "mp" to "∓",
    "le" to "≤",
    "leq" to "≤",
    "ge" to "≥",
    "geq" to "≥",
    "neq" to "≠",
    "ne" to "≠",
    "approx" to "≈",
    "equiv" to "≡",
    "infty" to "∞",
    "pi" to "π",
    "alpha" to "α",
    "beta" to "β",
    "gamma" to "γ",
    "delta" to "δ",
    "theta" to "θ",
    "lambda" to "λ",
    "mu" to "μ",
    "rho" to "ρ",
    "sigma" to "σ",
    "tau" to "τ",
    "phi" to "φ",
    "omega" to "ω",
    "Delta" to "Δ",
    "Omega" to "Ω",
    "sum" to "∑",
    "prod" to "∏",
    "int" to "∫",
    "to" to "→",
    "rightarrow" to "→",
    "Rightarrow" to "⇒",
    "lt" to "<",
    "gt" to ">",
    "angle" to "∠",
    "degree" to "°",
    "circ" to "∘",
    "because" to "∵",
    "therefore" to "∴",
    "in" to "∈",
    "notin" to "∉",
    "cup" to "∪",
    "cap" to "∩",
    "subset" to "⊂",
    "subseteq" to "⊆",
    "cdots" to "⋯",
    "ldots" to "…",
    "quad" to " ",
    "qquad" to "  ",
    "sin" to "sin",
    "cos" to "cos",
    "tan" to "tan",
    "log" to "log",
    "ln" to "ln",
    "lim" to "lim",
)

private sealed interface Tex {
    data class Txt(val text: String) : Tex
    data class Group(val children: List<Tex>) : Tex
    data class Frac(val numerator: List<Tex>, val denominator: List<Tex>) : Tex
    data class Sqrt(val body: List<Tex>) : Tex
    data class Script(val base: Tex, val sup: List<Tex>?, val sub: List<Tex>?) : Tex
}

private class TexParser(private val src: String) {
    private var pos = 0

    fun parseAll(): List<Tex> = parseList(inGroup = false)

    private fun parseList(inGroup: Boolean): List<Tex> {
        val out = ArrayList<Tex>()
        val buffer = StringBuilder()

        fun flush() {
            if (buffer.isNotEmpty()) {
                out.add(Tex.Txt(buffer.toString()))
                buffer.clear()
            }
        }

        while (pos < src.length) {
            val c = src[pos]
            when {
                c == '{' -> {
                    flush()
                    pos++
                    out.add(Tex.Group(parseList(inGroup = true)))
                }

                c == '}' -> {
                    pos++
                    flush()
                    if (inGroup) return out
                }

                c == '\\' -> {
                    flush()
                    pos++
                    out.add(readCommand())
                }

                c == '^' || c == '_' -> {
                    flush()
                    pos++
                    val arg = readArgument()
                    val base = if (out.isNotEmpty()) out.removeAt(out.size - 1) else null
                    val attachment: Tex = when {
                        base == null -> Tex.Script(
                            base = Tex.Txt(""),
                            sup = if (c == '^') arg else null,
                            sub = if (c == '_') arg else null,
                        )

                        base is Tex.Script -> if (c == '^') base.copy(sup = arg) else base.copy(sub = arg)

                        else -> {
                            val (head, tail) = splitTrailingChar(base)
                            if (head != null) out.add(head)
                            Tex.Script(
                                base = tail,
                                sup = if (c == '^') arg else null,
                                sub = if (c == '_') arg else null,
                            )
                        }
                    }
                    out.add(attachment)
                }

                c == '~' -> {
                    buffer.append(' ')
                    pos++
                }

                else -> {
                    buffer.append(c)
                    pos++
                }
            }
        }
        flush()
        return out
    }

    private fun splitTrailingChar(node: Tex): Pair<Tex?, Tex> = when {
        node is Tex.Txt && node.text.length > 1 ->
            Tex.Txt(node.text.dropLast(1)) to Tex.Txt(node.text.takeLast(1))

        else -> null to node
    }

    private fun readCommand(): Tex {
        if (pos >= src.length) return Tex.Txt("\\")
        if (!src[pos].isLetter()) {
            val ch = src[pos]
            pos++
            return when (ch) {
                '{' -> Tex.Txt("{")
                '}' -> Tex.Txt("}")
                ' ' -> Tex.Txt(" ")
                ',', ';', ':', '\\' -> Tex.Txt(" ")
                else -> Tex.Txt(ch.toString())
            }
        }
        val start = pos
        while (pos < src.length && src[pos].isLetter()) pos++
        val name = src.substring(start, pos)
        return when (name) {
            "frac", "dfrac", "tfrac" -> Tex.Frac(readArgument(), readArgument())
            "sqrt" -> Tex.Sqrt(readArgument())
            "text", "mathrm", "mathbf", "operatorname" -> Tex.Group(readArgument())
            else -> Tex.Txt(TEX_SYMBOLS[name] ?: "\\$name")
        }
    }

    private fun readArgument(): List<Tex> {
        while (pos < src.length && src[pos] == ' ') pos++
        if (pos >= src.length) return emptyList()
        return when (src[pos]) {
            '{' -> {
                pos++
                parseList(inGroup = true)
            }

            '\\' -> {
                pos++
                listOf(readCommand())
            }

            else -> {
                val ch = src[pos]
                pos++
                listOf(Tex.Txt(ch.toString()))
            }
        }
    }
}

/**
 * 公式渲染：支持 \frac、\sqrt、上下标、常用运算/希腊字母符号。
 * 解析失败或遇到不支持的命令时原样输出，绝不崩溃。
 */
@Composable
fun FormulaView(
    latex: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    color: Color = TixiTheme.colors.textPrimary,
) {
    val nodes = remember(latex) {
        runCatching { TexParser(latex).parseAll() }.getOrElse { emptyList() }
    }
    if (latex.isBlank()) return
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (nodes.isEmpty()) {
            Text(
                text = latex,
                color = color,
                fontSize = fontSize,
                fontFamily = TixiFontFamilies.Serif,
                fontStyle = FontStyle.Italic,
            )
        } else {
            TexNodeList(nodes = nodes, fontSize = fontSize, color = color)
        }
    }
}

@Composable
private fun TexNodeList(nodes: List<Tex>, fontSize: TextUnit, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        nodes.forEach { node -> TexNodeView(node = node, fontSize = fontSize, color = color) }
    }
}

@Composable
private fun TexNodeView(node: Tex, fontSize: TextUnit, color: Color) {
    when (node) {
        is Tex.Txt -> {
            if (node.text.isNotEmpty()) {
                Text(
                    text = node.text,
                    color = color,
                    fontSize = fontSize,
                    lineHeight = (fontSize.value * 1.2f).sp,
                    fontFamily = TixiFontFamilies.Serif,
                    fontStyle = FontStyle.Italic,
                    softWrap = false,
                )
            }
        }

        is Tex.Group -> TexNodeList(nodes = node.children, fontSize = fontSize, color = color)

        is Tex.Frac -> FractionView(
            color = color,
            numerator = {
                TexNodeList(nodes = node.numerator, fontSize = (fontSize.value * 0.9f).sp, color = color)
            },
            denominator = {
                TexNodeList(nodes = node.denominator, fontSize = (fontSize.value * 0.9f).sp, color = color)
            },
        )

        is Tex.Sqrt -> Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "√",
                color = color,
                fontSize = fontSize,
                fontFamily = TixiFontFamilies.Serif,
            )
            Column(
                modifier = Modifier
                    .padding(top = 1.dp)
                    .drawBehind {
                        val stroke = 1.dp.toPx()
                        drawLine(
                            color = color,
                            start = Offset(0f, stroke / 2f),
                            end = Offset(size.width, stroke / 2f),
                            strokeWidth = stroke,
                        )
                    },
            ) {
                TexNodeList(nodes = node.body, fontSize = fontSize, color = color)
            }
        }

        is Tex.Script -> Row(verticalAlignment = Alignment.CenterVertically) {
            TexNodeView(node = node.base, fontSize = fontSize, color = color)
            val scriptSize = (fontSize.value * 0.62f).sp
            Column {
                if (node.sup != null) {
                    Row(modifier = Modifier.offset(y = 2.dp)) {
                        TexNodeList(nodes = node.sup, fontSize = scriptSize, color = color)
                    }
                }
                if (node.sub != null) {
                    Row(modifier = Modifier.offset(y = (-1).dp)) {
                        TexNodeList(nodes = node.sub, fontSize = scriptSize, color = color)
                    }
                }
            }
        }
    }
}

/** 分式：上下叠放 + 中线，横线宽度取分子/分母较宽者。 */
@Composable
private fun FractionView(
    color: Color,
    numerator: @Composable () -> Unit,
    denominator: @Composable () -> Unit,
) {
    Layout(
        content = {
            numerator()
            denominator()
            Box(modifier = Modifier.background(color))
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val numPlaceable = measurables[0].measure(loose)
        val denPlaceable = measurables[1].measure(loose)
        val gap = 3.dp.roundToPx()
        val barThickness = 1.dp.roundToPx().coerceAtLeast(1)
        val sidePadding = (maxOf(numPlaceable.width, denPlaceable.width) * 0.14f).toInt()
        val barWidth = maxOf(numPlaceable.width, denPlaceable.width) + sidePadding
        val barPlaceable = measurables[2].measure(Constraints.fixed(barWidth, barThickness))
        val totalWidth = barWidth
        val totalHeight = numPlaceable.height + gap + barThickness + gap + denPlaceable.height
        layout(totalWidth, totalHeight) {
            val centerX = totalWidth / 2
            numPlaceable.placeRelative(centerX - numPlaceable.width / 2, 0)
            barPlaceable.placeRelative(0, numPlaceable.height + gap)
            denPlaceable.placeRelative(
                centerX - denPlaceable.width / 2,
                numPlaceable.height + gap + barThickness + gap,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 富内容展示
// ---------------------------------------------------------------------------

private fun richBlockLabel(block: RichBlock): String = when (block) {
    is RichBlock.Table -> "表 格"
    is RichBlock.Formula -> "公 式"
    is RichBlock.ImageBlock -> "原 图 · 函数图"
}

/** 富内容块列表：surfaceVariant 容器 + 10dp 圆角 + 顶部字距加宽标签。 */
@Composable
fun RichBlocksView(
    blocks: List<RichBlock>,
    modifier: Modifier = Modifier,
    eyeCareActive: Boolean = true,
) {
    val colors = TixiTheme.colors
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        blocks.forEach { block ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surfaceVariant)
                    .padding(12.dp),
            ) {
                Text(
                    text = richBlockLabel(block),
                    color = colors.textTertiary,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = TixiFontFamilies.Sans,
                )
                Spacer(Modifier.height(8.dp))
                when (block) {
                    is RichBlock.Table -> RichTable(rows = block.rows, colors = colors)

                    is RichBlock.Formula -> Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            FormulaView(
                                latex = block.latex,
                                fontSize = 16.sp,
                                color = colors.textPrimary,
                            )
                        }
                    }

                    is RichBlock.ImageBlock -> RichImageBlock(
                        block = block,
                        colors = colors,
                        eyeCareActive = eyeCareActive,
                    )
                }
                if (!block.caption.isNullOrBlank()) {
                    Text(
                        text = block.caption.orEmpty(),
                        modifier = Modifier.padding(top = 6.dp),
                        color = colors.textTertiary,
                        fontSize = 11.sp,
                        fontFamily = TixiFontFamilies.Sans,
                    )
                }
            }
        }
    }
}

/**
 * 表格：用 TextMeasurer 预量列宽，保证「不压缩列宽」并支持横向滚动。
 */
@Composable
private fun RichTable(rows: List<List<String>>, colors: TixiColors) {
    val colCount = rows.maxOfOrNull { it.size } ?: 0
    if (rows.isEmpty() || colCount == 0) return

    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val cellStyle = TextStyle(
        fontFamily = TixiFontFamilies.Sans,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        color = colors.textPrimary,
    )
    val headerStyle = cellStyle.copy(fontWeight = FontWeight.Medium)

    val tableData = remember(rows, colors.isDark, measurer, density) {
        with(density) {
            val padH = 8.dp.roundToPx()
            val padV = 6.dp.roundToPx()
            val maxCellWidth = 240.dp.roundToPx()
            val measured = rows.mapIndexed { rowIndex, row ->
                (0 until colCount).map { colIndex ->
                    measurer.measure(
                        text = AnnotatedString(row.getOrNull(colIndex).orEmpty()),
                        style = if (rowIndex == 0) headerStyle else cellStyle,
                        constraints = Constraints(maxWidth = maxCellWidth),
                    )
                }
            }
            val colWidths = IntArray(colCount) { colIndex ->
                measured.maxOf { cells -> cells[colIndex].size.width } + padH * 2
            }
            val rowHeights = IntArray(rows.size) { rowIndex ->
                measured[rowIndex].maxOf { it.size.height } + padV * 2
            }
            TableLayoutData(
                measured = measured,
                colWidths = colWidths,
                rowHeights = rowHeights,
                totalWidth = colWidths.sum(),
                totalHeight = rowHeights.sum(),
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        Canvas(
            modifier = Modifier
                .width(with(density) { tableData.totalWidth.toDp() })
                .height(with(density) { tableData.totalHeight.toDp() }),
        ) {
            val padH = 8.dp.toPx()
            val padV = 6.dp.toPx()
            val stroke = 1.dp.toPx()

            var y = 0f
            tableData.rowHeights.forEachIndexed { rowIndex, rowHeight ->
                if (rowIndex == 0) {
                    drawRect(
                        color = colors.surface,
                        topLeft = Offset(0f, y),
                        size = Size(tableData.totalWidth.toFloat(), rowHeight.toFloat()),
                    )
                }
                var x = 0f
                tableData.colWidths.forEachIndexed { colIndex, colWidth ->
                    val textLayout = tableData.measured[rowIndex][colIndex]
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(x + padH, y + padV),
                    )
                    x += colWidth
                }
                y += rowHeight
            }

            var lineY = 0f
            for (rowIndex in tableData.rowHeights.indices) {
                drawLine(
                    color = colors.outline,
                    start = Offset(0f, lineY),
                    end = Offset(tableData.totalWidth.toFloat(), lineY),
                    strokeWidth = stroke,
                )
                lineY += tableData.rowHeights[rowIndex]
            }
            drawLine(
                color = colors.outline,
                start = Offset(0f, tableData.totalHeight.toFloat() - stroke / 2f),
                end = Offset(tableData.totalWidth.toFloat(), tableData.totalHeight.toFloat() - stroke / 2f),
                strokeWidth = stroke,
            )

            var lineX = 0f
            for (colIndex in tableData.colWidths.indices) {
                drawLine(
                    color = colors.outline,
                    start = Offset(lineX, 0f),
                    end = Offset(lineX, tableData.totalHeight.toFloat()),
                    strokeWidth = stroke,
                )
                lineX += tableData.colWidths[colIndex]
            }
            drawLine(
                color = colors.outline,
                start = Offset(tableData.totalWidth.toFloat() - stroke / 2f, 0f),
                end = Offset(tableData.totalWidth.toFloat() - stroke / 2f, tableData.totalHeight.toFloat()),
                strokeWidth = stroke,
            )
        }
    }
}

private class TableLayoutData(
    val measured: List<List<TextLayoutResult>>,
    val colWidths: IntArray,
    val rowHeights: IntArray,
    val totalWidth: Int,
    val totalHeight: Int,
)

@Composable
private fun RichImageBlock(
    block: RichBlock.ImageBlock,
    colors: TixiColors,
    eyeCareActive: Boolean,
) {
    val colorFilter = remember(colors.isDark, eyeCareActive) {
        if (colors.isDark && eyeCareActive) {
            ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f,
                    ),
                ),
            )
        } else {
            null
        }
    }

    AsyncImage(
        model = File(block.path),
        contentDescription = block.alt.ifBlank { "题目原图" },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.FillWidth,
        colorFilter = colorFilter,
    )
    if (block.alt.isNotBlank()) {
        Text(
            text = block.alt,
            modifier = Modifier.padding(top = 6.dp),
            color = colors.textTertiary,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            fontFamily = TixiFontFamilies.Sans,
        )
    }
}

// ---------------------------------------------------------------------------
// 富内容编辑
// ---------------------------------------------------------------------------

@Composable
fun RichBlockEditor(
    blocks: List<RichBlock>,
    onChange: (List<RichBlock>) -> Unit,
    onRequestReplaceImage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        blocks.forEachIndexed { index, block ->
            EditorCard(
                label = richBlockLabel(block),
                onDelete = {
                    onChange(blocks.filterIndexed { i, _ -> i != index })
                },
            ) {
                when (block) {
                    is RichBlock.Table -> TableEditor(block = block) { updated ->
                        onChange(blocks.toMutableList().also { it[index] = updated })
                    }

                    is RichBlock.Formula -> FormulaEditor(block = block) { updated ->
                        onChange(blocks.toMutableList().also { it[index] = updated })
                    }

                    is RichBlock.ImageBlock -> ImageEditor(
                        block = block,
                        onRequestReplaceImage = { onRequestReplaceImage(index) },
                        onDelete = { onChange(blocks.filterIndexed { i, _ -> i != index }) },
                    ) { updated ->
                        onChange(blocks.toMutableList().also { it[index] = updated })
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorCard(
    label: String,
    onDelete: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = TixiTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceVariant)
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = colors.textTertiary,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = TixiFontFamilies.Sans,
            )
            TixiIconButton(
                icon = Icons.Outlined.Close,
                contentDescription = "删除此块",
                onClick = onDelete,
                tint = colors.textTertiary,
            )
        }
        content()
    }
}

@Composable
private fun TableEditor(block: RichBlock.Table, onUpdate: (RichBlock.Table) -> Unit) {
    val colors = TixiTheme.colors
    val rows = block.rows
    val colCount = rows.maxOfOrNull { it.size } ?: 0

    fun updateCell(rowIndex: Int, colIndex: Int, value: String) {
        val updated = rows.mapIndexed { ri, row ->
            if (ri != rowIndex) {
                row
            } else {
                val cells = row.toMutableList()
                while (cells.size <= colIndex) cells.add("")
                cells[colIndex] = value
                cells
            }
        }
        onUpdate(block.copy(rows = updated))
    }

    Column {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Column {
                rows.forEachIndexed { rowIndex, row ->
                    Row {
                        for (colIndex in 0 until colCount) {
                            EditableCell(
                                value = row.getOrNull(colIndex).orEmpty(),
                                isHeader = rowIndex == 0,
                                colors = colors,
                                onValueChange = { updateCell(rowIndex, colIndex, it) },
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TixiButton(
                text = "+ 行",
                onClick = {
                    onUpdate(block.copy(rows = rows + listOf(List(colCount.coerceAtLeast(1)) { "" })))
                },
                variant = TixiButtonVariant.Outlined,
                size = TixiButtonSize.Small,
            )
            TixiButton(
                text = "− 行",
                onClick = { if (rows.size > 1) onUpdate(block.copy(rows = rows.dropLast(1))) },
                variant = TixiButtonVariant.Outlined,
                size = TixiButtonSize.Small,
                enabled = rows.size > 1,
            )
            TixiButton(
                text = "+ 列",
                onClick = { onUpdate(block.copy(rows = rows.map { it + "" })) },
                variant = TixiButtonVariant.Outlined,
                size = TixiButtonSize.Small,
            )
            TixiButton(
                text = "− 列",
                onClick = {
                    if (colCount > 1) {
                        onUpdate(block.copy(rows = rows.map { row -> if (row.size > 1) row.dropLast(1) else row }))
                    }
                },
                variant = TixiButtonVariant.Outlined,
                size = TixiButtonSize.Small,
                enabled = colCount > 1,
            )
        }
    }
}

@Composable
private fun EditableCell(
    value: String,
    isHeader: Boolean,
    colors: TixiColors,
    onValueChange: (String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .width(92.dp)
            .height(34.dp)
            .background(if (focused) colors.surface else Color.Transparent)
            .border(1.dp, if (focused) colors.primary else colors.outline)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        textStyle = TextStyle(
            fontFamily = TixiFontFamilies.Sans,
            fontSize = 12.sp,
            color = colors.textPrimary,
            fontWeight = if (isHeader) FontWeight.Medium else FontWeight.Normal,
        ),
        singleLine = true,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(colors.primary),
    )
}

@Composable
private fun FormulaEditor(block: RichBlock.Formula, onUpdate: (RichBlock.Formula) -> Unit) {
    val colors = TixiTheme.colors
    var draft by remember(block.latex) { mutableStateOf(block.latex) }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface)
                .border(1.dp, colors.outline, RoundedCornerShape(8.dp))
                .padding(10.dp),
        ) {
            if (draft.isEmpty()) {
                Text(
                    text = "输入 LaTeX，如 \\frac{a}{b}",
                    color = colors.textTertiary,
                    fontSize = 11.sp,
                    fontFamily = TixiFontFamilies.Mono,
                )
            }
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    fontFamily = TixiFontFamilies.Mono,
                    fontSize = 11.sp,
                    lineHeight = 18.sp,
                    color = colors.textPrimary,
                ),
                minLines = 2,
                cursorBrush = SolidColor(colors.primary),
            )
        }
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TixiButton(
                text = "更新预览",
                onClick = { onUpdate(block.copy(latex = draft)) },
                variant = TixiButtonVariant.Tonal,
                size = TixiButtonSize.Small,
                enabled = draft != block.latex,
            )
            Text(
                text = "公式将居中展示",
                color = colors.textTertiary,
                fontSize = 11.sp,
                fontFamily = TixiFontFamilies.Sans,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface)
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (block.latex.isBlank()) {
                Text(
                    text = "暂无预览",
                    color = colors.textTertiary,
                    fontSize = 11.sp,
                    fontFamily = TixiFontFamilies.Sans,
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        FormulaView(
                            latex = block.latex,
                            fontSize = 15.sp,
                            color = colors.textPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageEditor(
    block: RichBlock.ImageBlock,
    onRequestReplaceImage: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: (RichBlock.ImageBlock) -> Unit,
) {
    val colors = TixiTheme.colors
    Row(verticalAlignment = Alignment.Top) {
        AsyncImage(
            model = File(block.path),
            contentDescription = block.alt.ifBlank { "图片缩略图" },
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            TixiTextField(
                value = block.alt,
                onValueChange = { onUpdate(block.copy(alt = it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "图片说明（alt）",
            )
            Row(
                modifier = Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TixiButton(
                    text = "替换图片",
                    onClick = onRequestReplaceImage,
                    variant = TixiButtonVariant.Tonal,
                    size = TixiButtonSize.Small,
                )
                Spacer(Modifier.width(4.dp))
                DangerTextAction(text = "删除", onClick = onDelete)
            }
        }
    }
}
