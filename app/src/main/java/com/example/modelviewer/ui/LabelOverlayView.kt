package com.example.modelviewer.ui



import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.modelviewer.glb.LabelDef


/**
 * Plain-Views replacement for the Compose Canvas label overlay. Stacked
 * exactly on top of its sibling SceneView by ModelContainerView, so anchor
 * points need no extra offset math (spec 2.2).
 */
class LabelOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private data class Entry(val text: String, val anchor: PointF)

    private var entries: List<Entry> = emptyList()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        strokeWidth = 3f
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
    }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 0, 0, 0)
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
    }

    /** Called only when the camera changed or the container was resized —
     *  not on a free-running per-frame timer (see ModelContainerView). */
    fun setLabels(points: List<Pair<LabelDef, PointF>>) {
        entries = points.map { (def, p) -> Entry(def.text, p) }
        invalidate()
    }

    fun clear() {
        entries = emptyList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (entry in entries) {
            val labelX = entry.anchor.x + 24f
            val labelY = entry.anchor.y - 24f

            canvas.drawLine(entry.anchor.x, entry.anchor.y, labelX, labelY, linePaint)
            canvas.drawCircle(entry.anchor.x, entry.anchor.y, 6f, dotPaint)

            val textWidth = textPaint.measureText(entry.text)
            val padding = 8f
            val bgRect = RectF(
                labelX - padding,
                labelY - textPaint.textSize - padding,
                labelX + textWidth + padding,
                labelY + padding,
            )
            canvas.drawRoundRect(bgRect, 8f, 8f, bgPaint)
            canvas.drawText(entry.text, labelX, labelY, textPaint)
        }
    }
}