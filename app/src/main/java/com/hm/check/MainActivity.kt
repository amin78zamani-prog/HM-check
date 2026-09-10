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
    private lateinit var btnSimulate: Button

    private var isThreatActive = false
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var liveUpdateRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // اتصال المان‌های رابط کاربری
        cardStatus = findViewById(R.id.cardStatus)
        txtStatusTitle = findViewById(R.id.txtStatusTitle)
        txtStatusDesc = findViewById(R.id.txtStatusDesc)
        txtSockets = findViewById(R.id.txtSockets)
        txtTraffic = findViewById(R.id.txtTraffic)
        btnSimulate = findViewById(R.id.btnSimulate)

        // مدیریت کلیک دکمه شبیه‌سازی برای ارائه در جلسه دفاع
        btnSimulate.setOnClickListener {
            isThreatActive = !isThreatActive
            updateDashboardState()
        }

        // راه‌اندازی شبیه‌ساز به‌روزرسانی زنده مقادیر شبکه
        startLiveMonitoring()
    }

    private fun updateDashboardState() {
        if (isThreatActive) {
            // حالت خطر / ناهنجاری شبکه
            cardStatus.setBackgroundColor(Color.parseColor("#E74C3C")) // رنگ قرمز
            txtStatusTitle.text = "SECURITY ALERT!"
            txtStatusDesc.text = "Anomalous network traffic detected (C&C Beacon)"
            btnSimulate.text = "Reset Dashboard"
        } else {
            // حالت امن و عادی
            cardStatus.setBackgroundColor(Color.parseColor("#27AE60")) // رنگ سبز
            txtStatusTitle.text = "SYSTEM SECURE"
            txtStatusDesc.text = "No network anomalies detected"
            btnSimulate.text = "Simulate Security Threat"
        }
    }

    private fun startLiveMonitoring() {
        liveUpdateRunnable = object : Runnable {
            override fun run() {
                if (!isThreatActive) {
                    // تولید مقادیر تصادفی نرمال برای سوکت‌ها و پهنای باند
                    val randomSockets = Random.nextInt(10, 25)
                    txtSockets.text = "Active Sockets: $randomSockets"
                    txtTraffic.text = "Network Bandwidth: Normal (${Random.nextInt(120, 350)} KB/s)"
                } else {
                    // مقادیر بالا و مشکوک در حالت تهدید
                    val highSockets = Random.nextInt(70, 120)
                    txtSockets.text = "Active Sockets: $highSockets (HIGH)"
                    txtTraffic.text = "Network Bandwidth: Critical (${Random.nextInt(2, 5)} MB/s)"
                }
                // تکرار هر ۲ ثانیه برای ایجاد حس داشبورد زنده
                handler.postDelayed(this, 2000)
            }
        }
        handler.post(liveUpdateRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        // جلوگیری از نشت حافظه
        handler.removeCallbacks(liveUpdateRunnable)
    }
}
