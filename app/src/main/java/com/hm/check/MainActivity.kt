package com.hm.check

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
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

class MainActivity : AppCompatActivity() {

    private lateinit var cardStatus: LinearLayout
    private lateinit var txtStatusTitle: TextView
    private lateinit var txtStatusDesc: TextView
    private lateinit var txtSockets: TextView
    private lateinit var txtTraffic: TextView
    private lateinit var lineChart: LineChartView
    private lateinit var txtLogHistory: TextView
    private lateinit var btnSimulate: Button // این دکمه را برای کنترل دستی یا تست وضعیت بحرانی واقعی حفظ می‌کنیم

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var liveUpdateRunnable: Runnable

    private var lastRxBytes: Long = 0
    private var lastTxBytes: Long = 0
    private var lastTime: Long = 0

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

        // مخفی کردن یا تغییر کاربری دکمه (دیگر شبیه‌سازی نیست، پایش واقعی است)
        btnSimulate.visibility = View.GONE

        // مقداردهی اولیه آمار ترافیک
        lastRxBytes = TrafficStats.getTotalRxBytes()
        lastTxBytes = TrafficStats.getTotalTxBytes()
        lastTime = System.currentTimeMillis()

        appendLog("سیستم مانیتورینگ واقعی ترافیک شبکه فعال شد.")

        startRealTrafficMonitoring()
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
            }
        }
    }

    private fun startRealTrafficMonitoring() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        liveUpdateRunnable = object : Runnable {
            override fun run() {
                val currentTime = System.currentTimeMillis()
                val currentRxBytes = TrafficStats.getTotalRxBytes()
                val currentTxBytes = TrafficStats.getTotalTxBytes()

                val timeElapsed = (currentTime - lastTime) / 1000.0 // بر حسب ثانیه
                if (timeElapsed > 0) {
                    val rxSpeed = (currentRxBytes - lastRxBytes) / timeElapsed // بایت بر ثانیه
                    val txSpeed = (currentTxBytes - lastTxBytes) / timeElapsed
                    val totalSpeedKbps = (rxSpeed + txSpeed) / 1024.0 // کیلوبایت بر ثانیه

                    lastRxBytes = currentRxBytes
                    lastTxBytes = currentTxBytes
                    lastTime = currentTime

                    // بررسی وضعیت اتصال واقعی
                    val activeNetwork = connectivityManager.activeNetwork
                    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                    
                    if (capabilities != null) {
                        val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        val netType = if (isWifi) "WiFi" else "داده همراه (Cellular)"
                        
                        txtSockets.text = "نوع اتصال: $netType"
                        txtTraffic.text = "مصرف لحظه‌ای: ${String.format(Locale.US, "%.1f", totalSpeedKbps)} کیلوبایت/ثانیه"

                        // تحلیل امنیتی واقعی: اگر مصرف از آستانه مشخصی (مثلا ۵۰۰ کیلوبایت بر ثانیه) فراتر رفت، هشدار بده
                        val chartValue = totalSpeedKbps.toFloat().coerceIn(0f, 100f)
                        
                        if (totalSpeedKbps > 500.0) {
                            // وضعیت بحرانی / ترافیک سنگین واقعی
                            cardStatus.setBackgroundColor(Color.parseColor("#E74C3C"))
                            txtStatusTitle.text = "هشدار ترافیک بالا!"
                            txtStatusDesc.text = "مصرف غیرعادی پهنای باند شناسایی شد"
                            
                            lineChart.addDataPoint(chartValue, true)
                            sendSecurityNotification("هشدار پهنای باند", "مصرف شبکه دستگاه از حد مجاز فراتر رفت.")
                            appendLog("هشدار: ترافیک سنگین شبکه (${String.format(Locale.US, "%.1f", totalSpeedKbps)} KB/s) ثبت شد.")
                        } else {
                            // وضعیت امن و عادی
                            cardStatus.setBackgroundColor(Color.parseColor("#27AE60"))
                            txtStatusTitle.text = "سیستم ایمن"
                            txtStatusDesc.text = "وضعیت مصرف شبکه نرمال است"
                            
                            lineChart.addDataPoint(chartValue, false)
                        }
                    } else {
                        txtSockets.text = "وضعیت: بدون اتصال به اینترنت"
                        txtTraffic.text = "مصرف: صفر"
                        lineChart.addDataPoint(0f, false)
                    }
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
