package com.example.unscroll.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unscroll.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val repository: UserPreferencesRepository) : ViewModel() {

    private val _isUsageStatsPermissionGranted = MutableStateFlow(false)
    val isUsageStatsPermissionGranted: StateFlow<Boolean> = _isUsageStatsPermissionGranted.asStateFlow()

    private val _isOverlayPermissionGranted = MutableStateFlow(false)
    val isOverlayPermissionGranted: StateFlow<Boolean> = _isOverlayPermissionGranted.asStateFlow()

    val youtubeUsage: Flow<Long> = repository.youtubeUsage
    val instagramUsage: Flow<Long> = repository.instagramUsage
    val tiktokUsage: Flow<Long> = repository.tiktokUsage

    val youtubeLimit: Flow<Long> = repository.youtubeLimit
    val instagramLimit: Flow<Long> = repository.instagramLimit
    val tiktokLimit: Flow<Long> = repository.tiktokLimit
    
    val youtubeEnabled: Flow<Boolean> = repository.youtubeEnabled
    val instagramEnabled: Flow<Boolean> = repository.instagramEnabled
    val tiktokEnabled: Flow<Boolean> = repository.tiktokEnabled

    fun checkPermissions(context: Context) {
        _isUsageStatsPermissionGranted.value = hasUsageStatsPermission(context)
        _isOverlayPermissionGranted.value = hasOverlayPermission(context)
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun openUsageStatsSettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    fun openOverlaySettings(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        context.startActivity(intent)
    }
    
    fun updateYoutubeLimit(limitMs: Long) {
        viewModelScope.launch { repository.updateYoutubeLimit(limitMs) }
    }
    
    fun updateInstagramLimit(limitMs: Long) {
        viewModelScope.launch { repository.updateInstagramLimit(limitMs) }
    }
    
    fun updateTiktokLimit(limitMs: Long) {
        viewModelScope.launch { repository.updateTiktokLimit(limitMs) }
    }

    fun updateYoutubeEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateYoutubeEnabled(enabled) }
    }

    fun updateInstagramEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateInstagramEnabled(enabled) }
    }

    fun updateTiktokEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateTiktokEnabled(enabled) }
    }
}
