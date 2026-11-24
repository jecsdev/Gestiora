package com.jecsdev.gestiora.report

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Picture
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import com.jecsdev.gestiora.presentation.ui.PrenatalChart
import com.jecsdev.gestiora.presentation.ui.PrenatalReportBlock
import com.jecsdev.gestiora.presentation.ui.PrenatalReportDocument
import com.jecsdev.gestiora.presentation.ui.PrenatalReportPage
import java.io.OutputStream

internal fun writePrenatalPdf(report: PrenatalReportDocument, output: OutputStream) {
    val pdf = PdfDocument()
    try {
        var page: PdfDocument.Page? = null
        renderPrenatalReport(
            report,
            startPage = { number ->
                pdf.startPage(PdfDocument.PageInfo.Builder(WIDTH, HEIGHT, number).create())
                    .also { page = it }.canvas
            },
            finishPage = { pdf.finishPage(checkNotNull(page)) },
        )
        pdf.writeTo(output)
    } finally {
        pdf.close()
    }
}

internal fun previewPrenatalReport(report: PrenatalReportDocument): List<PrenatalReportPage> = buildList {
    var picture = Picture()
    val description = StringBuilder()
    renderPrenatalReport(
        report,
        startPage = {
            picture = Picture()
            description.clear()
            picture.beginRecording(WIDTH, HEIGHT)
        },
        finishPage = {
            val recorded = picture
            recorded.endRecording()
            add(PrenatalReportPage(WIDTH.toFloat() / HEIGHT, description.toString().trim()) {
                scale(size.width / WIDTH, pivot = Offset.Zero) {
                    drawIntoCanvas { it.nativeCanvas.drawPicture(recorded) }
                }
            })
        },
        appendPageText = { description.appendLine(it) },
    )
}

private fun renderPrenatalReport(
    report: PrenatalReportDocument,
    startPage: (Int) -> Canvas,
    finishPage: () -> Unit,
    appendPageText: ((CharSequence) -> Unit)? = null,
) {
    val layout = PrenatalPdfLayout(report, startPage, finishPage, appendPageText)
    try {
        report.blocks.forEach { block ->
            when (block) {
                is PrenatalReportBlock.Text -> layout.text(block)
                is PrenatalReportBlock.Chart -> layout.chart(block.plot)
            }
        }
    } finally {
        layout.finishPage()
    }
}

private class PrenatalPdfLayout(
    private val report: PrenatalReportDocument,
    private val startPage: (Int) -> Canvas,
    private val endPage: () -> Unit,
    private val appendPageText: ((CharSequence) -> Unit)?,
) {
    private var page: Canvas? = null
    private var pageNumber = 0
    private var y = MARGIN
    private val accent = report.ink.accent.toInt()
    private val body = Color.rgb(41, 37, 42)

    private fun pageFor(height: Int): Canvas {
        if (page == null || y + height > BOTTOM) {
            finishPage()
            val next = startPage(++pageNumber)
            page = next
            y = MARGIN
            next.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), Paint().apply { color = Color.WHITE })
            next.drawRect(MARGIN.toFloat(), 28f, (WIDTH - MARGIN).toFloat(), 31f, Paint().apply { color = accent })
        }
        return checkNotNull(page)
    }

    fun finishPage() {
        if (page != null) endPage()
        page = null
    }

    fun text(block: PrenatalReportBlock.Text) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = (report.size + block.level * 3).toFloat()
            color = if (block.level == 0) body else accent
            typeface = Typeface.create(report.font.androidFamily, if (block.level == 0) Typeface.NORMAL else Typeface.BOLD)
        }
        val layout = StaticLayout.Builder.obtain(block.value, 0, block.value.length, paint, WIDTH - 2 * MARGIN)
            .setLineSpacing(3f, 1f)
            .build()
        var line = 0
        while (line < layout.lineCount) {
            val top = layout.getLineTop(line)
            val firstHeight = layout.getLineTop(line + 1) - top
            val keepWithNext = if (block.level > 0 && line == 0) report.size * 3 else 0
            val canvas = pageFor(firstHeight + keepWithNext)
            var end = line + 1
            while (end < layout.lineCount && y + layout.getLineTop(end + 1) - top <= BOTTOM) end++
            val height = layout.getLineTop(end) - top
            canvas.save()
            canvas.clipRect(MARGIN, y, WIDTH - MARGIN, y + height)
            canvas.translate(MARGIN.toFloat(), (y - top).toFloat())
            layout.draw(canvas)
            canvas.restore()
            appendPageText?.invoke(block.value.subSequence(layout.getLineStart(line), layout.getLineEnd(end - 1)))
            y += height
            line = end
        }
        y += if (block.level > 0) 9 else 12
    }

    fun chart(chart: PrenatalChart) {
        val canvas = pageFor(154)
        val left = MARGIN + 40f
        val right = WIDTH - MARGIN - 4f
        val top = y + 10f
        val bottom = top + 104f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.LTGRAY
            strokeWidth = 0.65f
        }
        for (position in listOf(top, (top + bottom) / 2, bottom)) {
            canvas.drawLine(left, position, right, position, paint)
        }
        paint.color = accent
        paint.strokeWidth = 1.5f
        val points = chart.series.single()
        fun x(fraction: Float) = left + fraction * (right - left)
        fun vertical(fraction: Float) = bottom - fraction * (bottom - top)
        points.zipWithNext { start, end ->
            if (start.x != end.x) canvas.drawLine(x(start.x), vertical(start.y), x(end.x), vertical(end.y), paint)
        }
        points.forEach { point -> canvas.drawCircle(x(point.x), vertical(point.y), 2.7f, paint) }
        paint.textSize = report.size.toFloat()
        paint.typeface = Typeface.create(report.font.androidFamily, Typeface.NORMAL)
        paint.color = body
        canvas.drawText(chart.upper, MARGIN.toFloat(), top + 4f, paint)
        canvas.drawText(chart.lower, MARGIN.toFloat(), bottom + 4f, paint)
        canvas.drawText(chart.firstDate, left, bottom + 22f, paint)
        chart.lastDate?.let { canvas.drawText(it, right - paint.measureText(it), bottom + 22f, paint) }
        y += 154
    }
}

private const val WIDTH = 595
private const val HEIGHT = 842
private const val MARGIN = 42
private const val BOTTOM = HEIGHT - MARGIN
