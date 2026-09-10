package com.hm.check

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var cardStatus: LinearLayout
    private lateinit var txtStatusTitle: TextView
    private lateinit var txtStatusDesc: TextView
    private lateinit var txtSockets: TextView
    private lateinit var txtTraffic: TextView
    private lateinit var lineChart: LineChartView
    private lateinit var txtLogHistory: TextView
    private lateinit var btnSimulate: Button

    private var isThreatActive = false
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var liveUpdateRunnable: Runnable

    private val PERMISSION_REQUEST_CODE = 1001
    private val CHANNEL_ID = "hm_security_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndRequestPermissions()
        createNotificationChannel()

        cardStatus = findViewById(R.id.cardStatus)
        txtStatusTitle = findViewById(R.id.txtStatusTitle)
        txtStatusDesc = findViewById(R.id.txtStatusDesc)
        txtSockets = findViewById(R.id.txtSockets)
        txtTraffic = findViewById(R.id.txtTraffic)
        lineChart = findViewById(R.id.lineChart)
        txtLogHistory = findViewById(R.id.txtLogHistory)
        btnSimulate = findViewById(R.id.btnSimulate)

        appendLog("سیستم پایشگر امنیتی با موفقیت راه‌اندازی شد.")

        btnSimulate.setOnClickListener {
            isThreatActive = !isThreatActive
            updateDashboardState()
            
            if (isThreatActive) {
                sendSecurityNotification("هشدار امنیتی بحرانی!", "ترافیک مشکوک و غیرعادی در شبکه شناسایی شد.")
                appendLog("هشدار: ترافیک مشکوک C&C شناسایی و ثبت شد!")
            } else {
                appendLog("وضعیت سیستم به حالت نرمال و امن بازگشت.")
            }
        }

        startLiveMonitoring()
    }

    private fun appendLog(message: String) {
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val currentText = txtLogHistory.text.toString()
        txtLogHistory.text = "$currentText[$currentTime] $message\n"
    }

    private fun checkAndRequestPermissions() {
        val networkStatePermission = android.Manifest.permission.ACCESS_NETWORK_STATE
        val phoneStatePermission = android.Manifest.permission.READ_PHONE_STATE

        val hasNetworkPermission = ContextCompat.checkSelfPermission(this, networkStatePermission) == PackageManager.PERMISSION_GRANTED
        val hasPhonePermission = ContextCompat.checkSelfPermission(this, phoneStatePermission) == PackageManager.PERMISSION_GRANTED

        if (!hasNetworkPermission || !hasPhonePermission) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(networkStatePermission, phoneStatePermission),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Security Alerts"
            val descriptionText = "Notifications for network security threats"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendSecurityNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, builder.build())
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                appendLog("دسترسی‌های سیستمی و شبکه تأیید شد.")
            } else {
                appendLog("اخطار: برخی دسترسی‌های سیستمی رد شد.")
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
                val currentValue: Float
                if (!isThreatActive) {
                    val randomSockets = Random.nextInt(10, 25)
                    txtSockets.text = "سوکت‌های فعال: $randomSockets"
                    txtTraffic.text = "ترافیک شبکه: عادی (${Random.nextInt(120, 350)} کیلوبایت بر ثانیه)"
                    currentValue = Random.nextFloat() * 20f + 15f
                    lineChart.addDataPoint(currentValue, false)
                } else {
                    val highSockets = Random.nextInt(70, 120)
                    txtSockets.text = "سوکت‌های فعال: $highSockets (بحرانی)"
                    txtTraffic.text = "ترافیک شبکه: بحرانی (${Random.nextInt(2, 5)} مگابایت بر ثانیه)"
                    currentValue = Random.nextFloat() * 25f + 70f
                    lineChart.addDataPoint(currentValue, true)
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
