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

    // دو لیست مجزا برای ذخیره نقاط: یکی برای فعالیت واقعی، یکی برای ریسک فدریت
    private val activityPoints = mutableListOf<Float>(10f, 15f, 12f, 18f, 20f, 16f, 22f)
    private val riskPoints = mutableListOf<Float>(5f, 8f, 6f, 10f, 12f, 9f, 11f)

    // پین خط اول (فعالیت واقعی نود - سبز)
    private val activityPaint = Paint().apply {
        color = 0xFF27AE60.toInt() // سبز
        strokeWidth = 4f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    // پین خط دوم (شاخص ریسک فدریت - قرمز/نارنجی)
    private val riskPaint = Paint().apply {
        color = 0xFFE74C3C.toInt() // قرمز
        strokeWidth = 4f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint().apply {
        color = 0xFF7F8C8D.toInt()
        textSize = 26f
        isAntiAlias = true
    }

    private val gridPaint = Paint().apply {
        color = 0xFFE0E0E0.toInt()
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    // متد جدید برای دریافت همزمان هر دو مقدار در هر چرخه مانیتورینگ
    fun addDataPoints(activityVal: Float, riskVal: Float) {
        if (activityPoints.size > 15) {
            activityPoints.removeAt(0)
            riskPoints.removeAt(0)
        }
        activityPoints.add(activityVal.coerceIn(0f, 100f))
        riskPoints.add(riskVal.coerceIn(0f, 100f))
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (activityPoints.isEmpty()) return

        val paddingLeft = 60f
        val paddingBottom = 20f
        val width = width.toFloat()
        val height = height.toFloat() - paddingBottom
        val maxVal = 100f

        // رسم خطوط راهنمای محور Y
        canvas.drawText("100", 5f, 25f, textPaint)
        canvas.drawLine(paddingLeft, 20f, width, 20f, gridPaint)

        canvas.drawText("50", 10f, height / 2f, textPaint)
        canvas.drawLine(paddingLeft, height / 2f, width, height / 2f, gridPaint)

        canvas.drawText("0", 20f, height, textPaint)
        canvas.drawLine(paddingLeft, height, width, height, gridPaint)

        val chartWidth = width - paddingLeft
        val stepX = chartWidth / (activityPoints.size - 1).coerceAtLeast(1)

        // رسم خط اول (فعالیت سیستم)
        val activityPath = Path()
        for (i in activityPoints.indices) {
            val x = paddingLeft + (i * stepX)
            val y = height - (activityPoints[i] / maxVal) * (height - 20f)
            if (i == 0) activityPath.moveTo(x, y) else activityPath.lineTo(x, y)
        }
        canvas.drawPath(activityPath, activityPaint)

        // رسم خط دوم (شاخص ریسک یادگیری فدریت)
        val riskPath = Path()
        for (i in riskPoints.indices) {
            val x = paddingLeft + (i * stepX)
            val y = height - (riskPoints[i] / maxVal) * (height - 20f)
            if (i == 0) riskPath.moveTo(x, y) else riskPath.lineTo(x, y)
        }
        canvas.drawPath(riskPath, riskPaint)
    }
}
