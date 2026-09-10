package com.hm.check

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

    private val PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // اتصال المان‌های رابط کاربری
        cardStatus = findViewById(R.id.cardStatus)
        txtStatusTitle = findViewById(R.id.txtStatusTitle)
        txtStatusDesc = findViewById(R.id.txtStatusDesc)
        txtSockets = findViewById(R.id.txtSockets)
        txtTraffic = findViewById(R.id.txtTraffic)
        lineChart = findViewById(R.id.lineChart)
        btnSimulate = findViewById(R.id.btnSimulate)

        // بررسی و درخواست دسترسی‌ها هنگام اجرای اولیه برنامه
        checkAndRequestPermissions()

        // مدیریت کلیک دکمه شبیه‌سازی برای ارائه در جلسه دفاع
        btnSimulate.setOnClickListener {
            isThreatActive = !isThreatActive
            updateDashboardState()
        }

        // راه‌اندازی شبیه‌ساز به‌روزرسانی زنده مقادیر شبکه
        startLiveMonitoring()
    }

    private fun checkAndRequestPermissions() {
        val networkStatePermission = android.Manifest.permission.ACCESS_NETWORK_STATE
        val phoneStatePermission = android.Manifest.permission.READ_PHONE_STATE

        val hasNetworkPermission = ContextCompat.checkSelfPermission(this, networkStatePermission) == PackageManager.PERMISSION_GRANTED
        val hasPhonePermission = ContextCompat.checkSelfPermission(this, phoneStatePermission) == PackageManager.PERMISSION_GRANTED

        if (!hasNetworkPermission || !hasPhonePermission) {
            // درخواست مجوزها از کاربر به صورت پاپ‌آپ
            ActivityCompat.requestPermissions(
                this,
                arrayOf(networkStatePermission, phoneStatePermission),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    // دریافت نتیجه پاسخ کاربر به کادر دسترسی
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "دسترسی‌های امنیتی شبکه تأیید شد", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "هشدار: برخی دسترسی‌ها رد شد، پایش شبکه ممکن است محدود شود", Toast.LENGTH_LONG).show()
            }
        }
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
                    lineChart.addDataPoint(Random.nextFloat() * 20f + 15f, false)
                } else {
                    val highSockets = Random.nextInt(70, 120)
                    txtSockets.text = "سوکت‌های فعال: $highSockets (بحرانی)"
                    txtTraffic.text = "ترافیک شبکه: بحرانی (${Random.nextInt(2, 5)} مگابایت بر ثانیه)"
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
