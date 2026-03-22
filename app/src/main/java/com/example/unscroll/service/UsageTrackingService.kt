package com.example.unscroll.service

import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.unscroll.data.repository.UserPreferencesRepository
import com.example.unscroll.ui.overlay.FocusOverlay
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.Calendar

class UsageTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: UserPreferencesRepository
    private lateinit var usageStatsManager: UsageStatsManager
    private var focusOverlay: FocusOverlay? = null

    private val targetPackages = mapOf(
        "com.google.android.youtube" to "YouTube",
        "com.instagram.android" to "Instagram",
        "com.zhiliaoapp.musically" to "TikTok",
        "com.ss.android.ugc.trill" to "TikTok",
        "com.ss.android.ugc.aweme" to "TikTok",
        "com.zhiliaoapp.musically.go" to "TikTok"
    )

    private val tiktokPackages = setOf(
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.ss.android.ugc.aweme",
        "com.zhiliaoapp.musically.go"
    )

    // Real-time tracking state
    private var currentForegroundPackage: String? = null
    private var sessionStartTime: Long = 0
    private var initialSystemUsage: Long = 0

    override fun onCreate() {
        super.onCreate()
        repository = UserPreferencesRepository(applicationContext)
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        
        focusOverlay = FocusOverlay(
            context = this,
            onDismiss = { goHome() },
            onSnooze = { snooze() }
        )

        startForeground(NOTIFICATION_ID, createNotification())
        startTracking()
    }

    private fun startTracking() {
        serviceScope.launch {
            while (true) {
                if (hasRequiredPermissions()) {
                    val currentTime = System.currentTimeMillis()
                    val detectedPackage = getForegroundPackage()
                    
                    if (detectedPackage != currentForegroundPackage) {
                        // Package changed
                        if (currentForegroundPackage != null && targetPackages.containsKey(currentForegroundPackage)) {
                            // Finalize previous session
                            val sessionDuration = currentTime - sessionStartTime
                            updateUsageInDataStore(currentForegroundPackage!!, initialSystemUsage + sessionDuration)
                        }
                        
                        currentForegroundPackage = detectedPackage
                        if (detectedPackage != null && targetPackages.containsKey(detectedPackage)) {
                            // Start new session
                            sessionStartTime = currentTime
                            initialSystemUsage = getSystemUsage(detectedPackage)
                        }
                    }
                    
                    // Periodic DataStore update for active session (every 5 seconds)
                    if (currentForegroundPackage != null && targetPackages.containsKey(currentForegroundPackage)) {
                        val sessionDuration = currentTime - sessionStartTime
                        if (sessionDuration % 5000 < 1000) {
                            updateUsageInDataStore(currentForegroundPackage!!, initialSystemUsage + sessionDuration)
                        }
                    }

                    checkAndResetDailyUsage()
                    checkBlocking(detectedPackage, currentTime)
                }
                delay(1000)
            }
        }
    }

    private fun getForegroundPackage(): String? {
        val time = System.currentTimeMillis()
        // Check for latest RESUMED event in the last 10 seconds
        val events = usageStatsManager.queryEvents(time - 10000, time)
        val event = UsageEvents.Event()
        var lastResumed: String? = null
        var lastResumedTime = 0L
        var lastPausedTime = 0L
        
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                if (event.timeStamp >= lastResumedTime) {
                    lastResumed = event.packageName
                    lastResumedTime = event.timeStamp
                }
            } else if (event.eventType == UsageEvents.Event.ACTIVITY_PAUSED) {
                if (event.packageName == lastResumed) {
                    lastPausedTime = event.timeStamp
                }
            }
        }

        if (lastResumed != null && lastResumedTime > lastPausedTime) {
            return lastResumed
        }

        // Fallback: Check UsageStats for recent activity
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 5000, time)
        return stats?.maxByOrNull { it.lastTimeUsed }?.let {
            if (time - it.lastTimeUsed < 3000) it.packageName else null
        }
    }

    private fun getSystemUsage(packageName: String): Long {
        val endTime = System.currentTimeMillis()
        val beginTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime)
        return if (tiktokPackages.contains(packageName)) {
            stats?.filter { tiktokPackages.contains(it.packageName) }?.sumOf { it.totalTimeInForeground } ?: 0L
        } else {
            stats?.find { it.packageName == packageName }?.totalTimeInForeground ?: 0L
        }
    }

    private suspend fun updateUsageInDataStore(packageName: String, usageMs: Long) {
        when {
            packageName == "com.google.android.youtube" -> repository.updateYoutubeUsage(usageMs)
            packageName == "com.instagram.android" -> repository.updateInstagramUsage(usageMs)
            tiktokPackages.contains(packageName) -> repository.updateTiktokUsage(usageMs)
        }
    }

    private suspend fun checkBlocking(packageName: String?, currentTime: Long) {
        if (packageName != null && targetPackages.containsKey(packageName)) {
            val isEnabled = getIsEnabledForPackage(packageName)
            if (!isEnabled) {
                withContext(Dispatchers.Main) { focusOverlay?.hide() }
                return
            }

            val limit = getLimitForPackage(packageName)
            val currentUsage = if (packageName == currentForegroundPackage) {
                initialSystemUsage + (currentTime - sessionStartTime)
            } else {
                getSystemUsage(packageName)
            }

            if (currentUsage >= limit) {
                withContext(Dispatchers.Main) {
                    focusOverlay?.show(targetPackages[packageName] ?: "Social Media")
                }
            } else {
                withContext(Dispatchers.Main) { focusOverlay?.hide() }
            }
        } else {
            withContext(Dispatchers.Main) { focusOverlay?.hide() }
        }
    }

    private suspend fun getLimitForPackage(packageName: String): Long {
        return when {
            packageName == "com.google.android.youtube" -> repository.youtubeLimit.first()
            packageName == "com.instagram.android" -> repository.instagramLimit.first()
            tiktokPackages.contains(packageName) -> repository.tiktokLimit.first()
            else -> Long.MAX_VALUE
        }
    }

    private suspend fun getIsEnabledForPackage(packageName: String): Boolean {
        return when {
            packageName == "com.google.android.youtube" -> repository.youtubeEnabled.first()
            packageName == "com.instagram.android" -> repository.instagramEnabled.first()
            tiktokPackages.contains(packageName) -> repository.tiktokEnabled.first()
            else -> false
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED && Settings.canDrawOverlays(this)
    }

    private suspend fun checkAndResetDailyUsage() {
        val lastReset = repository.lastResetTimestamp.first()
        val calendar = Calendar.getInstance()
        val lastResetCalendar = Calendar.getInstance().apply { timeInMillis = lastReset }
        if (calendar.get(Calendar.DAY_OF_YEAR) != lastResetCalendar.get(Calendar.DAY_OF_YEAR) ||
            calendar.get(Calendar.YEAR) != lastResetCalendar.get(Calendar.YEAR)) {
            repository.resetDailyUsage()
            initialSystemUsage = 0
            sessionStartTime = System.currentTimeMillis()
        }
    }

    private fun goHome() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        serviceScope.launch(Dispatchers.Main) { focusOverlay?.hide() }
    }

    private fun snooze() {
        serviceScope.launch {
            val foregroundPackage = currentForegroundPackage
            if (foregroundPackage != null) {
                val currentLimit = getLimitForPackage(foregroundPackage)
                when {
                    foregroundPackage == "com.google.android.youtube" -> repository.updateYoutubeLimit(currentLimit + 5 * 60 * 1000)
                    foregroundPackage == "com.instagram.android" -> repository.updateInstagramLimit(currentLimit + 5 * 60 * 1000)
                    tiktokPackages.contains(foregroundPackage) -> repository.updateTiktokLimit(currentLimit + 5 * 60 * 1000)
                }
            }
            withContext(Dispatchers.Main) { focusOverlay?.hide() }
        }
    }

    private fun createNotification(): Notification {
        val channelId = "usage_tracking_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Usage Tracking Service", NotificationManager.IMPORTANCE_LOW)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Unscroll is Active")
            .setContentText("Monitoring your scrolling habits.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        focusOverlay?.hide()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
