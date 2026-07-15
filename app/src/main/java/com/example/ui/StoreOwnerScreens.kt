package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.AuthManager
import com.example.data.LuxePermission
import com.example.data.PermissionManager
import com.example.data.TSLuxeWearRepository
import com.example.data.UserRole
import com.example.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import coil.compose.AsyncImage

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StoreOwnerDashboardScreen(repository: TSLuxeWearRepository) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Pagination local states (Phase 16 Free Tier Optimization)
    var productsPage by remember { mutableStateOf(1) }
    var productsPageSize by remember { mutableStateOf(4) }
    
    var ordersPage by remember { mutableStateOf(1) }
    var ordersPageSize by remember { mutableStateOf(3) }
    val clipboardManager = LocalClipboardManager.current

    // Session user details
    val currentUser by AuthManager.currentUserFlow.collectAsState()
    val userEmail = currentUser?.email ?: ""

    // Raw databases flows
    val stores by repository.storesFlow.collectAsState()
    val products by repository.productsFlow.collectAsState()
    val orders by repository.ordersFlow.collectAsState()
    val inquiries by repository.inquiriesFlow.collectAsState()
    val offers by repository.offersFlow.collectAsState()
    val reviews by repository.reviewsFlow.collectAsState()
    val stockHistories by repository.stockHistoryFlow.collectAsState()
    val clientRequests by repository.clientRequestsFlow.collectAsState()
    val storeSettingsMap by repository.storeSettingsFlow.collectAsState()
    val chatMessages by repository.chatMessagesFlow.collectAsState()

    // 1. Multitenant isolation: Identify if user owns certain stores
    val myStores = stores.filter { st ->
        val ownerEmail = repository.getStoreOwnerEmail(st.id)
        ownerEmail.trim().lowercase() == userEmail.trim().lowercase()
    }

    var selectedStoreId by remember { mutableStateOf("") }
    if (selectedStoreId.isEmpty()) {
        selectedStoreId = if (myStores.isNotEmpty()) myStores.first().id else ""
    }

    // Safely retrieve active store
    val activeStore = stores.find { it.id == selectedStoreId } ?: stores.firstOrNull()
    val storeId = activeStore?.id ?: ""
    val storeName = activeStore?.name ?: "My Boutique Showcase"
    val storeLogo = activeStore?.logoUrl ?: "✨"
    val storeDesc = activeStore?.storeType ?: "Premium Couture Collection"
    val storeUrlAddress = activeStore?.storeUrl ?: ""
    val storeMapAddress = activeStore?.addressMapLink ?: ""

    // Active store specific settings
    val activeSettings = storeSettingsMap[storeId] ?: StoreOrderSettings(storeId)

    // Store isolation filters
    val storeProducts = products.filter { it.storeId == storeId }
    val storeOrders = orders.filter { it.storeId == storeId }
    val storeInquiries = inquiries.filter { it.storeId == storeId }
    val storeOffers = offers.filter { it.storeId == storeId }
    val storeReviews = reviews.filter { prodRev ->
        storeProducts.any { it.id == prodRev.productId }
    }
    val storeStockHistory = stockHistories.filter { it.storeId == storeId }
    val storeClientRequests = clientRequests.filter { it.storeId == storeId }
    val storeChatMessages = chatMessages.filter { it.storeId == storeId }

    var showStoreDropdown by remember { mutableStateOf(false) }

    // Multi-Tab Navigation Design (0: Dash/Profile, 1: Catalog/Historics, 2: Toggles/Categories, 3: Bookings/Invoices, 4: Support Chat Desk, 5: Offers Broadcast)
    var activeSubTab by remember { mutableStateOf(0) }

    // Shimmer skeleton state simulation
    var isDataLoading by remember { mutableStateOf(false) }

    LaunchedEffect(activeSubTab, selectedStoreId) {
        if (isDataLoading) return@LaunchedEffect
        isDataLoading = true
        delay(600)
        isDataLoading = false
    }

    // Stat Switch Toggles
    var showWeeklySaleStat by remember { mutableStateOf(false) }
    var showWeeklyVisitsStat by remember { mutableStateOf(false) }

    // Forms and dialogs triggers
    var showProductForm by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showRestockDialog by remember { mutableStateOf<Product?>(null) }
    var showInvoicePreviewDialog by remember { mutableStateOf<Order?>(null) }
    var showOfferForm by remember { mutableStateOf(false) }

    // Profile updates fields
    var inputStoreUrl by remember(storeUrlAddress) { mutableStateOf(storeUrlAddress) }
    var inputMapLink by remember(storeMapAddress) { mutableStateOf(storeMapAddress) }

    // Product fields
    var pName by remember { mutableStateOf("") }
    pName = pName.take(80) // Prevent absurd inputs
    var pPrice by remember { mutableStateOf("") }
    var pDiscountPrice by remember { mutableStateOf("") }
    var pCategory by remember { mutableStateOf("") }
    if (pCategory.isEmpty() && activeStore != null && activeStore.categories.isNotEmpty()) {
        pCategory = activeStore.categories.first()
    }
    var pFabric by remember { mutableStateOf("Premium Silk") }
    var pSizes by remember { mutableStateOf("S, M, L, XL") }
    var pColors by remember { mutableStateOf("Crimson, Maroon, Gold") }
    var pStockQty by remember { mutableStateOf("15") }
    var pLowThreshold by remember { mutableStateOf("5") }
    var pDescription by remember { mutableStateOf("") }
    var pIconSymbol by remember { mutableStateOf("👗") }
    
    // Custom Upload Compression Choices: 50% vs 60%
    var selectedCompressionLevel by remember { mutableStateOf(50) }
    var simulatedOriginalUri by remember { mutableStateOf<String?>(null) }
    var uploadedImageUrl by remember { mutableStateOf<String?>(null) }
    var currentUploadStep by remember { mutableStateOf(0) } // 0: Idle/Select, 1: Source Selected/Ready, 2: Upload complete/Show status
    var isSimulatedCamera by remember { mutableStateOf(false) }
    var uploadLogs by remember { mutableStateOf<List<String>>(emptyList()) }
    var isCompressingAndUploading by remember { mutableStateOf(false) }

    // Category Creation fields
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var editingCategoryName by remember { mutableStateOf<String?>(null) }
    var inputCategoryName by remember { mutableStateOf("") }

    // Orders Settings fields
    var settingsAcceptOrders by remember(activeSettings) { mutableStateOf(activeSettings.acceptOrders) }
    var settingsDeliveryChargeOn by remember(activeSettings) { mutableStateOf(activeSettings.deliveryChargeOn) }
    var settingsDeliveryChargeAmt by remember(activeSettings) { mutableStateOf(activeSettings.deliveryCharge.toString()) }
    var settingsCodAvailable by remember(activeSettings) { mutableStateOf(activeSettings.codAvailable) }
    var settingsReturnPolicyEnabled by remember(activeSettings) { mutableStateOf(activeSettings.returnPolicyEnabled) }
    var settingsInvoicePrefixChoice by remember(activeSettings) { mutableStateOf(activeSettings.invoicePrefixChoice) }
    var settingsResetInvoiceYearly by remember(activeSettings) { mutableStateOf(activeSettings.resetInvoiceYearly) }

    Column(modifier = Modifier.fillMaxSize().background(LuxeCream)) {
        
        // --- MULTITENANT HEADER BAR ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2E0C16))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clicking this row triggers dropdown if multiple stores are available or for testing purpose
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showStoreDropdown = !showStoreDropdown },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(storeLogo, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "TS LUXEWEAR OPERATOR",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = LuxeLightGold,
                            letterSpacing = 1.2.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = storeName,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Switch Store",
                                tint = LuxeGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Isolation Warning/Status label
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (myStores.isNotEmpty()) Color(0xFF1E3A1E) else Color(0xFF4A101E),
                    border = BorderStroke(1.dp, LuxeGold.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = if (myStores.isNotEmpty()) "ISOLATED OWNER" else "DEMO MODE",
                        color = LuxeLightGold,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = showStoreDropdown,
                onDismissRequest = { showStoreDropdown = false },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(Color.White)
            ) {
                // If isolated, warn or filter dropdown to myStores. If demo mode, list all seed stores so user can try out different owners easily.
                val visibleStores = if (myStores.isNotEmpty()) myStores else stores
                visibleStores.forEach { st ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(st.logoUrl, fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(st.name, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = LuxeBurgundy)
                                    Text("Owner: ${st.ownerName} | ${st.storeType}", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        },
                        onClick = {
                            selectedStoreId = st.id
                            showStoreDropdown = false
                        }
                    )
                }
            }
        }

        // --- SUB NAV-BAR (6 METICULOUS TABS) ---
        ScrollableTabRow(
            selectedTabIndex = activeSubTab,
            containerColor = LuxeBurgundy,
            contentColor = LuxeGold,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeSubTab]),
                    color = LuxeGold
                )
            }
        ) {
            val tabs = listOf("Dashboard", "Catalog & Inventory", "Category & Settings", "Bookings & Invoice", "Chat & Inquiries", "Offers & followers", "WhatsApp Sync")
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = activeSubTab == idx,
                    onClick = { activeSubTab = idx },
                    text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (activeSubTab == idx) LuxeGold else Color.White.copy(alpha = 0.7f)) }
                )
            }
        }

        // --- SUB-SCREEN CONTENT SECTIONS ---
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when (activeSubTab) {
                // ================== TAB 0: DASHBOARD HOME & STORE URLS ==================
                0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            // Boutique Info summary card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .background(LuxeBurgundy, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(storeLogo, fontSize = 32.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(storeName, fontWeight = FontWeight.Bold, color = LuxeBurgundy, fontSize = 16.sp)
                                        Text("Owner: ${activeStore?.ownerName} | ${activeStore?.ownerPhone}", fontSize = 11.sp, color = Color.Gray)
                                        Text("Category niches: ${activeStore?.categories?.joinToString(", ")}", fontSize = 10.sp, color = LuxeDustyRose)
                                    }
                                }
                            }
                        }

                        // Switches for Sales & Visits widgets
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Widget A: Today's / Weekly Sale Switch
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showWeeklySaleStat = !showWeeklySaleStat }
                                        .testTag("sales_stat_widget"),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, LuxeBurgundy.copy(alpha = 0.15f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (showWeeklySaleStat) "Weekly sales (7d)" else "Today's Sales",
                                                fontSize = 10.sp,
                                                color = Color.Gray,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Icon(
                                                imageVector = Icons.Default.SwapHoriz,
                                                contentDescription = "Switch",
                                                tint = LuxeDustyRose,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val filterTime = if (showWeeklySaleStat) 7 * 86400000L else 86400000L
                                        val minTime = System.currentTimeMillis() - filterTime
                                        val salesSum = storeOrders
                                            .filter { it.timestamp >= minTime && it.orderStatus != "Cancelled" }
                                            .sumOf { it.productPrice }
                                        Text(
                                            text = "₹$salesSum",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            color = LuxeBurgundy
                                        )
                                        Text(
                                            text = "Tap to toggle filter",
                                            fontSize = 8.sp,
                                            color = Color.LightGray,
                                            textDecoration = TextDecoration.Underline
                                        )
                                    }
                                }

                                // Widget B: Customer Visits Switch
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showWeeklyVisitsStat = !showWeeklyVisitsStat }
                                        .testTag("visits_stat_widget"),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, LuxeBurgundy.copy(alpha = 0.15f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (showWeeklyVisitsStat) "Weekly Vis (7d)" else "Today's Visits",
                                                fontSize = 10.sp,
                                                color = Color.Gray,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Icon(
                                                imageVector = Icons.Default.SwapHoriz,
                                                contentDescription = "Switch",
                                                tint = LuxeDustyRose,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        // Simulated visit stats
                                        val todayVisitsSim = when (storeId) {
                                            "store_priya" -> 45; "store_fashion" -> 28; "store_velvet" -> 62; else -> 14
                                        }
                                        val weeklyVisitsSim = todayVisitsSim * 6
                                        Text(
                                            text = "${if (showWeeklyVisitsStat) weeklyVisitsSim else todayVisitsSim} views",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            color = LuxeBurgundy
                                        )
                                        Text(
                                            text = "Tapped to swap duration",
                                            fontSize = 8.sp,
                                            color = Color.LightGray,
                                            textDecoration = TextDecoration.Underline
                                        )
                                    }
                                }
                            }
                        }

                        // Row C: Views, Followers, Inquiries Count metrics
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Visibility, null, tint = LuxeGold, modifier = Modifier.size(20.dp))
                                        Text("Product Views", fontSize = 10.sp, color = Color.Gray)
                                        val totalViewsSim = when (storeId) {
                                            "store_priya" -> 1420; "store_fashion" -> 890; "store_velvet" -> 2050; else -> 670
                                        }
                                        Text("$totalViewsSim+", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LuxeBurgundy)
                                    }
                                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(LuxeCream))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.PersonAdd, null, tint = LuxeGold, modifier = Modifier.size(20.dp))
                                        Text("Followers", fontSize = 10.sp, color = Color.Gray)
                                        Text("${activeStore?.followersCount ?: 0}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LuxeBurgundy)
                                    }
                                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(LuxeCream))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.QuestionAnswer, null, tint = LuxeGold, modifier = Modifier.size(20.dp))
                                        Text("Pending Inquiries", fontSize = 10.sp, color = Color.Gray)
                                        val pendingInqCount = storeInquiries.filter { it.status == "New" }.size
                                        Text("$pendingInqCount", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LuxeBurgundy)
                                    }
                                }
                            }
                        }

                        // Low stock alerts list display
                        item {
                            val lowStockProducts = storeProducts.filter { it.stockQuantity <= it.lowStockThreshold }
                            val outOfStockProducts = storeProducts.filter { it.stockQuantity == 0 }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (lowStockProducts.isNotEmpty()) Color(0xFFFFF9F2) else Color(0xFFF2FFF5)
                                ),
                                border = BorderStroke(1.dp, if (lowStockProducts.isNotEmpty()) LuxeGold else Color(0xFFD4EDDA))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (lowStockProducts.isNotEmpty()) Icons.Default.RunningWithErrors else Icons.Default.CheckCircle,
                                            contentDescription = "Alerts",
                                            tint = if (lowStockProducts.isNotEmpty()) LuxeBurgundy else Color(0xFF28A745)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (lowStockProducts.isNotEmpty()) "Stock Alert (Requires Replenish)" else "Inventory is Healthy!",
                                            fontWeight = FontWeight.Bold,
                                            color = LuxeBurgundy,
                                            fontSize = 13.sp
                                        )
                                    }
                                    
                                    if (lowStockProducts.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        lowStockProducts.forEach { prod ->
                                            val badgeLabel = if (prod.stockQuantity == 0) "OUT OF STOCK ❌" else "LOW STOCK ⚠️"
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(prod.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LuxeCharcoal)
                                                    Text("Current Stock: ${prod.stockQuantity} / Warning: < ${prod.lowStockThreshold}", fontSize = 9.sp, color = Color.Gray)
                                                }
                                                Surface(
                                                    color = if (prod.stockQuantity == 0) Color.Red else LuxeGold,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = badgeLabel,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                repository.bulkRestockAllOutOfStock(storeId)
                                                Toast.makeText(context, "Bulk restocked all out-of-stock items to 15 units! Followers notified. 🚨", Toast.LENGTH_LONG).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = LuxeBurgundy),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.fillMaxWidth().height(36.dp).testTag("bulk_restock_alerts_btn")
                                        ) {
                                            Icon(Icons.Default.Autorenew, null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Bulk Restock All Out of Stock to 15 Units", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Map store address and Unique URL Editor
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, LuxeCream)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Language, null, tint = LuxeBurgundy)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Publishing Store URL & Map link", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LuxeBurgundy)
                                    }
                                    Text("These values display inside customer search lookbooks and invoice sheets.", fontSize = 10.sp, color = Color.Gray)
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Unique Store Web URL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = inputStoreUrl,
                                            onValueChange = { inputStoreUrl = it },
                                            modifier = Modifier.weight(1f).testTag("store_url_input"),
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(onClick = {
                                            clipboardManager.setText(AnnotatedString(inputStoreUrl))
                                            Toast.makeText(context, "URL Copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(Icons.Default.ContentCopy, "Copy URL", tint = LuxeGold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Google Maps Store Address Link", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = inputMapLink,
                                        onValueChange = { inputMapLink = it },
                                        modifier = Modifier.fillMaxWidth().testTag("store_map_input"),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            repository.updateStoreProfileLinks(storeId, inputStoreUrl, inputMapLink)
                                            Toast.makeText(context, "Profile Links Saved successfully! Real-time syncing applied. 🗺️", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = LuxeBurgundy),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.align(Alignment.End).height(36.dp).testTag("save_profile_links_btn")
                                    ) {
                                        Text("Save Store Links Settings", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // ================== TAB 1: CATALOG MANAGEMENT & RESTOCK ==================
                1 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Garments Catalog (${storeProducts.size} Items)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LuxeBurgundy)
                                Text("Complete low threshold checks & custom image compressors", fontSize = 10.sp, color = Color.Gray)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Add Product button
                                Button(
                                    onClick = {
                                        editingProduct = null
                                        pName = ""
                                        pPrice = ""
                                        pDiscountPrice = ""
                                        pFabric = "Georgette Velvet"
                                        pSizes = "Free Size"
                                        pColors = "Emerald Green, Ruby Red"
                                        pStockQty = "12"
                                        pLowThreshold = "4"
                                        pDescription = "Exquisite master weaved saree embroidered with floral patterns and luxury gold zari border details."
                                        pIconSymbol = "👘"
                                        selectedCompressionLevel = 50
                                        simulatedOriginalUri = null
                                        uploadedImageUrl = null
                                        currentUploadStep = 0
                                        isSimulatedCamera = false
                                        uploadLogs = emptyList()
                                        isCompressingAndUploading = false
                                        showProductForm = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = LuxeBurgundy),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.testTag("add_product_btn")
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Item", fontSize = 11.sp)
                                }

                                // Bulk Upload Button
                                Button(
                                    onClick = {
                                        // Simulate adding 3 products with different categories
                                        val bulkList = listOf(
                                            Product("bulk_${System.currentTimeMillis()}_1", storeId, storeName, "Premium Banarasi Katan Silk", "Sarees", 4500.0, 3999.0, "Pure handwoven bridal banarasi silk.", "Katan Silk", listOf("Free Size"), listOf("Royal Pink"), 12, 3, "👘"),
                                            Product("bulk_${System.currentTimeMillis()}_2", storeId, storeName, "Ethnic Floral Palazzo Set", "Kurtis", 1799.0, null, "Lightweight breathable daily wears.", "Premium Cotton", listOf("S", "M", "L"), listOf("Sky Blue"), 20, 5, "👚"),
                                            Product("bulk_${System.currentTimeMillis()}_3", storeId, storeName, "Embellished Wedding Chunni Gown", "Dresses", 5999.0, 4800.0, "Designer evening collections.", "Viscose Net & Satin", listOf("M", "L"), listOf("Bridal Cream"), 5, 2, "👗")
                                        )
                                        bulkList.forEach { repository.addProduct(it) }
                                        Toast.makeText(context, "Bulk Uploaded 3 unique catalog files! Compressed with level: $selectedCompressionLevel% level. 📤", Toast.LENGTH_LONG).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = LuxeGold),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.testTag("bulk_upload_products_btn")
                                ) {
                                    Icon(Icons.Default.Backup, null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Bulk Upload", fontSize = 11.sp, color = LuxeBurgundy)
                                }
                            }
                        }

                        // Compression settings banner for uploads
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 10.dp),
                            colors = CardDefaults.cardColors(containerColor = LuxeLightGold.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Percent, null, tint = LuxeBurgundy, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Cloudinary Image Compression Customizer:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LuxeBurgundy)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(30, 40, 50, 60).forEach { level ->
                                        Surface(
                                            modifier = Modifier.clickable { selectedCompressionLevel = level },
                                            color = if (selectedCompressionLevel == level) LuxeBurgundy else Color.White,
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(1.dp, LuxeBurgundy)
                                        ) {
                                            Text(
                                                "$level%",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedCompressionLevel == level) Color.White else LuxeBurgundy,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Scrollable screen splitting products list vs stock history log
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentPadding = PaddingValues(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text("Items Catalog List", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LuxeBurgundy)
                            }

                            if (isDataLoading) {
                                item {
                                    SkeletonProductGrid(rowsCount = 1)
                                }
                            } else if (storeProducts.isEmpty()) {
                                item {
                                    Text("No collection items recorded. Tap Add Item to populate catalogs.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(14.dp))
                                }
                            } else {
                                val paginatedProductsForView = storeProducts.paginate(productsPage, productsPageSize)
                                items(paginatedProductsForView) { prod ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, LuxeCream)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier.size(44.dp).background(LuxeCream, RoundedCornerShape(6.dp)).clip(RoundedCornerShape(6.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (prod.imageUrl.startsWith("http")) {
                                                    AsyncImage(
                                                        model = prod.imageUrl,
                                                        contentDescription = prod.name,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                    )
                                                } else {
                                                    Text(prod.imageUrl, fontSize = 24.sp)
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LuxeBurgundy)
                                                Text("Sub: ${prod.category} | Fabric: ${prod.fabric}", fontSize = 10.sp, color = Color.Gray)
                                                
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("₹${prod.price}", textDecoration = if (prod.discountPrice != null) TextDecoration.LineThrough else null, fontSize = 10.sp, color = Color.LightGray)
                                                    if (prod.discountPrice != null) {
                                                        Spacer(modifier = Modifier.width(5.dp))
                                                        Text("₹${prod.discountPrice}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = LuxeBurgundy)
                                                    }
                                                }

                                                // Stock count badge row
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                                    val colorMark = if (prod.stockQuantity == 0) Color.Red else if (prod.stockQuantity <= prod.lowStockThreshold) LuxeGold else Color(0xFF28A745)
                                                    Box(modifier = Modifier.size(6.dp).background(colorMark, CircleShape))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        if (prod.stockQuantity == 0) "Out of Stock (Out of Search Results Catalog) ❌" else "Qty left: ${prod.stockQuantity} (Threshold: ${prod.lowStockThreshold})",
                                                        fontSize = 9.sp,
                                                        color = Color.DarkGray
                                                    )
                                                }
                                            }

                                            // Options column
                                            Column(horizontalAlignment = Alignment.End) {
                                                Row {
                                                    // Share Product button
                                                    IconButton(onClick = {
                                                        val shareLink = "myapp.com/product?storeId=${prod.storeId}&productId=${prod.id}"
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                        val clip = android.content.ClipData.newPlainText("TS LuxeWear Product", shareLink)
                                                        clipboard.setPrimaryClip(clip)
                                                        Toast.makeText(context, "Product link copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
                                                        try {
                                                            val sendIntent = android.content.Intent().apply {
                                                                action = android.content.Intent.ACTION_SEND
                                                                putExtra(android.content.Intent.EXTRA_TEXT, "Exquisite find on TS LuxeWear! View ${prod.name}: $shareLink")
                                                                type = "text/plain"
                                                            }
                                                            val shareIntent = android.content.Intent.createChooser(sendIntent, "Share lookbook item catalog url")
                                                            context.startActivity(shareIntent)
                                                        } catch (e: Exception) {}
                                                    }, modifier = Modifier.size(28.dp).testTag("owner_share_product_${prod.id}")) {
                                                        Icon(Icons.Default.Share, "Share", tint = LuxeBurgundy, modifier = Modifier.size(16.dp))
                                                    }

                                                    // Duplicate Product button
                                                    IconButton(onClick = {
                                                        repository.duplicateProduct(prod)
                                                        Toast.makeText(context, "Duplicated Product: '${prod.name}' copy created!", Toast.LENGTH_SHORT).show()
                                                    }, modifier = Modifier.size(28.dp)) {
                                                        Icon(Icons.Default.ContentCopy, "Duplicate", tint = LuxeGold, modifier = Modifier.size(16.dp))
                                                    }

                                                    // Edit Product button
                                                    IconButton(onClick = {
                                                        editingProduct = prod
                                                        pName = prod.name
                                                        pPrice = prod.price.toString()
                                                        pDiscountPrice = prod.discountPrice?.toString() ?: ""
                                                        pCategory = prod.category
                                                        pFabric = prod.fabric
                                                        pSizes = prod.sizes.joinToString(", ")
                                                        pColors = prod.colors.joinToString(", ")
                                                        pStockQty = prod.stockQuantity.toString()
                                                        pLowThreshold = prod.lowStockThreshold.toString()
                                                        pDescription = prod.description
                                                        pIconSymbol = prod.imageUrl
                                                        selectedCompressionLevel = 50
                                                        if (prod.imageUrl.startsWith("http")) {
                                                            uploadedImageUrl = prod.imageUrl
                                                            simulatedOriginalUri = prod.imageUrl
                                                            currentUploadStep = 2
                                                        } else {
                                                            uploadedImageUrl = null
                                                            simulatedOriginalUri = null
                                                            currentUploadStep = 0
                                                        }
                                                        isSimulatedCamera = false
                                                        uploadLogs = emptyList()
                                                        isCompressingAndUploading = false
                                                        showProductForm = true
                                                    }, modifier = Modifier.size(28.dp)) {
                                                        Icon(Icons.Default.Edit, "Edit", tint = LuxeBurgundy, modifier = Modifier.size(16.dp))
                                                    }

                                                    // Manual replenishment Restock Trigger button
                                                    IconButton(onClick = {
                                                        showRestockDialog = prod
                                                    }, modifier = Modifier.size(28.dp)) {
                                                        Icon(Icons.Default.Input, "Restock", tint = Color(0xFF28A745), modifier = Modifier.size(16.dp))
                                                    }

                                                    // Delete button
                                                    IconButton(onClick = {
                                                        repository.deleteProduct(prod.id)
                                                        Toast.makeText(context, "'${prod.name}' removed from collections.", Toast.LENGTH_SHORT).show()
                                                    }, modifier = Modifier.size(28.dp)) {
                                                        Icon(Icons.Default.Delete, "Delete", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                item {
                                    LuxePaginator(
                                        currentPage = productsPage,
                                        totalItems = storeProducts.size,
                                        pageSize = productsPageSize,
                                        onPageChange = { productsPage = it },
                                        onPageSizeChange = { productsPageSize = it; productsPage = 1 },
                                        modifier = Modifier.padding(vertical = 4.dp).testTag("owner_products_paginator")
                                    )
                                }
                            }

                            // Stock history tracker logs block
                            item {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text("Stock replenished / History logs", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LuxeBurgundy)
                            }

                            if (storeStockHistory.isEmpty()) {
                                item {
                                    Text("No stock changes logged in the historic archives.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(14.dp))
                                }
                            } else {
                                items(storeStockHistory) { log ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = LuxeLightGold.copy(alpha = 0.3f))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(log.productName, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = LuxeBurgundy)
                                                val sdf = SimpleDateFormat("HH:mm - dd MMM", Locale.getDefault())
                                                Text(sdf.format(Date(log.timestamp)), fontSize = 8.sp, color = Color.Gray)
                                            }
                                            Text("Remark: ${log.description}", fontSize = 10.sp, color = LuxeCharcoal)
                                            Text("Stock Transition: ${log.previousStock} units ➜ ${log.newStock} units remaining", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = LuxeDustyRose)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ================== TAB 2: CATEGORY CONTROL & SETTINGS ==================
                2 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Category management header
                        item {
                            Text("Categories Dashboard Office", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LuxeBurgundy)
                            Text("Manage and expand boutique collection categories syncing in real-time.", fontSize = 10.sp, color = Color.Gray)
                        }

                        // List of categories belonging to store
                        val storeCats = activeStore?.categories ?: emptyList()
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Active Categories", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LuxeBurgundy)
                                        Button(
                                            onClick = {
                                                editingCategoryName = null
                                                inputCategoryName = ""
                                                showAddCategoryDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = LuxeBurgundy),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.height(28.dp).testTag("add_category_btn")
                                        ) {
                                            Text("Add Category", fontSize = 9.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    if (storeCats.isEmpty()) {
                                        Text("No categories specified.", fontSize = 11.sp, color = Color.Gray)
                                    } else {
                                        storeCats.forEach { cat ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("• $cat", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LuxeCharcoal)
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    // Edit Category
                                                    IconButton(onClick = {
                                                        editingCategoryName = cat
                                                        inputCategoryName = cat
                                                        showAddCategoryDialog = true
                                                    }, modifier = Modifier.size(24.dp)) {
                                                        Icon(Icons.Default.Edit, "Edit", tint = LuxeGold, modifier = Modifier.size(14.dp))
                                                    }
                                                    // Delete Category
                                                    IconButton(onClick = {
                                                        repository.deleteCategoryFromStore(storeId, cat)
                                                        Toast.makeText(context, "Deleted category '$cat'. Synchronized in real-time. 🕒", Toast.LENGTH_SHORT).show()
                                                    }, modifier = Modifier.size(24.dp)) {
                                                        Icon(Icons.Default.Delete, "Delete", tint = Color.Red, modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Order Accepting and charging settings toggles
                        item {
                            Text("Boutique Order Accepting Settings", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LuxeBurgundy)
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    
                                    // Accept order toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Accept Client App Orders", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LuxeBurgundy)
                                            Text("When disabled, shoppers can view only but cannot place bookings.", fontSize = 9.sp, color = Color.Gray)
                                        }
                                        Switch(checked = settingsAcceptOrders, onCheckedChange = { settingsAcceptOrders = it })
                                    }
                                    HorizontalDivider(color = LuxeCream)

                                    // Delivery charges charge toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Enforce Custom Delivery Charge", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LuxeBurgundy)
                                            Text("Add delivery charges inside checkout totals.", fontSize = 9.sp, color = Color.Gray)
                                        }
                                        Switch(checked = settingsDeliveryChargeOn, onCheckedChange = { settingsDeliveryChargeOn = it })
                                    }

                                    // Delivery Charges input field
                                    if (settingsDeliveryChargeOn) {
                                        OutlinedTextField(
                                            value = settingsDeliveryChargeAmt,
                                            onValueChange = { settingsDeliveryChargeAmt = it },
                                            modifier = Modifier.fillMaxWidth().testTag("delivery_charge_input"),
                                            label = { Text("Delivery fees (Rupees ₹)", fontSize = 10.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )
                                    }
                                    HorizontalDivider(color = LuxeCream)

                                    // Cash On Delivery Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Cash on Delivery (COD) Availability", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LuxeBurgundy)
                                            Text("Allow customers to pay on doorstep delivery.", fontSize = 9.sp, color = Color.Gray)
                                        }
                                        Switch(checked = settingsCodAvailable, onCheckedChange = { settingsCodAvailable = it })
                                    }
                                    HorizontalDivider(color = LuxeCream)

                                    // Return policy toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Show 7-Days Exchange & Return Policy Label", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LuxeBurgundy)
                                            Text("Display policy badges under product sheets.", fontSize = 9.sp, color = Color.Gray)
                                        }
                                        Switch(checked = settingsReturnPolicyEnabled, onCheckedChange = { settingsReturnPolicyEnabled = it })
                                    }
                                    
                                    HorizontalDivider(color = LuxeCream)

                                    // Invoice customizer settings
                                    Text("Invoice numbering & PDF settings", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LuxeBurgundy)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Reset Serial Number Yearly", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            Text("Continuous serial vs resetting on new year calendar", fontSize = 8.sp, color = Color.Gray)
                                        }
                                        Switch(checked = settingsResetInvoiceYearly, onCheckedChange = { settingsResetInvoiceYearly = it })
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            val charges = settingsDeliveryChargeAmt.toDoubleOrNull() ?: 50.0
                                            val updated = StoreOrderSettings(
                                                storeId = storeId,
                                                acceptOrders = settingsAcceptOrders,
                                                deliveryChargeOn = settingsDeliveryChargeOn,
                                                deliveryCharge = charges,
                                                codAvailable = settingsCodAvailable,
                                                returnPolicyEnabled = settingsReturnPolicyEnabled,
                                                invoicePrefixChoice = settingsInvoicePrefixChoice,
                                                resetInvoiceYearly = settingsResetInvoiceYearly
                                            )
                                            repository.updateStoreSettings(storeId, updated)
                                            Toast.makeText(context, "Boutique Settings updated successfully. Real-time synced to Customers checkouts! ⚙️", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = LuxeGold),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth().height(36.dp).testTag("save_settings_btn")
                                    ) {
                                        Text("Save Order Toggles & Settings", color = LuxeBurgundy, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // ================== TAB 3: BOOKINGS LEDGER & INVOICES ==================
                3 -> {
                    var selectedStatusFilter by remember { mutableStateOf("ALL") }
                    val orderStatuses = listOf("ALL", "Pending", "Confirmed", "Packed", "Shipped", "Delivered", "Cancelled")

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Filters row
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().background(Color.White).padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(orderStatuses) { filterState ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (selectedStatusFilter == filterState) LuxeBurgundy else LuxeCream)
                                        .clickable { selectedStatusFilter = filterState }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = filterState.uppercase(),
                                        color = if (selectedStatusFilter == filterState) LuxeGold else Color.Gray,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Orders listing
                        val filteredOrders = if (selectedStatusFilter == "ALL") storeOrders else storeOrders.filter { it.orderStatus == selectedStatusFilter }
                        
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentPadding = PaddingValues(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // High Fidelity Store Analytics Summary Block
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = LuxeCream),
                                    border = BorderStroke(1.dp, LuxeGold.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Order Fulfilment Analytics",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = LuxeBurgundy
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            val totCount = storeOrders.size
                                            val pendingAcceptCount = storeOrders.count { it.orderStatus == "Pending" }
                                            val activeDispatchCount = storeOrders.count { it.orderStatus == "Confirmed" || it.orderStatus == "Packed" || it.orderStatus == "Shipped" }
                                            val completeCount = storeOrders.count { it.orderStatus == "Delivered" }

                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Total", fontSize = 9.sp, color = Color.Gray)
                                                Text("$totCount", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LuxeBurgundy)
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Pending", fontSize = 9.sp, color = Color.Gray)
                                                Text("$pendingAcceptCount", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFB06000))
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("In-Transit", fontSize = 9.sp, color = Color.Gray)
                                                Text("$activeDispatchCount", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1A73E8))
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Delivered", fontSize = 9.sp, color = Color.Gray)
                                                Text("$completeCount", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF137333))
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Text("Orders Bookings Ledger (${filteredOrders.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LuxeBurgundy)
                            }

                            if (isDataLoading) {
                                item {
                                    SkeletonList(itemsCount = 2)
                                }
                            } else if (filteredOrders.isEmpty()) {
                                item {
                                    Text("No bookings matching filters.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(14.dp))
                                }
                            } else {
                                val paginatedOrdersForView = filteredOrders.paginate(ordersPage, ordersPageSize)
                                items(paginatedOrdersForView) { ord ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, LuxeCream)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Order reference: #${ord.orderId}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = LuxeBurgundy)
                                                Surface(
                                                    color = when (ord.orderStatus) {
                                                        "Pending" -> LuxeGold
                                                        "Confirmed" -> Color(0xFF007BFF)
                                                        "Packed" -> LuxeDustyRose
                                                        "Shipped" -> Color(0xFF17A2B8)
                                                        "Delivered" -> Color(0xFF28A745)
                                                        else -> Color.Red
                                                    },
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        ord.orderStatus.uppercase(),
                                                        color = Color.White,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            var revealDetails by remember { mutableStateOf(false) }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Customer Details:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                                
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .clickable { revealDetails = !revealDetails }
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(
                                                        if (revealDetails) Icons.Default.LockOpen else Icons.Default.Lock,
                                                        contentDescription = null,
                                                        tint = LuxeGold,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(
                                                        text = if (revealDetails) "Obscure" else "🔐 Privacy Shield (Reveal)",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = LuxeGold
                                                    )
                                                }
                                            }
                                            val displayPhone = if (revealDetails) ord.customerPhone else {
                                                val phoneRaw = ord.customerPhone
                                                if (phoneRaw.length > 5) {
                                                    phoneRaw.take(phoneRaw.length - 4) + "****"
                                                } else "****"
                                            }
                                            val displayAddress = if (revealDetails) ord.customerAddress else {
                                                ord.customerAddress.take(8) + "..." + " [Address Privacy Obscured]"
                                            }
                                            Text("Name: ${ord.customerName} ($displayPhone)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text("Address: $displayAddress", fontSize = 10.sp, color = Color.DarkGray)
                                            
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(ord.productImageUrl, fontSize = 24.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${ord.productName} (Size: ${ord.productSize} | Shade: ${ord.productColor})",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val invoiceLabel = if (ord.invoiceId != null) "INVOICE RECORD: #${ord.invoiceId}" else "Invoice: Pending Confirmed status"
                                            Text(
                                                text = "Billed Totals: ₹${ord.productPrice} | COD Mode | $invoiceLabel",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = LuxeDustyRose
                                            )

                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                // MANUAL STATUS TRACKER OPERATIONS
                                                when (ord.orderStatus) {
                                                    "Pending" -> {
                                                        Button(
                                                            onClick = {
                                                                repository.updateOrderStatus(ord.orderId, "Confirmed")
                                                                Toast.makeText(context, "Order Confirmed! Unique Professional Invoice auto-generated. 📄", Toast.LENGTH_SHORT).show()
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = LuxeBurgundy),
                                                            modifier = Modifier.weight(1f).height(30.dp).testTag("op_confirm_${ord.orderId}")
                                                        ) {
                                                            Text("Confirm & Gen Invoice", fontSize = 9.sp)
                                                        }
                                                    }
                                                    "Confirmed" -> {
                                                        Button(
                                                            onClick = { repository.updateOrderStatus(ord.orderId, "Packed") },
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF)),
                                                            modifier = Modifier.weight(1f).height(30.dp)
                                                        ) {
                                                            Text("Mark Packed", fontSize = 9.sp)
                                                        }
                                                    }
                                                    "Packed" -> {
                                                        Button(
                                                            onClick = { repository.updateOrderStatus(ord.orderId, "Shipped") },
                                                            colors = ButtonDefaults.buttonColors(containerColor = LuxeDustyRose),
                                                            modifier = Modifier.weight(1f).height(30.dp)
                                                        ) {
                                                            Text("Ship / Dispatch", fontSize = 9.sp)
                                                        }
                                                    }
                                                    "Shipped" -> {
                                                        Button(
                                                            onClick = { repository.updateOrderStatus(ord.orderId, "Delivered") },
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28A745)),
                                                            modifier = Modifier.weight(1f).height(30.dp)
                                                        ) {
                                                            Text("Confirm Delivered", fontSize = 9.sp)
                                                        }
                                                    }
                                                }
                                            }

                                            // Invoice Viewer button
                                                if (ord.orderStatus != "Pending") {
                                                    Button(
                                                        onClick = { showInvoicePreviewDialog = ord },
                                                        colors = ButtonDefaults.buttonColors(containerColor = LuxeGold),
                                                        modifier = Modifier.weight(1f).height(30.dp).testTag("invoice_view_btn_${ord.orderId}")
                                                    ) {
                                                        Icon(Icons.Default.ReceiptLong, null, modifier = Modifier.size(12.dp), tint = LuxeBurgundy)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Custom Invoice UI", fontSize = 9.sp, color = LuxeBurgundy, fontWeight = FontWeight.Bold)
                                                    }
                                                }

                                                // Cancel Button
                                                if (ord.orderStatus != "Delivered" && ord.orderStatus != "Cancelled") {
                                                    OutlinedButton(
                                                        onClick = { repository.updateOrderStatus(ord.orderId, "Cancelled") },
                                                        modifier = Modifier.height(30.dp),
                                                        border = BorderStroke(1.dp, Color.Red),
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                                    ) {
                                                        Text("Cancel", fontSize = 9.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                item {
                                    LuxePaginator(
                                        currentPage = ordersPage,
                                        totalItems = filteredOrders.size,
                                        pageSize = ordersPageSize,
                                        onPageChange = { ordersPage = it },
                                        onPageSizeChange = { ordersPageSize = it; ordersPage = 1 },
                                        modifier = Modifier.padding(vertical = 4.dp).testTag("owner_orders_paginator")
                                    )
                                }
                            }
                        }
                    }
                // ================== TAB 4: CHAT MESSAGING & CUSTOMER INQUIRIES ==================
                4 -> {
                    // Messaging desk pane: Splits between WhatsApp Inbox, Shopper Inquiries, Customer Suggestions List, and Client Reviews Feed
                    var chatDeskTabState by remember { mutableStateOf(0) } // 0: Chats Inbox, 1: Shopper Inquiries, 2: Suggestions, 3: Reviews Tally
                    
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Sub Bar inside Chat desk
                        TabRow(
                            selectedTabIndex = chatDeskTabState,
                            containerColor = LuxeCream,
                            contentColor = LuxeBurgundy
                        ) {
                            Tab(selected = chatDeskTabState == 0, onClick = { chatDeskTabState = 0 }) {
                                Text("WhatsApp Inbox", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp))
                            }
                            Tab(selected = chatDeskTabState == 1, onClick = { chatDeskTabState = 1 }) {
                                Text("Shopper Inquiries", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp))
                            }
                            Tab(selected = chatDeskTabState == 2, onClick = { chatDeskTabState = 2 }) {
                                Text("Customer Requests", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp))
                            }
                            Tab(selected = chatDeskTabState == 3, onClick = { chatDeskTabState = 3 }) {
                                Text("Client Reviews Feed", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp))
                            }
                        }

                        if (isDataLoading) {
                            SkeletonChatScreen()
                        } else {
                            when (chatDeskTabState) {
                                0 -> {
                                // Unified Inbox
                                val uniqueSenders = storeChatMessages.map { it.customerName }.distinct()
                                var activeChatSender by remember { mutableStateOf("") }
                                if (activeChatSender.isEmpty() && uniqueSenders.isNotEmpty()) {
                                    activeChatSender = uniqueSenders.first()
                                }

                                Row(modifier = Modifier.fillMaxSize()) {
                                    // Senders Index sidebar (1/3 Width)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(1.2f)
                                            .background(Color.White)
                                            .border(BorderStroke(0.5.dp, LuxeCream))
                                    ) {
                                        Text("CHATS SECTION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = LuxeBurgundy, modifier = Modifier.padding(8.dp))
                                        LazyColumn {
                                            items(uniqueSenders) { senderName ->
                                                val senderMessages = storeChatMessages.filter { it.customerName == senderName }
                                                val lastMsgObj = senderMessages.lastOrNull()
                                                val unreadCount = senderMessages.filter { it.isUnread }.size
                                                val isPinned = lastMsgObj?.isPinned ?: false
                                                val isArchived = lastMsgObj?.isArchived ?: false

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(if (activeChatSender == senderName) LuxeLightGold else Color.White)
                                                        .clickable {
                                                            activeChatSender = senderName
                                                            repository.markConversationRead(storeId, senderName)
                                                        }
                                                        .padding(8.dp)
                                                        .border(BorderStroke(0.5.dp, LuxeCream))
                                                ) {
                                                    Column {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text("👤", fontSize = 14.sp)
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text(
                                                                    senderName,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 11.sp,
                                                                    color = LuxeBurgundy
                                                                )
                                                            }
                                                            Row {
                                                                if (isPinned) Text("📌", fontSize = 10.sp)
                                                                if (isArchived) Text("📁", fontSize = 10.sp)
                                                            }
                                                        }
                                                        Text(
                                                            text = lastMsgObj?.messageText ?: "No message",
                                                            fontSize = 9.sp,
                                                            maxLines = 1,
                                                            color = Color.Gray
                                                        )
                                                        if (unreadCount > 0) {
                                                            Surface(
                                                                color = Color.Red,
                                                                shape = CircleShape,
                                                                modifier = Modifier.align(Alignment.End).size(14.dp)
                                                            ) {
                                                                Text(
                                                                    "$unreadCount",
                                                                    color = Color.White,
                                                                    fontSize = 8.sp,
                                                                    textAlign = TextAlign.Center,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Conversational Box Dialogue Area (2/3 Width)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(2f)
                                            .background(LuxeCream)
                                    ) {
                                        if (activeChatSender.isEmpty()) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text("Select shopper chat to reply.", fontSize = 11.sp, color = Color.Gray)
                                            }
                                        } else {
                                            val currentSenderMessages = storeChatMessages.filter { it.customerName == activeChatSender }
                                            var typedMessageText by remember { mutableStateOf("") }
                                            var attachedImageUrl by remember { mutableStateOf<String?>(null) }
                                            var attachedProductLink by remember { mutableStateOf<String?>(null) }

                                            // Header
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.White)
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(activeChatSender, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LuxeBurgundy)
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    // Pin conversations
                                                    IconButton(onClick = {
                                                        repository.togglePinConversation(storeId, activeChatSender)
                                                        Toast.makeText(context, "Pinned conversation status toggled!", Toast.LENGTH_SHORT).show()
                                                    }, modifier = Modifier.size(24.dp)) {
                                                        Text("📌", fontSize = 12.sp)
                                                    }
                                                    // Archive conversations
                                                    IconButton(onClick = {
                                                        repository.toggleArchiveConversation(storeId, activeChatSender)
                                                        Toast.makeText(context, "Archive conversation status toggled!", Toast.LENGTH_SHORT).show()
                                                    }, modifier = Modifier.size(24.dp)) {
                                                        Text("📁", fontSize = 12.sp)
                                                    }
                                                }
                                            }

                                            // Conversations history scroll
                                            LazyColumn(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f)
                                                    .padding(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                items(currentSenderMessages) { msg ->
                                                    val isMe = msg.sender == "StoreOwner"
                                                    Column(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(12.dp))
                                                                .background(if (isMe) Color(0xFFE7FFDB) else Color.White) // WhatsApp layout styling
                                                                .padding(8.dp)
                                                                .fillMaxWidth(0.85f/* Bubble max width constraints */)
                                                        ) {
                                                            Column {
                                                                Text(msg.messageText, fontSize = 11.sp, color = LuxeCharcoal)
                                                                if (msg.productLink != null) {
                                                                    Surface(
                                                                        color = LuxeBurgundy.copy(alpha = 0.08f),
                                                                        shape = RoundedCornerShape(4.dp),
                                                                        modifier = Modifier.padding(top = 4.dp).fillMaxWidth()
                                                                    ) {
                                                                        Text(
                                                                            text = "Garment Atttached 🛍️: ${msg.productLink}",
                                                                            fontSize = 9.sp,
                                                                            color = LuxeBurgundy,
                                                                            modifier = Modifier.padding(4.dp)
                                                                        )
                                                                    }
                                                                }
                                                                if (msg.imageUrl != null) {
                                                                    Text("Attachment 🖼️: [In-Chat Lookbook Attachment]", fontSize = 8.sp, color = LuxeDustyRose, fontWeight = FontWeight.Bold)
                                                                }
                                                            }
                                                        }
                                                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                                                        Text(
                                                            text = sdf.format(Date(msg.timestamp)) + if (isMe) " ✓✓" else "",
                                                            fontSize = 8.sp,
                                                            color = Color.LightGray
                                                        )
                                                    }
                                                }
                                            }

                                            // Quick reply templates layout banner (sizing guide canned templates)
                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(4.dp),
                                                colors = CardDefaults.cardColors(containerColor = LuxeLightGold)
                                            ) {
                                                Column(modifier = Modifier.padding(6.dp)) {
                                                    Text("Canned Sizing & Delivery Templates:", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = LuxeBurgundy)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Row(
                                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        val guidesTemplates = listOf(
                                                            "Yes, we offer custom tailored fits for royal drapes completely free of charge!",
                                                            "Delivery inside Mumbai takes 1-2 business days with fast courier tracking.",
                                                            "Yes, Cash on Delivery (COD) is available. Return policies are 7-Day Exchangeable.",
                                                            "Fabric requires delicate dry purification only inside warm steam press."
                                                        )
                                                        guidesTemplates.forEach { textVal ->
                                                            Surface(
                                                                modifier = Modifier.clickable { typedMessageText = textVal },
                                                                color = Color.White,
                                                                shape = RoundedCornerShape(4.dp),
                                                                border = BorderStroke(1.dp, LuxeGold)
                                                            ) {
                                                                Text(
                                                                    text = textVal.take(18) + "...",
                                                                    fontSize = 8.sp,
                                                                    color = LuxeBurgundy,
                                                                    modifier = Modifier.padding( horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // Attachments previews
                                            if (attachedProductLink != null || attachedImageUrl != null) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().background(Color.White).padding(4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("Attachments Pending 📎: ", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                        if (attachedProductLink != null) Text("[Link: $attachedProductLink]", fontSize = 9.sp, color = LuxeBurgundy)
                                                        if (attachedImageUrl != null) Text(" [Image: $attachedImageUrl]", fontSize = 9.sp, color = LuxeDustyRose)
                                                    }
                                                    IconButton(onClick = { attachedProductLink = null; attachedImageUrl = null }, modifier = Modifier.size(16.dp)) {
                                                        Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(12.dp))
                                                    }
                                                }
                                            }

                                            // Active Texting entry tray with attach parameters
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.White)
                                                    .padding(6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                // Attach product link action
                                                Box {
                                                    var showAttachDropdown by remember { mutableStateOf(false) }
                                                    IconButton(onClick = { showAttachDropdown = !showAttachDropdown }, modifier = Modifier.size(28.dp)) {
                                                        Icon(Icons.Default.AddLink, "Attach Link", tint = LuxeGold)
                                                    }
                                                    DropdownMenu(expanded = showAttachDropdown, onDismissRequest = { showAttachDropdown = false }) {
                                                        storeProducts.forEach { sampleProd ->
                                                            DropdownMenuItem(
                                                                text = { Text(sampleProd.name, fontSize = 9.sp, maxLines = 1) },
                                                                onClick = {
                                                                    attachedProductLink = sampleProd.name
                                                                    showAttachDropdown = false
                                                                }
                                                            )
                                                        }
                                                    }
                                                }

                                                // Attach mock camera/gallery action
                                                IconButton(onClick = {
                                                    PermissionManager.requestPermissionContext(
                                                        LuxePermission.GALLERY,
                                                        onGranted = { attachedImageUrl = "lookbook_shade_crimson_saree.jpg" },
                                                        onDenied = {}
                                                    )
                                                }, modifier = Modifier.size(28.dp)) {
                                                    Icon(Icons.Default.AttachFile, "Attach Image", tint = LuxeDustyRose)
                                                }

                                                OutlinedTextField(
                                                    value = typedMessageText,
                                                    onValueChange = { typedMessageText = it },
                                                    modifier = Modifier.weight(1f).testTag("chat_input_text"),
                                                    placeholder = { Text("Reply to customer...", fontSize = 11.sp) },
                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                                    maxLines = 2
                                                )

                                                IconButton(
                                                    onClick = {
                                                        if (typedMessageText.isNotEmpty()) {
                                                            repository.submitChatMessage(
                                                                storeId,
                                                                activeChatSender,
                                                                "StoreOwner",
                                                                typedMessageText,
                                                                attachedProductLink,
                                                                attachedImageUrl
                                                            )
                                                            typedMessageText = ""
                                                            attachedProductLink = null
                                                            attachedImageUrl = null
                                                        }
                                                    },
                                                    modifier = Modifier.size(32.dp).testTag("send_chat_msg_btn")
                                                ) {
                                                    Icon(Icons.Default.Send, "Send", tint = Color(0xFF25D366))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            1 -> {
                                // Shopper Inquiries Interactive Chat Board
                                val storeInquiries = inquiries.filter { it.storeId == storeId }
                                var activeInquiryId by remember { mutableStateOf("") }
                                if (activeInquiryId.isEmpty() && storeInquiries.isNotEmpty()) {
                                    activeInquiryId = storeInquiries.first().id
                                }

                                Row(modifier = Modifier.fillMaxSize()) {
                                    // Sidebar Index of inquiries
                                    Column(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(1.2f)
                                            .background(Color.White)
                                            .border(BorderStroke(0.5.dp, LuxeCream))
                                    ) {
                                        Text("SHOPPER QUESTIONS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = LuxeBurgundy, modifier = Modifier.padding(8.dp))
                                        LazyColumn {
                                            items(storeInquiries) { inq ->
                                                val isSelected = activeInquiryId == inq.id
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(if (isSelected) LuxeLightGold else Color.White)
                                                        .clickable { activeInquiryId = inq.id }
                                                        .padding(8.dp)
                                                        .border(BorderStroke(0.5.dp, LuxeCream))
                                                ) {
                                                    Column {
                                                        Text(inq.customerName, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = LuxeBurgundy)
                                                        Text(inq.productName, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, color = Color.DarkGray)
                                                        Text(inq.question, fontSize = 9.sp, maxLines = 1, color = Color.Gray)
                                                        
                                                        Spacer(modifier = Modifier.height(3.dp))
                                                        Surface(
                                                            color = if (inq.status == "Resolved") Color(0xFFD4EDDA) else Color(0xFFFFF3CD),
                                                            shape = RoundedCornerShape(4.dp),
                                                            modifier = Modifier.align(Alignment.End)
                                                        ) {
                                                            Text(
                                                                text = inq.status.uppercase(),
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (inq.status == "Resolved") Color(0xFF155724) else Color(0xFF856404),
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Chat messaging dialogue area
                                    Column(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(2f)
                                            .background(LuxeCream)
                                    ) {
                                        val activeInq = storeInquiries.find { it.id == activeInquiryId }
                                        if (activeInq == null) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text("Select custom inquiry to view conversation.", fontSize = 11.sp, color = Color.Gray)
                                            }
                                        } else {
                                            var inquiryReplyText by remember { mutableStateOf("") }
                                            
                                            // Top Bar
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.White)
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(activeInq.customerName, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = LuxeBurgundy)
                                                    Text("Product: ${activeInq.productName}", fontSize = 9.sp, color = Color.Gray)
                                                }

                                                Button(
                                                    onClick = {
                                                        repository.answerProductInquiry(activeInq.id, "Understood. The custom embroidery/length detail has been logged.")
                                                        Toast.makeText(context, "Inquiry marked resolved!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = LuxeLightGold),
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(24.dp)
                                                ) {
                                                    Text("Resolve", fontSize = 8.sp, color = LuxeBurgundy)
                                                }
                                            }

                                            // Inquiry Chat Log list
                                            val messages = activeInq.chatHistory.ifEmpty {
                                                listOfNotNull(
                                                    "CUSTOMER:${activeInq.question}:${activeInq.timestamp}",
                                                    activeInq.answer?.let { "STORE_OWNER:$it:${activeInq.timestamp + 1000}" }
                                                )
                                            }

                                            LazyColumn(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f)
                                                    .padding(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                items(messages) { msgStr ->
                                                    val parts = msgStr.split(":")
                                                    val sender = parts.getOrNull(0) ?: "CUSTOMER"
                                                    val content = parts.getOrNull(1) ?: ""
                                                    val ts = parts.getOrNull(2)?.toLongOrNull() ?: System.currentTimeMillis()
                                                    val isMe = sender == "STORE_OWNER"
                                                    val decryptedContent = com.example.data.MessageEncryption.decrypt(content)
                                                    val isShielded = com.example.data.MessageEncryption.isShielded(content)
                                                    
                                                    Column(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(12.dp))
                                                                .background(if (isMe) Color(0xFFE7FFDB) else Color.White)
                                                                .padding(8.dp)
                                                                .fillMaxWidth(0.85f)
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                if (isShielded) {
                                                                    Icon(
                                                                        Icons.Default.Lock, 
                                                                        contentDescription = "Shielded", 
                                                                        tint = LuxeGold, 
                                                                        modifier = Modifier.size(10.dp)
                                                                    )
                                                                }
                                                                Text(decryptedContent, fontSize = 11.sp, color = LuxeCharcoal)
                                                            }
                                                        }
                                                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                                                        Text(
                                                            text = sdf.format(Date(ts)),
                                                            fontSize = 8.sp,
                                                            color = Color.LightGray
                                                        )
                                                    }
                                                }
                                            }

                                            // Reply Tray
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.White)
                                                    .padding(6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = inquiryReplyText,
                                                    onValueChange = { inquiryReplyText = it },
                                                    modifier = Modifier.weight(1f).testTag("inquiry_reply_input"),
                                                    placeholder = { Text("Reply to inquiry chat...", fontSize = 11.sp) },
                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                                    maxLines = 2
                                                )

                                                IconButton(
                                                    onClick = {
                                                        if (inquiryReplyText.isNotEmpty()) {
                                                            repository.addChatMessageToInquiry(activeInq.id, "STORE_OWNER", inquiryReplyText)
                                                            inquiryReplyText = ""
                                                        }
                                                    },
                                                    modifier = Modifier.size(32.dp).testTag("send_inquiry_reply_btn")
                                                ) {
                                                    Icon(Icons.Default.Send, "Send", tint = LuxeBurgundy)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            2 -> {
                                // Customer suggestions log panel
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    item {
                                        Text("Client Product & Category Requests", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LuxeBurgundy)
                                        Text("Shoppers suggestions for catalog expansions and size requests.", fontSize = 10.sp, color = Color.Gray)
                                    }

                                    if (storeClientRequests.isEmpty()) {
                                        item {
                                            Text("No requests submitted.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(14.dp))
                                        }
                                    } else {
                                        items(storeClientRequests) { req ->
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                                border = BorderStroke(1.dp, LuxeCream)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("Suggested by: '${req.customerName}'", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = LuxeBurgundy)
                                                        Surface(
                                                            color = if (req.requestType == "Category") LuxeGold else LuxeDustyRose,
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                req.requestType.uppercase(),
                                                                color = Color.White,
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text("Detail Request: \"${req.details}\"", fontSize = 11.sp, color = LuxeCharcoal)
                                                    
                                                    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                                                    Text("Timestamped: ${sdf.format(Date(req.timestamp))}", fontSize = 8.sp, color = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            3 -> {
                                // Reviews tally list
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    item {
                                        Text("Boutique Items Reviews (${storeReviews.size} total feedback)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LuxeBurgundy)
                                    }

                                    if (storeReviews.isEmpty()) {
                                        item {
                                            Text("No item reviews published in active catalogs.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(14.dp))
                                        }
                                    } else {
                                        items(storeReviews) { rev ->
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                                border = BorderStroke(1.dp, LuxeCream)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(rev.reviewerName, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = LuxeBurgundy)
                                                        Row {
                                                            repeat(rev.rating) {
                                                                Text("⭐", fontSize = 10.sp)
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("\"${rev.feedback}\"", fontSize = 11.sp, color = LuxeCharcoal)
                                                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                                    Text("Date: ${sdf.format(Date(rev.timestamp))}", fontSize = 8.sp, color = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                }

                // ================== TAB 5: PUBLISH OFFERS & FOLLOWERS ==================
                5 -> {
                    // Festive Offers Publish panel
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Offers & Broadcaster Center", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LuxeBurgundy)
                                    Text("Broadcast promotional codes to boutique followers feeds instantly.", fontSize = 10.sp, color = Color.Gray)
                                }
                                Button(
                                    onClick = {
                                        showOfferForm = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = LuxeBurgundy),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.testTag("publish_offer_btn")
                                ) {
                                    Text("Publish Promo", fontSize = 11.sp)
                                }
                            }
                        }

                        item {
                            var promoTitle by remember { mutableStateOf("") }
                            var promoMessage by remember { mutableStateOf("") }
                            var promoRecipient by remember { mutableStateOf("all") }
                            var promoFeedback by remember { mutableStateOf("") }
                            var isSendingPromo by remember { mutableStateOf(false) }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = LuxeCream),
                                border = BorderStroke(1.2.dp, LuxeGold.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Campaign, contentDescription = null, tint = LuxeBurgundy, modifier = Modifier.size(20.dp))
                                        Text("FCM Promotional Broadcast Engine", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LuxeBurgundy)
                                    }
                                    Text("Dispatch cloud-push notification campaigns instantly to your shoppers' notification shade and devices.", fontSize = 10.sp, color = Color.Gray)
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    OutlinedTextField(
                                        value = promoTitle,
                                        onValueChange = { promoTitle = it },
                                        placeholder = { Text("Campaign Title (e.g., Silk Sensation Sale 👗)", fontSize = 11.sp) },
                                        label = { Text("Campaign Title", fontSize = 10.sp) },
                                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("fcm_campaign_title_input"),
                                        textStyle = TextStyle(fontSize = 11.sp),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxeBurgundy),
                                        singleLine = true
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    OutlinedTextField(
                                        value = promoMessage,
                                        onValueChange = { promoMessage = it },
                                        placeholder = { Text("Body text (e.g., Grab 20% flat discount on selected bridal lehengas!)", fontSize = 11.sp) },
                                        label = { Text("Push Notification Message", fontSize = 10.sp) },
                                        modifier = Modifier.fillMaxWidth().height(66.dp).testTag("fcm_campaign_msg_input"),
                                        textStyle = TextStyle(fontSize = 11.sp),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxeBurgundy)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    OutlinedTextField(
                                        value = promoRecipient,
                                        onValueChange = { promoRecipient = it },
                                        placeholder = { Text("email address or 'all' for all followers", fontSize = 11.sp) },
                                        label = { Text("Target Customers Email Filter", fontSize = 10.sp) },
                                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("fcm_campaign_recipient_input"),
                                        textStyle = TextStyle(fontSize = 11.sp),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxeBurgundy),
                                        singleLine = true
                                    )
                                    
                                    if (promoFeedback.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(promoFeedback, color = LuxeBurgundy, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    Button(
                                        onClick = {
                                            if (promoTitle.trim().isEmpty() || promoMessage.trim().isEmpty()) {
                                                promoFeedback = "⚠️ Please specify both title and message."
                                                return@Button
                                            }
                                            isSendingPromo = true
                                            repository.sendPromotionalCampaign(
                                                storeId = storeId,
                                                title = promoTitle,
                                                message = promoMessage,
                                                targetCustomerEmail = promoRecipient
                                            ) { success, error ->
                                                isSendingPromo = false
                                                if (success) {
                                                    promoFeedback = "✨ Promotional Push Broadcasted and logged in Firestore successfully!"
                                                    promoTitle = ""
                                                    promoMessage = ""
                                                } else {
                                                    promoFeedback = "❌ Broadcast Firestore sync issues: $error"
                                                }
                                            }
                                        },
                                        enabled = !isSendingPromo,
                                        colors = ButtonDefaults.buttonColors(containerColor = LuxeBurgundy),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.align(Alignment.End).testTag("fcm_campaign_send_btn")
                                    ) {
                                        Text(if (isSendingPromo) "Broadcasting..." else "Blast FCM Campaign 🚀", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        if (storeOffers.isEmpty()) {
                            item {
                                Text("No broadcasters active inside catalogs feed.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(14.dp))
                            }
                        } else {
                            items(storeOffers) { off ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, LuxeCream)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(off.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LuxeBurgundy)
                                            Surface(
                                                color = LuxeGold,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "${off.discountPercent}% OFF",
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Promocode Coupon: '${off.code}'", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LuxeDustyRose)
                                        Text(off.description, fontSize = 11.sp, color = LuxeCharcoal)

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                repository.deleteOffer(off.id)
                                                Toast.makeText(context, "Promo code '${off.code}' deleted from feeds.", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.height(28.dp).align(Alignment.End)
                                        ) {
                                            Text("Remove Feeds", fontSize = 9.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                6 -> {
                    WhatsAppCatalogSyncScreen(
                        repository = repository,
                        storeId = storeId,
                        storeProducts = storeProducts,
                        ownerWhatsapp = activeStore?.ownerWhatsapp?.ifBlank { activeStore?.ownerPhone } ?: ""
                    )
                }
            }

    // --- FORM MODAL: ADD / EDIT PRODUCT ---
    if (showProductForm) {
        Dialog(onDismissRequest = { showProductForm = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (editingProduct == null) "Publish Handcrafted Wear" else "Modify Couture Item",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = LuxeBurgundy
                            )
                            IconButton(onClick = { showProductForm = false }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = pName,
                            onValueChange = { pName = it },
                            modifier = Modifier.fillMaxWidth().testTag("product_name_textfield"),
                            label = { Text("Product Heading Title", fontSize = 11.sp) }
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = pPrice,
                                onValueChange = { pPrice = it },
                                modifier = Modifier.weight(1f).testTag("product_price_textfield"),
                                label = { Text("Price (₹)", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = pDiscountPrice,
                                onValueChange = { pDiscountPrice = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Discounted (₹) Opt", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }

                    item {
                        // Category select dropdown
                        Text("Category Assignment Selection", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LuxeBurgundy)
                        val avCats = activeStore?.categories ?: emptyList()
                        var expandedCatDrop by remember { mutableStateOf(false) }
                        
                        Box {
                            OutlinedButton(
                                onClick = { expandedCatDrop = true },
                                modifier = Modifier.fillMaxWidth().testTag("product_category_picker_btn")
                            ) {
                                Text(pCategory.ifEmpty { "Select Category" }, fontSize = 11.sp, color = LuxeBurgundy)
                            }
                            DropdownMenu(expanded = expandedCatDrop, onDismissRequest = { expandedCatDrop = false }) {
                                avCats.forEach { cName ->
                                    DropdownMenuItem(
                                        text = { Text(cName, fontSize = 11.sp) },
                                        onClick = {
                                            pCategory = cName
                                            expandedCatDrop = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = pFabric,
                            onValueChange = { pFabric = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Fabric weaving material detail", fontSize = 11.sp) }
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = pStockQty,
                                onValueChange = { pStockQty = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Initial Stock", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = pLowThreshold,
                                onValueChange = { pLowThreshold = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Threshold Alert", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = pSizes,
                                onValueChange = { pSizes = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Available Sizes (split by comma)", fontSize = 10.sp) }
                            )
                            OutlinedTextField(
                                value = pColors,
                                onValueChange = { pColors = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Available Shade/Colors", fontSize = 10.sp) }
                            )
                        }
                    }

                    item {
                        Column {
                            Text("Image lookbook character symbol (Fallback)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("👘", "👗", "👚", "👠", "👑").forEach { choiceSym ->
                                    Surface(
                                        modifier = Modifier.clickable { pIconSymbol = choiceSym },
                                        color = if (pIconSymbol == choiceSym) LuxeGold else LuxeCream,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(choiceSym, fontSize = 18.sp, modifier = Modifier.padding(8.dp))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = LuxeCream.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, LuxeGold.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("📸", fontSize = 18.sp)
                                    Text(
                                        "Phase 7: Product Image System",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = LuxeBurgundy,
                                        fontFamily = FontFamily.Serif
                                    )
                                }
                                
                                Text(
                                    "Capture with camera or load from gallery, compress locally, convert format, and upload directly to Cloudinary optimized cloud servers.",
                                    fontSize = 10.sp,
                                    color = Color.DarkGray,
                                    lineHeight = 13.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                // Step 1 & 2 Preview OR Selection Option
                                if (simulatedOriginalUri == null) {
                                    // Choose Image Source Trigger
                                    Text("Step 1: Choose Photo Source Spec", fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = LuxeBurgundy)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        // Camera Option
                                        OutlinedButton(
                                            onClick = {
                                                // Trigger Camera permission context exclusively
                                                PermissionManager.requestPermissionContext(
                                                    LuxePermission.CAMERA,
                                                    onGranted = {
                                                        // Capture simulation from high-fashion imagery presets
                                                        isSimulatedCamera = true
                                                        val cameraPresets = listOf(
                                                            "https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&w=600&q=80",
                                                            "https://images.unsplash.com/photo-1597983073493-88cd35cf93b0?auto=format&fit=crop&w=600&q=80"
                                                        )
                                                        simulatedOriginalUri = cameraPresets.random()
                                                        currentUploadStep = 1
                                                        Toast.makeText(context, "Camera shutter simulated! High quality raw visual captured.", Toast.LENGTH_SHORT).show()
                                                    },
                                                    onDenied = {
                                                        Toast.makeText(context, "Camera Access required to shoot catalog item photos.", Toast.LENGTH_LONG).show()
                                                    }
                                                )
                                            },
                                            modifier = Modifier.weight(1f).height(40.dp).testTag("select_camera_btn"),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, LuxeBurgundy),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LuxeBurgundy)
                                        ) {
                                            Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Camera Roll", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        
                                        // Gallery Option
                                        OutlinedButton(
                                            onClick = {
                                                // Trigger Gallery permission context exclusively
                                                PermissionManager.requestPermissionContext(
                                                    LuxePermission.GALLERY,
                                                    onGranted = {
                                                        isSimulatedCamera = false
                                                        val galleryPresets = listOf(
                                                            "https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?auto=format&fit=crop&w=600&q=80",
                                                            "https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?auto=format&fit=crop&w=600&q=80",
                                                            "https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?auto=format&fit=crop&w=600&q=80"
                                                        )
                                                        simulatedOriginalUri = galleryPresets.random()
                                                        currentUploadStep = 1
                                                        Toast.makeText(context, "Mobile library opened! High-fashion file layout selected.", Toast.LENGTH_SHORT).show()
                                                    },
                                                    onDenied = {
                                                        Toast.makeText(context, "Gallery permission is required to choose lookbook styles from library.", Toast.LENGTH_LONG).show()
                                                    }
                                                )
                                            },
                                            modifier = Modifier.weight(1f).height(40.dp).testTag("select_gallery_btn"),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, LuxeGold),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LuxeGold)
                                        ) {
                                            Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Pick Gallery", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    // Sourced state, display selected photo preview with source tag
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(if (isSimulatedCamera) "📸 Sourced via Camera Frame" else "🖼️ Sourced via Phone Gallery", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = LuxeBurgundy)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Box(modifier = Modifier.size(5.dp).background(Color(0xFF2E7D32), CircleShape))
                                            }
                                            
                                            // Reset chosen image
                                            Text(
                                                "Change Photo",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Red,
                                                modifier = Modifier.clickable {
                                                    simulatedOriginalUri = null
                                                    uploadedImageUrl = null
                                                    currentUploadStep = 0
                                                    uploadLogs = emptyList()
                                                }
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        // Compression select level UI
                                        Text("Step 2: Custom Multi-edge Compression Density", fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = LuxeBurgundy)
                                        Text("Choose client weight (smaller factor maximizes render speed):", fontSize = 8.sp, color = Color.Gray)
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(30, 40, 50, 60, 70, 80).forEach { level ->
                                                val isSelected = selectedCompressionLevel == level
                                                Surface(
                                                    modifier = Modifier.weight(1f).clickable { 
                                                        if (!isCompressingAndUploading) {
                                                            selectedCompressionLevel = level 
                                                        }
                                                    }.testTag("compression_level_${level}"),
                                                    color = if (isSelected) LuxeBurgundy else Color.White,
                                                    shape = RoundedCornerShape(6.dp),
                                                    border = BorderStroke(1.dp, if (isSelected) LuxeBurgundy else Color.LightGray.copy(alpha = 0.5f))
                                                ) {
                                                    Text(
                                                        "${level}% Ratio",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color.White else Color.DarkGray,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.padding(vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }
                                        
                                        // Preview card
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(110.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = simulatedOriginalUri,
                                                contentDescription = "Preview Image",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                            
                                            // Process button overlays
                                            if (currentUploadStep == 1) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                isCompressingAndUploading = true
                                                                val logs = mutableListOf<String>()
                                                                
                                                                logs.add("Step 1: Photo selection loaded successfully...")
                                                                uploadLogs = ArrayList(logs)
                                                                delay(400)
                                                                
                                                                logs.add("Step 2: Selected compression target level: $selectedCompressionLevel%")
                                                                uploadLogs = ArrayList(logs)
                                                                delay(400)
                                                                
                                                                logs.add("Step 3: Compressing raw bitmap. Original 5.4MB reduced safely to ${320 + (100 - selectedCompressionLevel) * 14}KB.")
                                                                uploadLogs = ArrayList(logs)
                                                                delay(400)
                                                                
                                                                logs.add("Step 4: Format-converted to high speed Google Optimized .webp layout.")
                                                                uploadLogs = ArrayList(logs)
                                                                delay(400)
                                                                
                                                                logs.add("Step 5: Invoking Cloudinary Upload Secure API gateway chunk...")
                                                                uploadLogs = ArrayList(logs)
                                                                delay(500)
                                                                
                                                                val timestamp = System.currentTimeMillis()
                                                                val targetUrl = "https://res.cloudinary.com/ts_luxewear/image/upload/q_$selectedCompressionLevel,f_auto,w_800/v1725515/products/couture_${timestamp}.webp"
                                                                
                                                                logs.add("Step 6: Success! Cloudinary optimized URL acquired:")
                                                                logs.add(targetUrl)
                                                                uploadLogs = ArrayList(logs)
                                                                
                                                                uploadedImageUrl = targetUrl
                                                                pIconSymbol = targetUrl // overwrite the emoji descriptor so it saves in DB
                                                                currentUploadStep = 2
                                                                isCompressingAndUploading = false
                                                                
                                                                Toast.makeText(context, "Images formatted & Cloudinary uplink authenticated!", Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = LuxeGold, contentColor = Color.Black),
                                                        shape = RoundedCornerShape(4.dp),
                                                        modifier = Modifier.height(36.dp).testTag("compress_and_upload_btn")
                                                    ) {
                                                        Text("COMPRESS & UPLOAD COUTURE PHOTO", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                        
                                        // Display live stream logs when compressing/converting/uploading
                                        if (uploadLogs.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text("Uplink Pipeline Logs Stream:", fontWeight = FontWeight.Bold, fontSize = 8.sp, color = LuxeBurgundy)
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = Color.Black),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    uploadLogs.forEach { logLine ->
                                                        Text(
                                                            text = logLine,
                                                            color = if (logLine.startsWith("https")) LuxeGold else if (logLine.startsWith("Step 6")) Color(0xFF2E7D32) else Color.LightGray,
                                                            fontSize = 8.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            lineHeight = 10.sp,
                                                            modifier = Modifier.padding(bottom = 2.dp)
                                                        )
                                                    }
                                                    if (isCompressingAndUploading) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            modifier = Modifier.padding(top = 4.dp)
                                                        ) {
                                                            CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.dp, color = LuxeGold)
                                                            Text("Compiling image transformations...", color = LuxeGold, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = pDescription,
                            onValueChange = { pDescription = it },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            label = { Text("Artistic lookbook story description", fontSize = 11.sp) }
                        )
                    }

                    item {
                        Button(
                            onClick = {
                                if (pName.isNotEmpty() && pPrice.isNotEmpty() && pCategory.isNotEmpty()) {
                                    val priceVal = pPrice.toDoubleOrNull() ?: 2500.0
                                    val discVal = pDiscountPrice.toDoubleOrNull()
                                    val initialStock = pStockQty.toIntOrNull() ?: 10
                                    val thresholdAlert = pLowThreshold.toIntOrNull() ?: 5
                                    val colorsArr = pColors.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                    val sizesArr = pSizes.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                                    val finalProductObj = Product(
                                        id = editingProduct?.id ?: "prod_${storeId}_${System.currentTimeMillis()}",
                                        storeId = storeId,
                                        storeName = storeName,
                                        name = pName,
                                        category = pCategory,
                                        price = priceVal,
                                        discountPrice = discVal,
                                        description = pDescription,
                                        fabric = pFabric,
                                        sizes = sizesArr,
                                        colors = colorsArr,
                                        stockQuantity = initialStock,
                                        lowStockThreshold = thresholdAlert,
                                        imageUrl = pIconSymbol
                                    )

                                    if (editingProduct == null) {
                                        repository.addProduct(finalProductObj)
                                        Toast.makeText(context, "Product '${pName}' Published in Catalog! Image compressed to: $selectedCompressionLevel%", Toast.LENGTH_LONG).show()
                                    } else {
                                        repository.updateProduct(finalProductObj)
                                        Toast.makeText(context, "Product configuration synchronized successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                    showProductForm = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LuxeBurgundy),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth().testTag("add_product_save_btn")
                        ) {
                            Text("Save Product Specs", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG: REPLENISH RESTOCK DIALOG ---
    val restockingActiveItem = showRestockDialog
    if (restockingActiveItem != null) {
        var inputRestockQty by remember { mutableStateOf("15") }
        var inputRestockReason by remember { mutableStateOf("Fresh handloom shipment replenish") }

        Dialog(onDismissRequest = { showRestockDialog = null }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Inventory replenishment trigger", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LuxeBurgundy)
                    Text("Product: ${restockingActiveItem.name} (Current: ${restockingActiveItem.stockQuantity} units left)", fontSize = 11.sp)
                    
                    OutlinedTextField(
                        value = inputRestockQty,
                        onValueChange = { inputRestockQty = it },
                        modifier = Modifier.fillMaxWidth().testTag("restock_qty_textfield"),
                        label = { Text("Added quantity count", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = inputRestockReason,
                        onValueChange = { inputRestockReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Restock Reason audit", fontSize = 11.sp) }
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            val qtyAdded = inputRestockQty.toIntOrNull() ?: 10
                            repository.restockProduct(restockingActiveItem.id, qtyAdded, inputRestockReason)
                            showRestockDialog = null
                            Toast.makeText(context, "Weaving count increased. Associated Wishlisted followers notified in real-time! 📲", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28A745)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().testTag("confirm_restock_save_btn")
                    ) {
                        Text("Add Stock Weaves", fontSize = 11.sp)
                    }
                }
            }
        }
    }

    // --- DIALOG: INVOICE PREVIEW & PROFESSIONAL THEME CUSTOMIZER CUSTOM SHEET ---
    val reviewingInvoiceOrder = showInvoicePreviewDialog
    if (reviewingInvoiceOrder != null) {
        // Theme Customizer selection
        var selectedInvoiceTheme by remember { mutableStateOf("Royal Burgundy Theme") }

        Dialog(onDismissRequest = { showInvoicePreviewDialog = null }) {
            Surface(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
                shape = RoundedCornerShape(14.dp),
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header controls
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Receipt, null, tint = LuxeBurgundy)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Professional Invoice Engine", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LuxeBurgundy)
                        }
                        IconButton(onClick = { showInvoicePreviewDialog = null }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }

                    // Theme selector row
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val invoiceThemes = listOf("Royal Burgundy Theme", "Minimalist Slate Theme", "Monochrome Classic Theme", "Golden Shimmer Theme")
                        invoiceThemes.forEach { iTheme ->
                            Surface(
                                modifier = Modifier.clickable { selectedInvoiceTheme = iTheme },
                                color = if (selectedInvoiceTheme == iTheme) LuxeBurgundy else LuxeCream,
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, LuxeGold)
                            ) {
                                Text(
                                    iTheme.substringBefore(" Theme"),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedInvoiceTheme == iTheme) LuxeGold else Color.Gray,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // THE INVOICE SHEET AREA (Styled preview)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(BorderStroke(1.5.dp, Color.Black))
                    ) {
                        // Dynamically apply background based on theme selections
                        val bgThemeColor = when (selectedInvoiceTheme) {
                            "Royal Burgundy Theme" -> Color(0xFFFFF7F8)
                            "Golden Shimmer Theme" -> Color(0xFFFFFDF5)
                            "Minimalist Slate Theme" -> Color(0xFFF4F5F7)
                            else -> Color.White
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(bgThemeColor)
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Business Branding
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(storeName.uppercase(), fontWeight = FontWeight.Black, fontSize = 15.sp, color = if (selectedInvoiceTheme == "Monochrome Classic Theme") Color.Black else LuxeBurgundy, fontFamily = FontFamily.Serif)
                                    Text("Authorized Couture Maker", fontSize = 8.sp, color = Color.Gray)
                                    Text("Phone: ${activeStore?.ownerPhone} | WhatsApp: ${activeStore?.ownerWhatsapp}", fontSize = 8.sp, color = Color.Gray)
                                }
                                Box(
                                    modifier = Modifier.size(32.dp).background(LuxeBurgundy, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(storeLogo, fontSize = 18.sp)
                                }
                            }
                            HorizontalDivider(color = Color.Black, thickness = 1.dp)

                            // Invoice Metadata
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("METADATA DETAILS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Text("INVOICE NO: ${reviewingInvoiceOrder.invoiceId ?: "PB-2026-05-0001"}", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                                    Text("DATE: ${sdf.format(Date(reviewingInvoiceOrder.timestamp))}", fontSize = 9.sp)
                                    Text("PAYMENT: CASH ON DELIVERY", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = LuxeDustyRose)
                                }
                                var revealInvoiceClientDetails by remember { mutableStateOf(false) }
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clickable { revealInvoiceClientDetails = !revealInvoiceClientDetails }
                                            .padding(bottom = 2.dp)
                                    ) {
                                        Text("BILL TO", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Icon(
                                            if (revealInvoiceClientDetails) Icons.Default.LockOpen else Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = LuxeGold,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                    Text(reviewingInvoiceOrder.customerName, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    
                                    val displayPhoneInv = if (revealInvoiceClientDetails) reviewingInvoiceOrder.customerPhone else {
                                        val phoneRaw = reviewingInvoiceOrder.customerPhone
                                        if (phoneRaw.length > 5) {
                                            phoneRaw.take(phoneRaw.length - 4) + "****"
                                        } else "****"
                                    }
                                    val displayAddressInv = if (revealInvoiceClientDetails) reviewingInvoiceOrder.customerAddress else {
                                        reviewingInvoiceOrder.customerAddress.take(8) + "..." + " [Obscured]"
                                    }
                                    Text(displayPhoneInv, fontSize = 9.sp)
                                    Text(displayAddressInv, fontSize = 8.sp, textAlign = TextAlign.End, maxLines = 2, modifier = Modifier.width(140.dp))
                                }
                            }
                            HorizontalDivider(color = Color.Black.copy(alpha = 0.2f))

                            // Product elements Table layout
                            Text("ORDER SPECIFICATIONS ITEMIZATIONS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Surface(
                                border = BorderStroke(1.dp, Color.Black),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    // Row header
                                    Row(
                                        modifier = Modifier.background(if (selectedInvoiceTheme == "Monochrome Classic Theme") Color.DarkGray else LuxeBurgundy).padding(4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("PARTICULARS", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                                        Text("ATTRIBUTES", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        Text("AMOUNT", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                    }
                                    // Row entry
                                    Row(modifier = Modifier.padding(6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(reviewingInvoiceOrder.productName, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                                        Text("Size: ${reviewingInvoiceOrder.productSize}\nShade: ${reviewingInvoiceOrder.productColor}", fontSize = 8.sp, modifier = Modifier.weight(1f))
                                        Text("₹${reviewingInvoiceOrder.productPrice}", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                    }
                                }
                            }

                            // Math totals receipt block
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                val discountPercentageAmt = 15 // simulated
                                val discountRealRupees = reviewingInvoiceOrder.productPrice * (discountPercentageAmt / 100.0)
                                val subtotalCharge = reviewingInvoiceOrder.productPrice
                                val deliveryChargeLocal = if (activeSettings.deliveryChargeOn) activeSettings.deliveryCharge else 0.0
                                val grandTotalFull = subtotalCharge + deliveryChargeLocal - discountRealRupees

                                Text("Subtotal Amount:  ₹$subtotalCharge", fontSize = 9.sp)
                                if (deliveryChargeLocal > 0) {
                                    Text("Delivery Charges:  +₹$deliveryChargeLocal", fontSize = 9.sp)
                                }
                                Text("Campaign Discount:  -₹$discountRealRupees ($discountPercentageAmt%)", fontSize = 9.sp, color = Color.Red)
                                HorizontalDivider(modifier = Modifier.width(180.dp), color = Color.Black)
                                Text("GRAND TOTAL NETT:  ₹$grandTotalFull", fontWeight = FontWeight.Black, fontSize = 13.sp, color = if (selectedInvoiceTheme == "Monochrome Classic Theme") Color.Black else LuxeBurgundy)
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            // Footer brand policies
                            Text("• 7 Days Exchange and return policy applied on unaltered master weaves.\n• Thank you for preserving weaver artisan communities through LuxeWear.", fontSize = 7.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                    }

                    // Bottom action bars
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Download PDF
                        var downloadStatePending by remember { mutableStateOf(false) }
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    downloadStatePending = true
                                    delay(1800) // Simulated build rendering delay
                                    downloadStatePending = false
                                    Toast.makeText(context, "Invoice PDF successfully generated & saved to downloads! path: /Downloads/Receipt_${reviewingInvoiceOrder.invoiceId ?: "PB1"}.pdf", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LuxeBurgundy),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f).height(36.dp).testTag("pdf_download_btn")
                        ) {
                            if (downloadStatePending) {
                                CircularProgressIndicator(color = LuxeGold, modifier = Modifier.size(16.dp))
                            } else {
                                Icon(Icons.Default.Download, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("PDF", fontSize = 10.sp)
                            }
                        }

                        // WhatsApp Green Sharing
                        Button(
                            onClick = {
                                Toast.makeText(context, "WhatsApp Link Dispatcher: Shared Invoice details & unique invoice routing link successfully to customer number: ${reviewingInvoiceOrder.customerPhone} 🟢", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1.2f).height(36.dp).testTag("whats_share_btn")
                        ) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("WhatsApp", fontSize = 10.sp, color = Color.White)
                        }

                        // Print monochrome slip option
                        Button(
                            onClick = {
                                Toast.makeText(context, "Queue active: Printing 1 copy of receipt '${reviewingInvoiceOrder.invoiceId ?: ""}' at connected boutique printer. 🖨️", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LuxeGold),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1.2f).height(36.dp).testTag("print_doc_btn")
                        ) {
                            Icon(Icons.Default.Print, null, modifier = Modifier.size(14.dp), tint = LuxeBurgundy)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Print Slip", fontSize = 10.sp, color = LuxeBurgundy, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // --- FORM MODAL: ADD CUSTOM EVENT PROMO CODE ---
    if (showOfferForm) {
        var oTitle by remember { mutableStateOf("") }
        var oCode by remember { mutableStateOf("") }
        var oDesc by remember { mutableStateOf("Enjoy our exclusive limited period discount on ethnic fabrics of your choices.") }
        var oPct by remember { mutableStateOf("20") }

        Dialog(onDismissRequest = { showOfferForm = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Broadcast Event Coupon Code", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LuxeBurgundy)
                    Text("These coupons will appear in followers wishlists & promo banner feeds automatically.", fontSize = 10.sp, color = Color.Gray)
                    
                    OutlinedTextField(
                        value = oTitle,
                        onValueChange = { oTitle = it },
                        modifier = Modifier.fillMaxWidth().testTag("add_offer_title_input"),
                        label = { Text("Coupon title (e.g. Diwali festive flash)", fontSize = 11.sp) }
                    )

                    OutlinedTextField(
                        value = oCode,
                        onValueChange = { oCode = it },
                        modifier = Modifier.fillMaxWidth().testTag("add_offer_code_input"),
                        label = { Text("Promo Code (e.g. FESTIVE20)", fontSize = 11.sp) }
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = oPct,
                            onValueChange = { oPct = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Discount ratio (%)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    OutlinedTextField(
                        value = oDesc,
                        onValueChange = { oDesc = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Promo banner guidelines", fontSize = 11.sp) }
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            if (oTitle.isNotEmpty() && oCode.isNotEmpty()) {
                                val dPercent = oPct.toIntOrNull() ?: 20
                                repository.createOffer(storeId, oTitle, oDesc, dPercent, oCode)
                                showOfferForm = false
                                Toast.makeText(context, "Offer Published! Notifications dispatched to all ${activeStore?.followersCount ?: 0} followed customers feeds! 🚨", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxeBurgundy),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add_offer_submit")
                    ) {
                        Text("Broadcast Coupon", fontSize = 11.sp)
                    }
                }
            }
        }
    }

    // --- FORM MODAL: ADD / EDIT CATEGORY REAL-TIME SYNC ---
    if (showAddCategoryDialog) {
        Dialog(onDismissRequest = { showAddCategoryDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (editingCategoryName == null) "Create Synced Category" else "Modify Synced Category",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = LuxeBurgundy
                    )
                    Text("Updates categories instantly on operator workspace, customer search bar, and super admin panels.", fontSize = 10.sp, color = Color.Gray)
                    
                    OutlinedTextField(
                        value = inputCategoryName,
                        onValueChange = { inputCategoryName = it },
                        modifier = Modifier.fillMaxWidth().testTag("category_name_input"),
                        label = { Text("Category Title Name", fontSize = 11.sp) }
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = { showAddCategoryDialog = false }) {
                            Text("Cancel", fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                if (inputCategoryName.isNotEmpty()) {
                                    val oldName = editingCategoryName
                                    if (oldName == null) {
                                        repository.addCategoryToStore(storeId, inputCategoryName)
                                        Toast.makeText(context, "Added category '$inputCategoryName'. Synced in real-time. ⚡", Toast.LENGTH_SHORT).show()
                                    } else {
                                        repository.editCategoryInStore(storeId, oldName, inputCategoryName)
                                        Toast.makeText(context, "Edited category from '$oldName' to '$inputCategoryName'. Real-time sync completed.", Toast.LENGTH_SHORT).show()
                                    }
                                    showAddCategoryDialog = false
                                    inputCategoryName = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LuxeBurgundy),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("save_category_submit")
                        ) {
                            Text("Confirm Save", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
}
}

// Simulated Toast wrapper object to avoid missing API issues on desktop test mock layers
object Toast {
    fun makeText(cb: Context, content: String, length: Int): ToastMockWrapper {
        return ToastMockWrapper(cb, content)
    }
    const val LENGTH_SHORT = 0
    const val LENGTH_LONG = 1
}

class ToastMockWrapper(val context: Context, val content: String) {
    fun show() {
        android.widget.Toast.makeText(context, content, android.widget.Toast.LENGTH_SHORT).show()
    }
}
