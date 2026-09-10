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

    private val textPaint = Paint().apply {
        color = 0xFF7F8C8D.toInt()
        textSize = 28f
        isAntiAlias = true
    }

    private val gridPaint = Paint().apply {
        color = 0xFFE0E0E0.toInt()
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    fun addDataPoint(value: Float, alert: Boolean) {
        isAlertMode = alert
        if (alert) {
            linePaint.color = 0xFFE74C3C.toInt()
            fillPaint.color = 0x22E74C3C
        } else {
            linePaint.color = 0xFF27AE60.toInt()
            fillPaint.color = 0x2227AE60
        }

        if (dataPoints.size > 15) {
            dataPoints.removeAt(0)
        }
        dataPoints.add(value)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.isEmpty()) return

        val paddingLeft = 70f
        val paddingBottom = 20f
        val width = width.toFloat()
        val height = height.toFloat() - paddingBottom
        val maxVal = 100f

        // رسم خطوط راهنمای مقادیر در کنار نمودار (محور Y)
        canvas.drawText("100", 5f, 30f, textPaint)
        canvas.drawLine(paddingLeft, 20f, width, 20f, gridPaint)

        canvas.drawText("50", 15f, height / 2f, textPaint)
        canvas.drawLine(paddingLeft, height / 2f, width, height / 2f, gridPaint)

        canvas.drawText("0", 25f, height, textPaint)
        canvas.drawLine(paddingLeft, height, width, height, gridPaint)

        val chartWidth = width - paddingLeft
        val stepX = chartWidth / (dataPoints.size - 1).coerceAtLeast(1)
        
        val path = Path()
        val fillPath = Path()

        fillPath.moveTo(paddingLeft, height)

        for (i in dataPoints.indices) {
            val x = paddingLeft + (i * stepX)
            val y = height - (dataPoints[i] / maxVal) * (height - 20f)

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
