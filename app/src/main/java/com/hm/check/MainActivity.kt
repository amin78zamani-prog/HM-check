package com.hm.check

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var cardStatus: LinearLayout
    private lateinit var txtStatusTitle: TextView
    private lateinit var txtStatusDesc: TextView
    private lateinit var txtSockets: TextView
    private lateinit var txtTraffic: TextView
    private lateinit var lineChart: LineChartView
    private lateinit var btnSimulate: Button

    private var isThreatActive = false
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var liveUpdateRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cardStatus = findViewById(R.id.cardStatus)
        txtStatusTitle = findViewById(R.id.txtStatusTitle)
        txtStatusDesc = findViewById(R.id.txtStatusDesc)
        txtSockets = findViewById(R.id.txtSockets)
        txtTraffic = findViewById(R.id.txtTraffic)
        lineChart = findViewById(R.id.lineChart)
        btnSimulate = findViewById(R.id.btnSimulate)

        btnSimulate.setOnClickListener {
            isThreatActive = !isThreatActive
            updateDashboardState()
        }

        startLiveMonitoring()
    }

    private fun updateDashboardState() {
        if (isThreatActive) {
            cardStatus.setBackgroundColor(Color.parseColor("#E74C3C"))
            txtStatusTitle.text = "هشدار امنیتی!"
            txtStatusDesc.text = "شناسایی ترافیک مشکوک شبکه (ارتباط C&C)"
            btnSimulate.text = "بازنشانی داشبورد"
        } else {
            cardStatus.setBackgroundColor(Color.parseColor("#27AE60"))
            txtStatusTitle.text = "سیستم ایمن"
            txtStatusDesc.text = "هیچ ناهنجاری در شبکه شناسایی نشد"
            btnSimulate.text = "شبیه‌سازی تهدید امنیتی"
        }
    }

    private fun startLiveMonitoring() {
        liveUpdateRunnable = object : Runnable {
            override fun run() {
                if (!isThreatActive) {
                    val randomSockets = Random.nextInt(10, 25)
                    txtSockets.text = "سوکت‌های فعال: $randomSockets"
                    txtTraffic.text = "ترافیک شبکه: عادی (${Random.nextInt(120, 350)} کیلوبایت بر ثانیه)"
                    
                    // ارسال مقدار نرمال به نمودار (بین 15 تا 35)
                    lineChart.addDataPoint(Random.nextFloat() * 20f + 15f, false)
                } else {
                    val highSockets = Random.nextInt(70, 120)
                    txtSockets.text = "سوکت‌های فعال: $highSockets (بحرانی)"
                    txtTraffic.text = "ترافیک شبکه: بحرانی (${Random.nextInt(2, 5)} مگابایت بر ثانیه)"
                    
                    // ارسال مقدار بالا و بحرانی به نمودار (بین 70 تا 95)
                    lineChart.addDataPoint(Random.nextFloat() * 25f + 70f, true)
                }
                handler.postDelayed(this, 2000)
            }
        }
        handler.post(liveUpdateRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(liveUpdateRunnable)
    }
}
