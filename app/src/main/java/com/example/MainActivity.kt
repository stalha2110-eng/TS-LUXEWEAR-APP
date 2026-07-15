package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.model.Product
import com.example.ui.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    // Keep track of incoming deep link path
    private val deepLinkPathState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Handle incoming intent details for deep links (e.g. myapp.com/admin)
        handleDeepLink(intent)

        setContent {
            MyApplicationTheme {
                MainAppShell(
                    initialDeepLink = deepLinkPathState.value,
                    onDeepLinkConsumed = { deepLinkPathState.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data != null) {
            val path = data.path
            if (path != null) {
                deepLinkPathState.value = path
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppShell(
    initialDeepLink: String?,
    onDeepLinkConsumed: () -> Unit
) {
    val repository = TSLuxeWearRepository
    val context = LocalContext.current
    
    // Initialize Database on Startup
    LaunchedEffect(Unit) {
        repository.initDatabase(context)
        com.example.data.FirebaseBackend.initialize(context)
    }

    // Auth and Navigation states
    val currentUser by AuthManager.currentUserFlow.collectAsState()
    
    // Notification list and count tracking
    var showNotificationCenter by remember { mutableStateOf(false) }
    val notificationsList by if (currentUser != null) {
        repository.getNotificationsFlow(currentUser!!.role.name, currentUser!!.email)
            ?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    } else {
        remember { mutableStateOf(emptyList()) }
    }
    val unreadCount = notificationsList.count { it.isRead == 0 }

    // Current Active Screen Route. Establishes a clean, secure navigation state
    // Routes: "login", "customer_home", "store_owner_dashboard", "super_admin_dashboard"
    var currentRoute by remember { mutableStateOf("login") }
    var showNavDrawer by remember { mutableStateOf(false) }
    
    // Selected view helpers inside customer screen
    var selectedProductForDetail by remember { mutableStateOf<Product?>(null) }
    var activeVirtualTryProduct by remember { mutableStateOf<Product?>(null) }
    
    // Interactive testing parameters
    var simulatedUrlInput by remember { mutableStateOf("myapp.com/login") }
    var securityIncidentMessage by remember { mutableStateOf<String?>(null) }
    var showRedirectionOverlay by remember { mutableStateOf(false) }
    var targetRedirectionRoute by remember { mutableStateOf("") }
    
    // Trigger deep links reactively on start/updated flow
    LaunchedEffect(initialDeepLink) {
        if (initialDeepLink != null) {
            onDeepLinkConsumed()
            handleUrlNavigationInput(initialDeepLink, currentUser, 
                onBlocked = { msg, fallback -> 
                    securityIncidentMessage = msg
                    currentRoute = fallback
                    simulatedUrlInput = "myapp.com/$fallback"
                },
                onGranted = { route ->
                    currentRoute = route
                    if (initialDeepLink.contains("productId")) {
                        simulatedUrlInput = "myapp.com" + if (initialDeepLink.startsWith("/")) initialDeepLink else "/$initialDeepLink"
                    } else {
                        simulatedUrlInput = "myapp.com/admin"
                        Toast.makeText(context, "Deep link authorized: Admin Mode", Toast.LENGTH_SHORT).show()
                    }
                },
                onProductShared = { prod ->
                    selectedProductForDetail = prod
                    Toast.makeText(context, "Sensation Shared Lookbook opened! View ${prod.name}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    // Sync simulated address bar with route changes
    LaunchedEffect(currentRoute) {
        simulatedUrlInput = "myapp.com/$currentRoute"
    }

    // SECURITY COMPASS: Reactive Side-Effect Guard
    // Block unauthorized users from ever remaining on "super_admin_dashboard" 
    // even if state is manipulated or hardcoded.
    LaunchedEffect(currentRoute, currentUser) {
        if (currentRoute == "super_admin_dashboard") {
            val user = currentUser
            if (user == null || !AuthManager.isSuperAdminEmail(user.email)) {
                // Critical Security Access Violation! Trigger immediate block and redirect
                val blockedEmail = user?.email ?: "Anonymous Guest"
                securityIncidentMessage = "CRITICAL SECURITY BLOCKED:\nAttempted unauthorized access to Admin Dashboard by profile: $blockedEmail.\n\nRedirecting to safe zone."
                
                // Perform forced redirect
                if (user == null) {
                    currentRoute = "login"
                } else if (user.role == UserRole.STORE_OWNER) {
                    currentRoute = "store_owner_dashboard"
                } else {
                    currentRoute = "customer_home"
                }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isDesktop = maxWidth >= 600.dp

        MobileNavigationDrawer(
            isOpen = showNavDrawer,
            onDismiss = { showNavDrawer = false },
            currentRoute = currentRoute,
            currentUser = currentUser,
            onNavigate = { currentRoute = it }
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                AppVersionFooter(currentRoute = currentRoute)
            },
            topBar = {
                Column {
                    // Safe notch spacer
                    Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                    
                    // Premium Interactive Sandbox / Browser Address bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF141416), // Deep velvet obsidian
                        tonalElevation = 8.dp,
                        border = BorderStroke(1.dp, LuxeGold.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Header title block
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (currentUser != null && !isDesktop) {
                                        IconButton(
                                            onClick = { showNavDrawer = true },
                                            modifier = Modifier.size(34.dp).testTag("hamburger_nav_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Menu,
                                                contentDescription = "Open Navigation Menu",
                                                tint = LuxeGold,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .border(1.2.dp, LuxeGold, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.logo_luxe),
                                            contentDescription = "Brand Logo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Text(
                                        text = "TS LUXEWEAR GATEWAY",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.sp
                                    )
                                }
                                
                                // User Info capsule and Notification button
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Bell Button with Badge Count
                                    IconButton(
                                        onClick = { showNotificationCenter = true },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .testTag("notification_center_bell_btn")
                                    ) {
                                        BadgedBox(
                                            badge = {
                                                if (unreadCount > 0) {
                                                    Badge(
                                                        containerColor = Color.Red,
                                                        contentColor = Color.White
                                                    ) {
                                                        Text("$unreadCount", fontSize = 8.sp)
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = "Open In-App Notification Center",
                                                tint = if (unreadCount > 0) LuxeGold else Color.LightGray.copy(alpha = 0.8f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(if (currentUser != null) Color(0xFF25D366) else Color.Gray, CircleShape)
                                        )
                                        Text(
                                            text = if (currentUser != null) "${currentUser?.displayName} (${currentUser?.role?.displayName})" else "Not Authenticated",
                                            color = if (currentUser != null) Color.LightGray else Color.Gray,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (currentUser != null) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFE53935))
                                                .clickable {
                                                    AuthManager.logout()
                                                    currentRoute = "login"
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("Logout", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            LuxeErrorBoundary(
                repository = repository,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(LuxeCream)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxSize()) {
                    if (currentUser != null && isDesktop) {
                        NavigationSideRail(
                            currentRoute = currentRoute,
                            currentUser = currentUser,
                            onNavigate = { currentRoute = it }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        // Main Router Content Screen with beautiful transitions
                    AnimatedContent(
                        targetState = currentRoute,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                        },
                        label = "ScreenNavigator"
                    ) { routeState ->
                when (routeState) {
                    "login" -> {
                        LoginScreen(
                            currentRoute = "login",
                            onNavigateToRoute = { currentRoute = it },
                            onLoginSuccess = { email, role ->
                                // Secure login redirection mapping:
                                when {
                                    AuthManager.isSuperAdminEmail(email) -> {
                                        currentRoute = "super_admin_dashboard"
                                        Toast.makeText(context, "Authenticated as Super Admin: $email", Toast.LENGTH_LONG).show()
                                    }
                                    role == UserRole.STORE_OWNER -> {
                                        currentRoute = "store_owner_dashboard"
                                        Toast.makeText(context, "Authenticated as Store Owner: $email", Toast.LENGTH_LONG).show()
                                    }
                                    else -> {
                                        currentRoute = "customer_home"
                                        Toast.makeText(context, "Welcome to Shop: $email", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        )
                    }
                    "customer_home" -> {
                        val currentDetail = selectedProductForDetail
                        
                        // Render Active Virtual Try On overlay if running
                        if (activeVirtualTryProduct != null) {
                            VirtualTryOnDialog(
                                product = activeVirtualTryProduct!!,
                                onDismiss = { activeVirtualTryProduct = null },
                                onNavigateToWhatsApp = { prod, msg ->
                                    Toast.makeText(context, "Redirecting to WhatsApp to order ${prod.name}! 🚀", Toast.LENGTH_LONG).show()
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(msg))
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "WhatsApp secure draft simulated! 📝", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }

                        if (currentDetail != null) {
                            ProductDetailSheet(
                                product = currentDetail,
                                repository = repository,
                                onBack = { selectedProductForDetail = null },
                                onVirtualTryClick = {
                                    activeVirtualTryProduct = currentDetail
                                },
                                onPlaceOrder = { order -> }
                            )
                        } else {
                            CustomerDashboardScreen(
                                repository = repository,
                                onProductClick = { prod ->
                                    repository.addProductToRecentlyViewed(prod.id)
                                    selectedProductForDetail = prod
                                },
                                onVirtualTryClick = { prod ->
                                    activeVirtualTryProduct = prod
                                }
                            )
                        }
                    }
                    "store_owner_dashboard" -> {
                        StoreOwnerDashboardScreen(repository = repository)
                    }
                    "super_admin_dashboard" -> {
                        // Secure rendering verify
                        val user = currentUser
                        if (user != null && AuthManager.isSuperAdminEmail(user.email)) {
                            SuperAdminDashboardScreen(repository = repository)
                        } else {
                            // Empty secure screen spacer fallback
                            Box(modifier = Modifier.fillMaxSize().background(Color.White))
                        }
                    }
                }
            } // closes AnimatedContent
                    } // closes weighted Box
                } // closes Row

                // Real-time FCM sliding notifications banner
            RealtimeNotificationBanner(
                repository = repository,
                onNavigate = { route ->
                    currentRoute = route
                }
            )

            // Full Dialog notification center
            if (showNotificationCenter) {
                NotificationCenterDialog(
                    repository = repository,
                    onDismiss = { showNotificationCenter = false },
                    onNavigate = { route ->
                        currentRoute = route
                    }
                )
            }

            // Initial first-launch onboarding permissions sheet (WelcomeOnboardSheet)
            var showWelcomeOnboard by remember { mutableStateOf(false) }
            
            // Check if application is newly launched to onboard standard essential permissions
            LaunchedEffect(Unit) {
                val isFirstLaunch = com.example.data.PermissionManager.isFirstOpen(context)
                if (isFirstLaunch) {
                    showWelcomeOnboard = true
                }
            }

            if (showWelcomeOnboard) {
                com.example.ui.WelcomePermissionsOnboardSheet(
                    onDismiss = {
                        com.example.data.PermissionManager.markFirstOpenOnboarded(context)
                        showWelcomeOnboard = false
                    },
                    onNotificationGrant = {
                        // Dynamically accept the essential/common permissions
                        com.example.data.PermissionManager.grantPermission(com.example.data.LuxePermission.NOTIFICATION)
                        com.example.data.PermissionManager.markFirstOpenOnboarded(context)
                        showWelcomeOnboard = false
                        Toast.makeText(context, "Essential Notification alert channels enabled successfully!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Real-time reactive rationale or manual settings redirection prompts
            val activeRequest by com.example.data.PermissionManager.activeRequest.collectAsState()
            val activeSettingsGuide by com.example.data.PermissionManager.activeSettingsGuide.collectAsState()

            activeRequest?.let { req ->
                com.example.ui.PermissionRationaleDialog(
                    permission = req,
                    onDismiss = {
                        com.example.data.PermissionManager.onRationaleDecision(approved = false)
                    },
                    onConfirm = {
                        com.example.data.PermissionManager.onRationaleDecision(approved = true)
                    }
                )
            }

            activeSettingsGuide?.let { guide ->
                com.example.ui.PermissionSettingsInstructionDialog(
                    permission = guide,
                    onDismiss = {
                        com.example.data.PermissionManager.dismissSettingsGuide()
                    }
                )
            }

            // Beautiful Security Warning Incident Modal to show unauthorized blockage and redirect clearly
            if (securityIncidentMessage != null) {
                Dialog(onDismissRequest = { securityIncidentMessage = null }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .testTag("security_incident_alert"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E0C0E)), // Dark blood wine
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, Color(0xFFEF5350))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.GppBad,
                                contentDescription = "Security Alert",
                                tint = Color(0xFFEF5350),
                                modifier = Modifier.size(54.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            Text(
                                text = "SECURITY PROTOCOL VIOLATION",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Text(
                                text = securityIncidentMessage!!,
                                color = Color(0xFFFFCDD2),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Button(
                                onClick = { securityIncidentMessage = null },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Acknowledge & Sync", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            } // Close the securityIncidentMessage != null if block
            
            // Floating simulated packet drop fault switch for interactive testing
            SimulatedNetworkFaultOverlay(
                repository = repository,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}
}
}

/**
 * Custom URL router input handling
 */
/**
 * Custom URL router input handling with robust query and path parser for Phase 8 shared product routing
 */
fun handleUrlNavigationInput(
    urlInput: String,
    currentUser: AuthUser?,
    onBlocked: (errorMessage: String, fallbackRoute: String) -> Unit,
    onGranted: (targetRoute: String) -> Unit,
    onProductShared: ((Product) -> Unit)? = null
) {
    val cleanUrl = urlInput.trim().replace("http://", "").replace("https://", "").removePrefix("myapp.com").removePrefix("/")
    
    // Phase 8 Product Sharing link parsing
    // Link looks like: myapp.com/product?storeId=X&productId=Y or /product?store_id=X&product_id=Y
    if (cleanUrl.startsWith("product") && (cleanUrl.contains("storeId=") || cleanUrl.contains("store_id=")) && (cleanUrl.contains("productId=") || cleanUrl.contains("product_id="))) {
        val paramsPart = cleanUrl.substringAfter("?", "")
        val params = paramsPart.split("&").associate { param ->
            val parts = param.split("=")
            val key = parts.getOrNull(0)?.trim()?.lowercase() ?: ""
            val value = parts.getOrNull(1)?.trim() ?: ""
            key to value
        }
        val targetStoreId = params["storeid"] ?: params["store_id"] ?: ""
        val targetProductId = params["productid"] ?: params["product_id"] ?: ""
        
        if (targetStoreId.isNotEmpty() && targetProductId.isNotEmpty()) {
            val matchingProduct = TSLuxeWearRepository.productsFlow.value.find { 
                it.id.equals(targetProductId, ignoreCase = true) && it.storeId.equals(targetStoreId, ignoreCase = true)
            }
            if (matchingProduct != null) {
                if (onProductShared != null) {
                    onProductShared(matchingProduct)
                }
                onGranted("customer_home")
                return
            }
        }
    }
    
    // Also support restful style path segment parsing: store/X/product/Y
    if (cleanUrl.contains("/product/") || cleanUrl.startsWith("store/") || cleanUrl.startsWith("product/")) {
        val parts = cleanUrl.split("/")
        var targetStoreId = ""
        var targetProductId = ""
        val storeIdx = parts.indexOf("store")
        val productIdx = parts.indexOf("product")
        
        if (storeIdx != -1 && storeIdx + 1 < parts.size) {
            targetStoreId = parts[storeIdx + 1]
        }
        if (productIdx != -1 && productIdx + 1 < parts.size) {
            targetProductId = parts[productIdx + 1]
        }
        
        if (targetStoreId.isNotEmpty() && targetProductId.isNotEmpty()) {
            val matchingProduct = TSLuxeWearRepository.productsFlow.value.find { 
                it.id.equals(targetProductId, ignoreCase = true) && it.storeId.equals(targetStoreId, ignoreCase = true)
            }
            if (matchingProduct != null) {
                if (onProductShared != null) {
                    onProductShared(matchingProduct)
                }
                onGranted("customer_home")
                return
            }
        }
    }
    
    if (cleanUrl.startsWith("admin") || cleanUrl == "admin" || cleanUrl.contains("/admin")) {
        // Evaluate Administrator validation rules
        val user = currentUser
        if (user == null) {
            onBlocked(
                "ACCESS DENIED:\nAnonymous Guest accounts are strictly forbidden from viewing Super Administrative controls.\n\nPlease log in through Google with an authorized email address.",
                "login"
            )
        } else if (!AuthManager.isSuperAdminEmail(user.email)) {
            // Block and redirect
            val fallback = when (user.role) {
                UserRole.STORE_OWNER -> "store_owner_dashboard"
                else -> "customer_home"
            }
            onBlocked(
                "UNAUTHORIZED ACTION (403 Forbidden):\nGmail address '${user.email}' is NOT registered within the Super Admin database whitelist.\n\nAccess blocked. Redirecting you to your default dashboard workspace.",
                fallback
            )
        } else {
            // Admin approved
            onGranted("super_admin_dashboard")
        }
    } else {
        // General route matching
        val target = when(cleanUrl) {
            "login", "" -> "login"
            "customer_home", "customer" -> "customer_home"
            "store_owner_dashboard", "owner", "store" -> {
                if (currentUser == null) "login" else "store_owner_dashboard"
            }
            else -> "customer_home"
        }
        onGranted(target)
    }
}

/**
 * Mobile-responsive adaptive drawer menu.
 */
@Composable
fun MobileNavigationDrawer(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    currentRoute: String,
    currentUser: AuthUser?,
    onNavigate: (String) -> Unit
) {
    if (isOpen) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .fillMaxHeight(0.75f)
                    .clip(RoundedCornerShape(16.dp)),
                color = LuxeCharcoal,
                border = BorderStroke(1.5.dp, LuxeGold)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.5.dp, LuxeGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.logo_luxe),
                                    contentDescription = "Brand Logo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Text(
                                text = "TS LUXEWEAR MENU",
                                color = LuxeGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Serif
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    
                    Divider(color = LuxeGold.copy(alpha = 0.3f), thickness = 1.dp)
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    NavigationNavItem(
                        label = "Shopping Zone",
                        description = "Browse boutique catalogs",
                        icon = Icons.Default.Home,
                        isSelected = currentRoute == "customer_home",
                        onClick = {
                            onNavigate("customer_home")
                            onDismiss()
                        }
                    )
                    
                    if (currentUser != null && (currentUser.role == UserRole.STORE_OWNER || currentUser.role == UserRole.SUPER_ADMIN)) {
                        NavigationNavItem(
                            label = "Boutique Panel",
                            description = "Manage sales & inventory",
                            icon = Icons.Default.Store,
                            isSelected = currentRoute == "store_owner_dashboard",
                            onClick = {
                                onNavigate("store_owner_dashboard")
                                onDismiss()
                            }
                        )
                    }
                    
                    if (currentUser != null && currentUser.role == UserRole.SUPER_ADMIN) {
                        NavigationNavItem(
                            label = "Network Admin",
                            description = "Platform core & control",
                            icon = Icons.Default.Settings,
                            isSelected = currentRoute == "super_admin_dashboard",
                            onClick = {
                                onNavigate("super_admin_dashboard")
                                onDismiss()
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (currentUser != null) Color(0xFF25D366) else Color.Gray, CircleShape)
                        )
                        Column {
                            Text(
                                text = currentUser?.displayName ?: "Guest Client",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentUser?.role?.displayName ?: "Viewer Status",
                                color = LuxeGold,
                                fontSize = 8.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Desktop side navigation rail.
 */
@Composable
fun NavigationSideRail(
    currentRoute: String,
    currentUser: AuthUser?,
    onNavigate: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(220.dp)
            .background(LuxeCharcoal)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, LuxeGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_luxe),
                    contentDescription = "Brand Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Column {
                Text(
                    text = "TS LuxeWear",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "COUTURE PORTAL",
                    color = LuxeGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp
                )
            }
        }
        
        NavigationNavItem(
            label = "Shopping Zone",
            description = "Browse boutique catalogs",
            icon = Icons.Default.Home,
            isSelected = currentRoute == "customer_home",
            onClick = { onNavigate("customer_home") }
        )
        
        if (currentUser != null && (currentUser.role == UserRole.STORE_OWNER || currentUser.role == UserRole.SUPER_ADMIN)) {
            NavigationNavItem(
                label = "Boutique Panel",
                description = "Manage sales & inventory",
                icon = Icons.Default.Store,
                isSelected = currentRoute == "store_owner_dashboard",
                onClick = { onNavigate("store_owner_dashboard") }
            )
        }
        
        if (currentUser != null && currentUser.role == UserRole.SUPER_ADMIN) {
            NavigationNavItem(
                label = "Network Admin",
                description = "Platform core & control",
                icon = Icons.Default.Settings,
                isSelected = currentRoute == "super_admin_dashboard",
                onClick = { onNavigate("super_admin_dashboard") }
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = "TS LUXEWEAR © 2026",
            color = Color.Gray,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }
}

/**
 * Dynamic design nav item component.
 */
@Composable
fun NavigationNavItem(
    label: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) LuxeBurgundy else Color.White.copy(alpha = 0.05f)
    val borderCol = if (isSelected) LuxeGold else Color.Transparent
    val textCol = if (isSelected) Color.White else Color.LightGray
    val subTextCol = if (isSelected) LuxeLightGold else Color.Gray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) LuxeGold else Color.LightGray,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = label,
                color = textCol,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = subTextCol,
                fontSize = 8.sp
            )
        }
    }
}


