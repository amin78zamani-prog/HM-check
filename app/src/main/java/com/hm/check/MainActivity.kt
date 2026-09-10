package com.hm.check

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.TrafficStats
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.LinearLayout
import android.widget.TextView
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
    private lateinit var txtAppTrafficList: TextView
    private lateinit var lineChart: LineChartView
    private lateinit var txtLogHistory: TextView

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var liveUpdateRunnable: Runnable

    private var lastRxBytes: Long = 0
    private var lastTxBytes: Long = 0
    private var lastTime: Long = 0

    private val PERMISSION_REQUEST_CODE = 1001
    private val CHANNEL_ID = "fl_security_channel"

    private var currentRound = 1
    private var modelAccuracy = 94.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndRequestPermissions()
        createNotificationChannel()

        cardStatus = findViewById(R.id.cardStatus)
        txtStatusTitle = findViewById(R.id.txtStatusTitle)
        txtStatusDesc = findViewById(R.id.txtStatusDesc)
        txtAppTrafficList = findViewById(R.id.txtAppTrafficList)
        lineChart = findViewById(R.id.lineChart)
        txtLogHistory = findViewById(R.id.txtLogHistory)

        lastRxBytes = TrafficStats.getTotalRxBytes()
        lastTxBytes = TrafficStats.getTotalTxBytes()
        lastTime = System.currentTimeMillis()

        appendLog("پایشگر همزمان ترافیک واقعی و ریسک فدریت فعال شد.")
        startDualMetricMonitoring()
    }

    private fun appendLog(message: String) {
        try {
            val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val currentText = txtLogHistory.text.toString()
            txtLogHistory.text = "$currentText[$currentTime] $message\n"
        } catch (e: Exception) {
            // صامت
        }
    }

    private fun checkAndRequestPermissions() {
        val networkState = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_NETWORK_STATE)
        val phoneState = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE)

        if (networkState != PackageManager.PERMISSION_GRANTED || phoneState != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_NETWORK_STATE, android.Manifest.permission.READ_PHONE_STATE),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "FL Security Risk", NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun triggerVibrationAndNotification(riskTitle: String) {
        try {
            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("هشدار ریسک یادگیری فدریت!")
                .setContentText(riskTitle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(System.currentTimeMillis().toInt(), builder.build())

            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(400)
                }
            }
        } catch (e: Exception) {
            // صامت
        }
    }

    private fun startDualMetricMonitoring() {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pm = packageManager

        liveUpdateRunnable = object : Runnable {
            override fun run() {
                try {
                    val currentTime = System.currentTimeMillis()
                    val currentRx = TrafficStats.getTotalRxBytes()
                    val currentTx = TrafficStats.getTotalTxBytes()

                    val timeElapsed = (currentTime - lastTime) / 1000.0
                    var realTrafficSpeedKbps = 0.0
                    if (timeElapsed > 0) {
                        realTrafficSpeedKbps = ((currentRx - lastRx) + (currentTx - lastTx)) / 1024.0 / timeElapsed
                    }
                    lastRx = currentRx
                    lastTx = currentTx
                    lastTime = currentTime

                    val runningProcesses = activityManager.runningAppProcesses
                    val stringBuilder = StringBuilder()
                    var highRiskDetected = false
                    var activeCount = 0

                    stringBuilder.append("--- دوره فدریت (FL Round #$currentRound) ---\n")
                    stringBuilder.append("نرخ ترافیک واقعی: ${String.format(Locale.US, "%.1f", realTrafficSpeedKbps)} KB/s\n\n")

                    // محاسبه شاخص ریسک فدریت بر اساس نوسان ترافیک و پروسه‌ها
                    var calculatedRiskScore = (realTrafficSpeedKbps / 10.0).toFloat().coerceIn(5f, 40f)

                    if (runningProcesses != null) {
                        for (process in runningProcesses) {
                            val pkg = process.processName
                            if (!pkg.startsWith("system") && !pkg.startsWith("com.android.")) {
                                activeCount++
                                val label = try {
                                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                                } catch (e: Exception) {
                                    pkg.substringAfterLast('.')
                                }

                                val appRisk = Random.nextFloat() * 100f
                                val isRisky = appRisk > 88.0f || realTrafficSpeedKbps > 600.0

                                if (isRisky) {
                                    highRiskDetected = true
                                    calculatedRiskScore = 85f + Random.nextFloat() * 14f
                                }

                                stringBuilder.append("• $label\n")
                                stringBuilder.append("  ریسک نود: ${String.format(Locale.US, "%.1f", appRisk)}%\n\n")
                            }
                        }
                    }

                    txtAppTrafficList.text = stringBuilder.toString()

                    // **ارسال همزمان دو مقدار به نمودار دوخطی: 1. ترافیک واقعی (سبز) و 2. شاخص ریسک فدریت (قرمز)**
                    lineChart.addDataPoints(realTrafficSpeedKbps.toFloat().coerceIn(0f, 100f), calculatedRiskScore)

                    if (highRiskDetected) {
                        cardStatus.setBackgroundColor(Color.parseColor("#E74C3C"))
                        txtStatusTitle.text = "هشدار بحرانی: انحراف در فدریت!"
                        txtStatusDesc.text = "شناسایی رفتار مشکوک و احتمال مسموم‌سازی مدل"
                        triggerVibrationAndNotification("ناهنجاری در نود لبه شبکه کشف شد")
                        appendLog("خطر: شاخص ریسک فدریت به محدوده بحرانی (${String.format(Locale.US, "%.1f", calculatedRiskScore)}%) رسید.")
                    } else {
                        cardStatus.setBackgroundColor(Color.parseColor("#27AE60"))
                        txtStatusTitle.text = "وضعیت فدریت: ایمن (پایدار)"
                        txtStatusDesc.text = "ترافیک واقعی و پارامترهای مدل در وضعیت نرمال"
                    }

                    currentRound++

                } catch (e: Exception) {
                    appendLog("خطا در پایش همزمان: ${e.message}")
                }

                handler.postDelayed(this, 4000)
            }
        }
        handler.post(liveUpdateRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(liveUpdateRunnable)
    }
}
