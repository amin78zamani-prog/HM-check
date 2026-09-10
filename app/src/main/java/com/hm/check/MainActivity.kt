import android.net.ConnectivityManager
import android.net.NetworkCapabilities
// ... بقیه ایمپورت‌های قبلی

private fun startLiveMonitoring() {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    liveUpdateRunnable = object : Runnable {
        override fun run() {
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

            val currentValue: Float
            if (capabilities != null) {
                // استخراج اطلاعات واقعی پهنای باند (بر حسب کیلوبیت بر ثانیه)
                val downSpeed = capabilities.linkDownstreamBandwidthKbps / 1000 // تبدیل به مگابیت یا واحد مقیاس
                val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                val networkType = if (isWifi) "WiFi" else "Cellular"

                if (!isThreatActive) {
                    txtSockets.text = "نوع اتصال: $networkType (پایدار)"
                    txtTraffic.text = "سرعت دانلود: ${downSpeed.coerceIn(5, 50)} مگابیت بر ثانیه"
                    
                    // تنظیم مقدار نمودار بر اساس سرعت واقعی شبکه
                    currentValue = downSpeed.toFloat().coerceIn(10f, 50f)
                    lineChart.addDataPoint(currentValue, false)
                } else {
                    txtSockets.text = "نوع اتصال: $networkType (مشکوک به ناهنجاری)"
                    txtTraffic.text = "ترافیک غیرعادی شناسایی شد!"
                    
                    currentValue = 85f
                    lineChart.addDataPoint(currentValue, true)
                }
            } else {
                txtSockets.text = "وضعیت: بدون اتصال به شبکه"
                txtTraffic.text = "قطع ارتباط"
                lineChart.addDataPoint(5f, false)
            }

            handler.postDelayed(this, 2000)
        }
    }
    handler.post(liveUpdateRunnable)
}
