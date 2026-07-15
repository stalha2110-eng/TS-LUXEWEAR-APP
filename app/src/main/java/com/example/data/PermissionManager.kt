package com.example.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LuxePermission(
    val id: String,
    val androidPermission: String?,
    val title: String,
    val iconEmoji: String,
    val description: String,
    val purpose: String
) {
    CAMERA(
        id = "camera",
        androidPermission = "android.permission.CAMERA",
        title = "Camera Access",
        iconEmoji = "📸",
        description = "This allows TS LuxeWear to access your camera so you can take product photos, upload instant boutique styling reviews, or capture a beautiful profile picture.",
        purpose = "Capturing boutique photos and instant reviews directly in the app."
    ),
    GALLERY(
        id = "gallery",
        androidPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            "android.permission.READ_MEDIA_IMAGES"
        } else {
            "android.permission.READ_EXTERNAL_STORAGE"
        },
        title = "Gallery & Photos",
        iconEmoji = "🖼️",
        description = "To choose photos from your device library, TS LuxeWear needs access to select images. This allows you to pick beautiful existing boutique styles from your camera roll.",
        purpose = "Uploading existing product catalog drafts, changing profile avatars, or adding photos to reviews."
    ),
    NOTIFICATION(
        id = "notification",
        androidPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            "android.permission.POST_NOTIFICATIONS"
        } else {
            null
        },
        title = "Push Notifications",
        iconEmoji = "🔔",
        description = "Keep up to date on your handloom garment orders, receive alerts for new collections, followed stores uploads, or low stock warnings immediately.",
        purpose = "Receiving instant booking dispatch notices, promotional invites, and restocking alerts."
    ),
    LOCATION(
        id = "location",
        androidPermission = "android.permission.ACCESS_FINE_LOCATION",
        title = "Location Services",
        iconEmoji = "📍",
        description = "Discover luxury multitenant handloom weavers and dynamic boutique showrooms operating with premium designs closest to your current place.",
        purpose = "Locating custom artisans, active delivery hubs, and regional boutique stores near you."
    ),
    STORAGE(
        id = "storage",
        androidPermission = "android.permission.WRITE_EXTERNAL_STORAGE",
        title = "Storage & Downloads",
        iconEmoji = "📁",
        description = "Storing and caching rich designer assets, handloom receipts, or exporting professional invoice PDF documents requires secure storage access.",
        purpose = "Saving digital handloom bills, order invoices, and downloadable product files safely on your device."
    )
}

enum class PermissionState {
    NOT_REQUESTED,
    GRANTED,
    DENIED,
    RE_SUPPORT_MANUAL // Indicates previously denied, user requested again, need to show settings instructions
}

object PermissionManager {
    // Flows holding permission states for dynamic state observation
    private val _permissionStates = MutableStateFlow<Map<LuxePermission, PermissionState>>(
        LuxePermission.values().associateWith { PermissionState.NOT_REQUESTED }
    )
    val permissionStates: StateFlow<Map<LuxePermission, PermissionState>> = _permissionStates.asStateFlow()

    fun isPermissionApproved(permission: LuxePermission): Boolean {
        return _permissionStates.value[permission] == PermissionState.GRANTED
    }

    // Tracking active rational dialogue request
    private val _activeRequest = MutableStateFlow<LuxePermission?>(null)
    val activeRequest: StateFlow<LuxePermission?> = _activeRequest.asStateFlow()

    // Persistent settings fallback guide tracking
    private val _activeSettingsGuide = MutableStateFlow<LuxePermission?>(null)
    val activeSettingsGuide: StateFlow<LuxePermission?> = _activeSettingsGuide.asStateFlow()

    private var currentOnGranted: () -> Unit = {}
    private var currentOnDenied: () -> Unit = {}

    // Tracking if the user has completed the "First Launch Welcome Permission Introduction"
    private val _hasDoneIntro = MutableStateFlow(false)
    val hasDoneIntro: StateFlow<Boolean> = _hasDoneIntro.asStateFlow()

    fun completeIntro() {
        _hasDoneIntro.value = true
    }

    /**
     * Set a permission's state manually or after request flow completes
      */
    fun updatePermission(permission: LuxePermission, state: PermissionState) {
        val current = _permissionStates.value.toMutableMap()
        current[permission] = state
        _permissionStates.value = current
    }

    /**
     * Request a context-based permission. Highly professional wrapper.
     * If already granted, invokes callback immediately.
     * If denied, triggers the device manual settings guide dialogue.
     * Otherwise, shows custom context rationale dialogue first.
     */
    fun requestPermissionContext(
        permission: LuxePermission,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        val currentState = _permissionStates.value[permission] ?: PermissionState.NOT_REQUESTED
        
        if (currentState == PermissionState.GRANTED) {
            onGranted()
            return
        }
        
        currentOnGranted = onGranted
        currentOnDenied = onDenied

        if (currentState == PermissionState.DENIED || currentState == PermissionState.RE_SUPPORT_MANUAL) {
            _activeSettingsGuide.value = permission
            onDenied()
        } else {
            _activeRequest.value = permission
        }
    }

    /**
     * Handle user's selection in the custom rationale dialogue
     */
    fun handleUserDecision(accepted: Boolean) {
        val permission = _activeRequest.value ?: return
        _activeRequest.value = null
        if (accepted) {
            updatePermission(permission, PermissionState.GRANTED)
            currentOnGranted()
        } else {
            updatePermission(permission, PermissionState.DENIED)
            currentOnDenied()
        }
    }

    fun dismissSettingsGuide() {
        _activeSettingsGuide.value = null
    }

    fun dismissActiveRequest() {
        val permission = _activeRequest.value
        _activeRequest.value = null
        if (permission != null) {
            updatePermission(permission, PermissionState.DENIED)
            currentOnDenied()
        }
    }

    /**
     * Verify state natively utilizing the Android context
     */
    fun verifyStateWithContext(context: Context) {
        val current = _permissionStates.value.toMutableMap()
        LuxePermission.values().forEach { permission ->
            val androidPerm = permission.androidPermission
            if (androidPerm != null) {
                val hasPermission = ContextCompat.checkSelfPermission(context, androidPerm) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    current[permission] = PermissionState.GRANTED
                } else if (current[permission] == PermissionState.GRANTED) {
                    // Reset to not requested if user withdrew natively in background
                    current[permission] = PermissionState.NOT_REQUESTED
                }
            }
        }
        _permissionStates.value = current
    }

    /**
     * Check if a specific permission is currently granted
     */
    fun isGranted(permission: LuxePermission): Boolean {
        return _permissionStates.value[permission] == PermissionState.GRANTED
    }

    /**
     * Check if a permission is currently denied
     */
    fun isDenied(permission: LuxePermission): Boolean {
        return _permissionStates.value[permission] == PermissionState.DENIED
    }

    /**
     * Transparent Privacy Policy log
     */
    fun getPrivacyRationale(permission: LuxePermission): String {
        return "TS LuxeWear is committed to strict privacy. Data for ${permission.title} is processed fully under local security regulations and is never shared with third parties."
    }

    fun isFirstOpen(context: Context): Boolean {
        val prefs = context.getSharedPreferences("luxe_permissions_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("is_first_open_luxe", true)
    }

    fun markFirstOpenOnboarded(context: Context) {
        val prefs = context.getSharedPreferences("luxe_permissions_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_first_open_luxe", false).apply()
    }

    fun grantPermission(permission: LuxePermission) {
        updatePermission(permission, PermissionState.GRANTED)
    }

    fun onRationaleDecision(approved: Boolean) {
        handleUserDecision(approved)
    }
}
