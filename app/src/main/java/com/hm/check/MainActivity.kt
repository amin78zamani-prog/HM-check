package com.hm.check

import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
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

class MainActivity : AppCompatActivity() {

    private lateinit var cardStatus: LinearLayout
    private lateinit var txtStatusTitle: TextView
    private lateinit var txtStatusDesc: TextView
    private lateinit var txtAppTrafficList: TextView
    private lateinit var lineChart: LineChartView
    private lateinit var txtLogHistory: TextView

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var liveUpdateRunnable: Runnable

    private val PERMISSION_REQUEST_CODE = 1001
    private val CHANNEL_ID = "hm_security_channel"

    // ذخیره میزان مصرف قبلی برنامه‌ها برای محاسبه سرعت لحظه‌ای هر اپ
    private val previousAppBytes = mutableMapOf<String, Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndRequestPermissions()

        if (!hasUsageStatsPermission()) {
            Toast.makeText(this, "لطفاً دسترسی پایش مصرف برنامه‌ها را تأیید کنید", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
        }

        createNotificationChannel()

        cardStatus = findViewById(R.id.cardStatus)
        txtStatusTitle = findViewById(R.id.txtStatusTitle)
        txtStatusDesc = findViewById(R.id.txtStatusDesc)
        txtAppTrafficList = findViewById(R.id.txtAppTrafficList)
        lineChart = findViewById(R.id.lineChart)
        txtLogHistory = findViewById(R.id.txtLogHistory)

        appendLog("سیستم تشخیص نشت پنهان و پایش اپلیکیشن‌ها راه‌اندازی شد.")
        startAppSecurityAndTrafficMonitoring()
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun appendLog(message: String) {
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val currentText = txtLogHistory.text.toString()
        txtLogHistory.text = "$currentText[$currentTime] $message\n"
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
            val name = "Security Alerts"
            val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun triggerVibrationAndNotification(appName: String, details: String) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("هشدار امنیتی: نشت پنهان داده!")
            .setContentText("برنامه $appName رفتار مشکوک ثبت کرد.")
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
    }

    private fun startAppSecurityAndTrafficMonitoring() {
        val networkStatsManager = getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val pm = packageManager

        liveUpdateRunnable = object : Runnable {
            override fun run() {
                try {
                    val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    val currentTime = System.currentTimeMillis()
                    val startTime = currentTime - (1000 * 10 * 60) // ۱۰ دقیقه گذشته

                    val stringBuilder = StringBuilder()
                    var systemCompromised = false
                    var maxTotalKb = 0f

                    for (appInfo in packages) {
                        // فقط برنامه‌های غیرسیستمی یا دارای اینترنت را تحلیل می‌کنیم
                        if ((appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 || appInfo.packageName == "com.google.android.youtube") {
                            try {
                                val bucket = networkStatsManager.querySummaryForUser(
                                    ConnectivityManager.TYPE_WIFI,
                                    null,
                                    startTime,
                                    currentTime
                                )
                                val currentBytes = bucket.rxBytes + bucket.txBytes
                                val appName = appInfo.loadLabel(pm).toString()
                                
                                val previousBytes = previousAppBytes[appInfo.packageName] ?: currentBytes
                                val diffBytes = if (currentBytes >= previousBytes) currentBytes - previousBytes else currentBytes
                                previousAppBytes[appInfo.packageName] = currentBytes

                                val speedKb = diffBytes / 1024.0
                                maxTotalKb += speedKb.toFloat()

                                // --- الگوریتم امنیتی تشخیص نشت پنهان (Data Exfiltration Check) ---
                                // اگر برنامه‌ای در پس‌زمینه نرخ انتقال بالایی داشته باشد یا مشکوک تشخیص داده شود
                                val isExfiltrating = speedKb > 250.0 // آستانه حساسیت نشت داده

                                val statusText = if (isExfiltrating) {
                                    systemCompromised = true
                                    "🔴 [ناامن - نشت پنهان فعال]"
                                } else {
                                    "🟢 [امن / نرمال]"
                                }

                                stringBuilder.append("• $appName\n")
                                stringBuilder.append("  مصرف لحظه‌ای: ${String.format(Locale.US, "%.1f", speedKb)} KB\n")
                                stringBuilder.append("  وضعیت امنیت: $statusText\n\n")

                                if (isExfiltrating) {
                                    triggerVibrationAndNotification(appName, "ترافیک غیرعادی پنهان")
                                    appendLog("اخطار امنیتی: نشت پنهان در برنامه $appName با سرعت ${String.format(Locale.US, "%.1f", speedKb)} KB کشف شد!")
                                }

                            } catch (e: Exception) {
                                // صرف‌نظر از پکیج‌های فاقد دسترسی
                            }
                        }
                    }

                    txtAppTrafficList.text = stringBuilder.toString()
                    lineChart.addDataPoint(maxTotalKb.coerceIn(0f, 100f), systemCompromised)

                    if (systemCompromised) {
                        cardStatus.setBackgroundColor(Color.parseColor("#E74C3C"))
                        txtStatusTitle.text = "هشدار بحرانی: نشت پنهان داده!"
                        txtStatusDesc.text = "برخی اپلیکیشن‌ها در حال ارسال مشکوک اطلاعات هستند"
                    } else {
                        cardStatus.setBackgroundColor(Color.parseColor("#27AE60"))
                        txtStatusTitle.text = "وضعیت: کاملاً امن"
                        txtStatusDesc.text = "هیچ ناهنجاری یا نشت پنهانی گزارش نشد"
                    }

                } catch (e: Exception) {
                    appendLog("خطا در پایش اپلیکیشن‌ها: ${e.message}")
                }

                handler.postDelayed(this, 4000) // هر ۴ ثانیه به‌روزرسانی زنده
            }
        }
        handler.post(liveUpdateRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(liveUpdateRunnable)
    }
}
