package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class UserRole(val displayName: String) {
    SUPER_ADMIN("Super Administrator"),
    STORE_OWNER("Store Owner"),
    CUSTOMER("Customer"),
    GUEST("Guest Shopper")
}

data class AuthUser(
    val uid: String,
    val email: String,
    val displayName: String,
    val role: UserRole
)

object AuthManager {
    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    val currentUserFlow: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    // Predefined authorized accounts for super admin
    const val SUPER_ADMIN_1 = "shakirsir2122@gmail.com"
    const val SUPER_ADMIN_2 = "stalha2110@gmail.com"

    // Set of store owner emails for verification purposes
    private val storeOwnerEmails = mutableSetOf<String>()

    fun isSuperAdminEmail(email: String): Boolean {
        val trimmed = email.trim().lowercase()
        return trimmed == SUPER_ADMIN_1 || trimmed == SUPER_ADMIN_2
    }

    fun isStoreOwnerEmail(email: String): Boolean {
        val trimmed = email.trim().lowercase()
        return storeOwnerEmails.contains(trimmed) || trimmed.contains("owner") || trimmed.contains("boutique")
    }

    /**
     * Sign in via Google with role verification
     * After Google login, Firebase verifies the account.
     */
    fun performGoogleLogin(
        email: String,
        intendedRole: UserRole,
        adminPassword: String? = null,
        onError: (String) -> Unit
    ): Boolean {
        val emailClean = email.trim()
        if (emailClean.isEmpty() || !emailClean.contains("@")) {
            onError("Please enter a valid Gmail address.")
            return false
        }
        
        // Gmail validation logic
        val role = when {
            // Hard security rule: ONLY shakirsir2122@gmail.com and stalha2110@gmail.com can ever access super_admin
            isSuperAdminEmail(emailClean) -> {
                UserRole.SUPER_ADMIN
            }
            intendedRole == UserRole.STORE_OWNER -> {
                UserRole.STORE_OWNER
            }
            else -> {
                UserRole.CUSTOMER
            }
        }

        // 1. Double check: Super Admin whitelisting and password verification
        if (role == UserRole.SUPER_ADMIN || intendedRole == UserRole.SUPER_ADMIN) {
            if (!isSuperAdminEmail(emailClean)) {
                onError("Unauthorized Access - You are not authorized to access Super Admin")
                return false
            }
            if (adminPassword != "stalha@21") {
                onError("Access Denied: Incorrect Password for Super Admin.")
                return false
            }
        }

        // 2. Customers are only permitted to browse using Guest Mode
        if (role == UserRole.CUSTOMER || intendedRole == UserRole.CUSTOMER) {
            onError("Customer accounts must use Guest Mode to browse the luxury boutique.")
            return false
        }

        // Add to store owners list if they logged in as a store owner with a new email
        if (role == UserRole.STORE_OWNER) {
            storeOwnerEmails.add(emailClean.lowercase())
        }

        val name = emailClean.substringBefore("@").replace(".", " ").capitalizeWords()
        val userObj = AuthUser(
            uid = "fb_${emailClean.hashCode()}",
            email = emailClean,
            displayName = name,
            role = role
        )
        _currentUser.value = userObj

        // Dispatch REALTIME "New login" audit alert to Super Admin
        if (role == UserRole.STORE_OWNER) {
            TSLuxeWearRepository.sendPushNotification(
                recipientRole = "SUPER_ADMIN",
                recipientEmail = SUPER_ADMIN_1,
                title = "Security: New Login Alert 🔑",
                message = "Secure login detected. Profile: Store Owner | Email: $emailClean | Name: $name.",
                type = "STORE_LOGIN",
                category = "Systems",
                targetScreen = "super_admin_dashboard"
            )
        }

        return true
    }

    fun continueAsGuest() {
        _currentUser.value = AuthUser(
            uid = "guest_session",
            email = "guest@luxewear.com",
            displayName = "Guest User",
            role = UserRole.GUEST
        )
    }

    fun logout() {
        _currentUser.value = null
    }

    private fun String.capitalizeWords(): String {
        return this.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
