package com.hm.check

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class LineChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dataPoints = mutableListOf<Float>(10f, 12f, 15f, 11f, 14f, 13f, 16f)
    private var isAlertMode = false

    private val linePaint = Paint().apply {
        color = 0xFF27AE60.toInt()
        strokeWidth = 5f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    private val fillPaint = Paint().apply {
        color = 0x2227AE60
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    fun addDataPoint(value: Float, alert: Boolean) {
        isAlertMode = alert
        if (alert) {
            linePaint.color = 0xFFE74C3C.toInt() // قرمز در حالت هشدار
            fillPaint.color = 0x22E74C3C
        } else {
            linePaint.color = 0xFF27AE60.toInt() // سبز در حالت عادی
            fillPaint.color = 0x2227AE60
        }

        if (dataPoints.size > 15) {
            dataPoints.removeAt(0)
        }
        dataPoints.add(value)
        invalidate() // بازترسیم نمودار
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        val maxVal = 100f // سقف مقادیر نمودار

        val stepX = width / (dataPoints.size - 1).coerceAtLeast(1)
        val path = Path()
        val fillPath = Path()

        fillPath.moveTo(0f, height)

        for (i in dataPoints.indices) {
            val x = i * stepX
            val y = height - (dataPoints[i] / maxVal) * height * 0.8f - 10f

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(width, height)
        fillPath.close()

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)
    }
}
