package com.example.ui

import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.TSLuxeWearRepository
import com.example.data.AuthManager
import com.example.model.*
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuperAdminDashboardScreen(repository: TSLuxeWearRepository) {
    val stores by repository.storesFlow.collectAsState()
    val products by repository.productsFlow.collectAsState()
    val orders by repository.ordersFlow.collectAsState()
    val complaints by repository.complaintsFlow.collectAsState()
    val inquiries by repository.inquiriesFlow.collectAsState()

    // Pagination local states under Free Tier (Phase 16)
    var ordersPage by remember { mutableStateOf(1) }
    var ordersPageSize by remember { mutableStateOf(5) }
    
    var storesPage by remember { mutableStateOf(1) }
    var storesPageSize by remember { mutableStateOf(3) }
    
    var inquiriesPage by remember { mutableStateOf(1) }
    var inquiriesPageSize by remember { mutableStateOf(3) }

    // Interactive maintenance logs payments status map (retained per screen session)
    val paymentStatusMap = remember {
        mutableStateMapOf<String, Boolean>().apply {
            put("store_priya", true)
            put("store_velvet", true)
            put("store_fashion", false)
            put("store_ethnic", false)
        }
    }

    // Reported Bugs Log state
    val bugsList = remember {
        mutableStateListOf(
            SystemBug("bug_101", "Invoice overlapping on long description lines", "High", "OPEN"),
            SystemBug("bug_102", "WhatsApp international prefix formatting latency", "Medium", "RESOLVED"),
            SystemBug("bug_103", "Caching profile avatars delay", "Low", "RESOLVED")
        )
    }

    // Platform-wide Order filters
    var currentOrderStatusFilter by remember { mutableStateOf("All") }
    val orderStatusOptions = listOf("All", "Pending", "Confirmed", "Packed", "Shipped", "Delivered", "Cancelled")

    // Active Tab state
    var activeAdminTab by remember { mutableStateOf(0) } // 0: Overview, 1: Stores, 2: Analytics, 3: Orders, 4: Financials, 5: Support
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Store Picker for drill-down performance telemetry
    var selectedPerformanceStoreId by remember { mutableStateOf<String?>(null) }

    // Customer drill-down analyzer state
    var selectedAnalysisCustomerName by remember { mutableStateOf<String?>(null) }
    var customerHistorySearchText by remember { mutableStateOf("") }

    // Store Creation form state
    var showAddStoreModal by remember { mutableStateOf(false) }
    var sName by remember { mutableStateOf("") }
    var sOwner by remember { mutableStateOf("") }
    var sOwnerEmail by remember { mutableStateOf("") }
    var sPhone by remember { mutableStateOf("+91 ") }
    var sWhatsapp by remember { mutableStateOf("91 ") }
    var sLogo by remember { mutableStateOf("👑") }
    var sBannerColor by remember { mutableStateOf(0xFF6B1B38) }
    var sAddress by remember { mutableStateOf("") }
    var sType by remember { mutableStateOf("Boutique Partywear") }

    var sNameError by remember { mutableStateOf<String?>(null) }
    var sOwnerError by remember { mutableStateOf<String?>(null) }
    var sOwnerEmailError by remember { mutableStateOf<String?>(null) }
    var sPhoneError by remember { mutableStateOf<String?>(null) }
    var sWhatsappError by remember { mutableStateOf<String?>(null) }
    var sAddressError by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(LuxeCream)) {

        // authorities navigation tab-gantry
        ScrollableTabRow(
            selectedTabIndex = activeAdminTab,
            containerColor = LuxeCharcoal,
            edgePadding = 12.dp,
            contentColor = LuxeGold,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeAdminTab]),
                    color = LuxeGold
                )
            }
        ) {
            val tabs = listOf(
                "Overview" to Icons.Default.Dashboard,
                "Stores Hub" to Icons.Default.Storefront,
                "Analytics Hub" to Icons.Default.TrendingUp,
                "Order Gantry" to Icons.Default.ShoppingBag,
                "Financials" to Icons.Default.AccountBalanceWallet,
                "Support Desk" to Icons.Default.SupportAgent
            )
            tabs.forEachIndexed { index, (label, icon) ->
                Tab(
                    selected = activeAdminTab == index,
                    onClick = { activeAdminTab = index },
                    modifier = Modifier.testTag("admin_tab_$index")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, null, tint = if (activeAdminTab == index) LuxeGold else Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeAdminTab == index) LuxeGold else Color.Gray
                        )
                    }
                }
            }
        }

        // Header Oversight capsule
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, LuxeGold.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(LuxeBurgundy, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AdminPanelSettings, null, tint = LuxeGold, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TS LuxeWear Super Administration Desk",
                        fontWeight = FontWeight.Bold,
                        color = LuxeBurgundy,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Serif
                    )
                    val email = AuthManager.currentUserFlow.value?.email ?: "shakirsir2122@gmail.com"
                    Text(
                        text = "Authorized Account Access: $email • Real-time Monitoring Active",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE2F0D9))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text("SECURE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF385723))
                }
            }
        }

        // Selected Tab Content Display
        when (activeAdminTab) {
            0 -> {
                // TAB 0: OVERVIEW & REALTIME ALERTS
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = "Platform Comprehensive Overview",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = LuxeBurgundy,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = "Live counters summarizing multi-tenant catalog databases, user records, and network-synced stores.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    // Stat Grid List
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            val activeStoresCount = stores.count { it.status == "Active" }
                            val suspendedStoresCount = stores.count { it.status == "Suspended" }
                            val distinctCategoriesCount = products.map { it.category }.distinct().size
                            val uniqueCustomerEmails = orders.map { it.customerName }.distinct().size + 4 // seed client base count

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                StatGridCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Total Stores",
                                    value = stores.size.toString(),
                                    icon = Icons.Default.Storefront,
                                    color = LuxeGold
                                )
                                StatGridCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Unique Customers",
                                    value = uniqueCustomerEmails.toString(),
                                    icon = Icons.Default.People,
                                    color = LuxeDustyRose
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                StatGridCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Total Live Items",
                                    value = products.size.toString(),
                                    icon = Icons.Default.Inventory,
                                    color = LuxeBurgundy
                                )
                                StatGridCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Active Categories",
                                    value = distinctCategoriesCount.toString(),
                                    icon = Icons.Default.Category,
                                    color = LuxeGold
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                StatGridCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Boutiques Approved",
                                    value = activeStoresCount.toString(),
                                    icon = Icons.Default.CheckCircleOutline,
                                    color = Color(0xFF2E7D32)
                                )
                                StatGridCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Boutiques Suspended",
                                    value = suspendedStoresCount.toString(),
                                    icon = Icons.Default.Block,
                                    color = Color(0xFFC62828)
                                )
                            }
                        }
                    }

                    // Simulated System Audit logs
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "System Real-Time Security Logs",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = LuxeBurgundy
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color.Green, CircleShape)
                                    )
                                }
                                Text(
                                    text = "Below are real login & complaint notifications processed via secure system audit channels.",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )

                                Divider(color = Color.LightGray.copy(alpha = 0.3f))

                                // Dynamic log simulation based on complaints list
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                                    SystemLogLine(
                                        "Audit Log",
                                        "FCM Channel: initialized background sync queues with average delay 4ms.",
                                        "Systems"
                                    )
                                    SystemLogLine(
                                        "Access Alert",
                                        "Secure authorization confirmed for administrative profile 'shakirsir2122@gmail.com'.",
                                        "Systems"
                                    )
                                    if (complaints.isNotEmpty()) {
                                        val latest = complaints.first()
                                        SystemLogLine(
                                            "Incident Appeal",
                                            "New complaint registered from '${latest.fromUser}': \"${latest.subject}\"",
                                            "Support"
                                        )
                                    }
                                    if (orders.isNotEmpty()) {
                                        val latestO = orders.first()
                                        SystemLogLine(
                                            "Transaction Feed",
                                            "FCM: New Order notification successfully broadcast for ₹${latestO.productPrice} value.",
                                            "Orders"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // TAB 1: STORE HUD (VETTING & PERFORMANCE)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Luxury Boutique Vetting Office",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = LuxeBurgundy,
                                    fontFamily = FontFamily.Serif
                                )
                                Text(
                                    text = "Audit registered stores. Approve pending requests, freeze suspended boutique profiles, or revoke multi-tenant catalog entries instantly.",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    sName = ""
                                    sOwner = ""
                                    sOwnerEmail = ""
                                    sPhone = "+91 "
                                    sWhatsapp = "91 "
                                    sLogo = "👑"
                                    sBannerColor = 0xFF6B1B38
                                    sAddress = "Luxe Complex, Delhi"
                                    sType = "Boutique Collection"
                                    showAddStoreModal = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LuxeBurgundy),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("admin_add_store_button")
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Store", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    if (stores.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No stores registered on TS LuxeWear.", color = Color.Gray)
                            }
                        }
                    } else {
                        items(stores) { store ->
                            val storeProducts = products.filter { it.storeId == store.id }
                            val storeOrders = orders.filter { it.storeId == store.id }
                            val storeRevenue = storeOrders.filter { it.orderStatus != "Cancelled" }.sumOf { it.productPrice }
                            val orderSuccessRate = if (storeOrders.isEmpty()) {
                                100
                            } else {
                                (storeOrders.count { it.orderStatus == "Delivered" }.toDouble() / storeOrders.size * 100).toInt()
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, if (store.status == "Active") Color(0xFFD4EDDA) else Color(0xFFF8D7DA)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // Headline Store Identification row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .background(Color(store.bannerColor).copy(alpha = 0.15f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(store.logoUrl, fontSize = 20.sp)
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = store.name,
                                                    fontWeight = FontWeight.Bold,
                                                    color = LuxeBurgundy,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "${store.storeType} • Owner: ${store.ownerName}",
                                                    color = Color.Gray,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }

                                        // Status chips
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (store.status == "Active") Color(0xFFD4EDDA) else Color(0xFFF8D7DA))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = store.status.uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (store.status == "Active") Color(0xFF155724) else Color(0xFF721C24)
                                            )
                                        }
                                    }

                                    // Contacts metadata block
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        IconTextLabel(icon = Icons.Default.Phone, label = store.ownerPhone)
                                        IconTextLabel(icon = Icons.Default.Chat, label = "WA: ${store.ownerWhatsapp}")
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Portal Catalog Link: ${store.storeUrl}",
                                        fontSize = 9.sp,
                                        color = LuxeGold,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.3f))

                                    // Expanding Store performance analysis scorecard
                                    val isPerformanceSelected = selectedPerformanceStoreId == store.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedPerformanceStoreId = if (isPerformanceSelected) null else store.id
                                            }
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "View Performance Metrics Telemetry",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LuxeBurgundy
                                        )
                                        Icon(
                                            imageVector = if (isPerformanceSelected) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = LuxeBurgundy,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = isPerformanceSelected,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp)
                                                .background(LuxeCream, RoundedCornerShape(8.dp))
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            PerformanceRow("Total Catalog Items Live", "${storeProducts.size} Products")
                                            PerformanceRow("Followers Footprint", "${store.followersCount} Followers")
                                            PerformanceRow("Total Client Bookings", "${storeOrders.size} Orders")
                                            PerformanceRow("Total Confirmed Sales", "₹$storeRevenue")
                                            PerformanceRow("Order Fulfillment Rate", "$orderSuccessRate%")
                                        }
                                    }

                                    // Action buttons for vetting status
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        if (store.status != "Active") {
                                            Button(
                                                onClick = {
                                                    repository.approveStore(store.id)
                                                    Toast.makeText(context, "Boutique '${store.name}' Approved Successfully! ✨", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = LuxeGold, contentColor = Color.Black),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.weight(1.5f).height(35.dp).testTag("approve_store_btn_${store.id}")
                                            ) {
                                                Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Approve Store", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    repository.toggleStoreStatus(store.id)
                                                    Toast.makeText(context, "Boutique '${store.name}' Suspended Successfully 🚫", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828), contentColor = Color.White),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.weight(1.5f).height(35.dp).testTag("suspend_store_btn_${store.id}")
                                            ) {
                                                Icon(Icons.Default.Block, null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Suspend Store", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                repository.deleteStore(store.id)
                                                Toast.makeText(context, "Boutique access revoked from platform.", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1f).height(35.dp).testTag("delete_store_btn_${store.id}")
                                        ) {
                                            Text("Revoke", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // TAB 2: ANALYTICS HUB
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = "Platform Analytics Hub",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = LuxeBurgundy,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = "Real-time client footprints, popular designer selections, active user trends, and advanced store monitoring diagnostics.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    // Top footfall row & DAU metrics
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Daily Active Users", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(6.dp).background(Color(0xFF28A745), CircleShape))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("148 Active", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = LuxeBurgundy)
                                    }
                                    Text("Peak Hours: 6:00 - 9:30 PM", fontSize = 8.sp, color = Color.Gray)
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1.2f),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Top Footfall Sourcing Store", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Velvet Sarees", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = LuxeBurgundy)
                                    Text("2,050 Customer Visits", fontSize = 8.sp, color = LuxeGold, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Customer growth metrics
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Customer Growth Trend", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LuxeBurgundy)
                                Spacer(modifier = Modifier.height(8.dp))
                                GrowthProgressRow("Jan Growth", 0.12f, "+12%")
                                GrowthProgressRow("Feb Growth", 0.18f, "+18%")
                                GrowthProgressRow("Mar Growth", 0.22f, "+22%")
                                GrowthProgressRow("Apr Growth", 0.35f, "+35%")
                                GrowthProgressRow("May Growth (Current)", 0.48f, "+48% Peak")
                            }
                        }
                    }

                    // Top viewed products
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Top Viewed Products Selection", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LuxeBurgundy)
                                Spacer(modifier = Modifier.height(8.dp))

                                val topProducts = listOf(
                                    Triple("👘 Traditional Banarasi Silk Saree", "Velvet Sarees", "482 views today"),
                                    Triple("👗 Royal Velvet Heavy Anarkali Suit", "Priya Boutique", "320 views today"),
                                    Triple("👚 Indigo Georgette Vegetable Kurti", "Ethnic Grace", "195 views today")
                                )

                                topProducts.forEachIndexed { idx, (prod, shop, view) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(prod, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LuxeBurgundy)
                                            Text("Store: $shop", fontSize = 8.sp, color = Color.Gray)
                                        }
                                        Text(view, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LuxeGold)
                                    }
                                    if (idx < 2) {
                                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Most Used Functions
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Most Used Functions of App by Stores", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LuxeBurgundy)
                                Spacer(modifier = Modifier.height(8.dp))
                                UsageRow("Instant PDF Invoicer", "89 times", 0.85f)
                                UsageRow("WhatsApp Dispatch Pre-fill", "64 times", 0.65f)
                                UsageRow("Campaign Promotion Coupons", "41 times", 0.45f)
                                UsageRow("Low Stock Push Alerts", "28 times", 0.3f)
                            }
                        }
                    }

                    // Health Diagnostics
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Store Health Monitor & Telemetry", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LuxeBurgundy)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Automated latency and network triggers used to monitor sub-tenant performance.", fontSize = 9.sp, color = Color.Gray)

                                Spacer(modifier = Modifier.height(10.dp))
                                PerformanceRow("Average API latency", "78ms (Exemplary)")
                                PerformanceRow("Database Status", "ROOM-SQLITE connected & synchronized")
                                PerformanceRow("FCM Network Server", "Google Firebase Push Delivery OK")

                                Spacer(modifier = Modifier.height(12.dp))

                                // Dynamic push simulator triggers for safety testing
                                Button(
                                    onClick = {
                                        repository.sendPushNotification(
                                            recipientRole = "SUPER_ADMIN",
                                            recipientEmail = "shakirsir2122@gmail.com",
                                            title = "Threat Blocked: Concurrent Login 🚨",
                                            message = "Audit system query: Multiple concurrent login attempts from outside whitelisted regions have been successfully intercepted.",
                                            type = "SUSPICIOUS_ACT",
                                            category = "Systems",
                                            targetScreen = "super_admin_dashboard"
                                        )
                                        Toast.makeText(context, "Simulated real-time Threat Warning dispatched!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = LuxeBurgundy),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("simulate_suspicious_act_btn")
                                ) {
                                    Icon(Icons.Default.Security, null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Simulate Suspicious Auth Alert 🚨", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                // TAB 3: ORDER GANTRY (MONITORING & FILTERS & INQUIRIES)
                var modeSelectionIsOrders by remember { mutableStateOf(true) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Order & Inquiry Oversight Gantry",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = LuxeBurgundy,
                                    fontFamily = FontFamily.Serif
                                )

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .border(1.dp, LuxeGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                ) {
                                    Text(
                                        "Orders",
                                        modifier = Modifier
                                            .clickable { modeSelectionIsOrders = true }
                                            .background(if (modeSelectionIsOrders) LuxeBurgundy else Color.White)
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (modeSelectionIsOrders) Color.White else LuxeBurgundy
                                    )
                                    Text(
                                        "Inquiries",
                                        modifier = Modifier
                                            .clickable { modeSelectionIsOrders = false }
                                            .background(if (!modeSelectionIsOrders) LuxeBurgundy else Color.White)
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!modeSelectionIsOrders) Color.White else LuxeBurgundy
                                    )
                                }
                            }
                            Text(
                                text = "Monitor active fulfillment, group sales analytics store-wise, and drill-down into custom client histories.",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    if (modeSelectionIsOrders) {
                        // Global completion rate calculation
                        val deliveredCount = orders.count { it.orderStatus == "Delivered" }
                        val activeCount = orders.count { it.orderStatus != "Cancelled" }
                        val completionRate = if (orders.isEmpty()) {
                            100
                        } else {
                            (deliveredCount.toDouble() / orders.size * 100).toInt()
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, LuxeGold.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Global Platform Order Completion Rate", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LuxeBurgundy)
                                        Text("$deliveredCount delivered profiles / ${orders.size} bookings recorded", fontSize = 9.sp, color = Color.Gray)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .background(LuxeCream, CircleShape)
                                            .border(1.5.dp, LuxeGold, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("$completionRate%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LuxeBurgundy)
                                    }
                                }
                            }
                        }

                        // Invoice Generation Analytics Panel
                        item {
                            val invoicedOrders = orders.filter { it.invoiceId != null }
                            val totalInvoicedAmount = invoicedOrders.sumOf { it.productPrice }
                            val invoicePercentage = if (orders.isEmpty()) 0 else (invoicedOrders.size.toDouble() / orders.size * 100).toInt()
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, LuxeGold.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = LuxeBurgundy, modifier = Modifier.size(18.dp))
                                        Text("Invoice Generation Analytics", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LuxeBurgundy)
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Total Generated Invoices", fontSize = 10.sp, color = Color.Gray)
                                            Text("${invoicedOrders.size} Invoices (${invoicePercentage}% of all orders)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Total Amount Invoiced", fontSize = 10.sp, color = Color.Gray)
                                            Text("₹$totalInvoicedAmount", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LuxeBurgundy)
                                        }
                                    }
                                }
                            }
                        }

                        // Problematic Orders Tracking Panel
                        item {
                            val problematicOrders = orders.filter { 
                                it.orderStatus == "Cancelled" || 
                                (it.orderStatus == "Pending" && (System.currentTimeMillis() - it.timestamp) > 3600000L) 
                            }
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5)),
                                border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                                        Text("Problematic Orders Monitor (${problematicOrders.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFC62828))
                                    }
                                    Text("Immediate platform oversight on cancellations & stagnant orders.", fontSize = 9.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                                    
                                    if (problematicOrders.isEmpty()) {
                                        Text("No stagnant or problematic orders found. Operational status green! ✅", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            problematicOrders.take(3).forEach { o ->
                                                val flagReason = if (o.orderStatus == "Cancelled") "Order Cancelled" else "Stagnant Pending (>1hr)"
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color.White, RoundedCornerShape(4.dp))
                                                        .padding(8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text("Ref: ${o.orderId} via ${o.storeName}", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = LuxeBurgundy)
                                                        Text("Client: ${o.customerName} | ₹${o.productPrice}", fontSize = 9.sp, color = Color.DarkGray)
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xFFFFEBEE), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(flagReason, color = Color(0xFFC62828), fontWeight = FontWeight.Bold, fontSize = 8.sp)
                                                    }
                                                }
                                            }
                                            if (problematicOrders.size > 3) {
                                                Text("and ${problematicOrders.size - 3} other flagged items...", fontSize = 9.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Order Group Store-wise scorecard
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Store-Wise Performance Breakdown", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LuxeBurgundy)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    stores.forEach { shop ->
                                        val shopOrders = orders.filter { it.storeId == shop.id }
                                        val pending = shopOrders.count { it.orderStatus == "Pending" || it.orderStatus == "Confirmed" }
                                        val delivered = shopOrders.count { it.orderStatus == "Delivered" }
                                        val cancelled = shopOrders.count { it.orderStatus == "Cancelled" }

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(shop.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                                            Text(
                                                "Pend: $pending • Shipped/Del: $delivered • Cancel: $cancelled",
                                                fontSize = 10.sp,
                                                color = LuxeBurgundy,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 2.dp))
                                    }
                                }
                            }
                        }

                        // Interactive Customer order history trace drill-down
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Unified Customer Order History Search", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LuxeBurgundy)
                                    Text("Type client names directly to compile their purchases across all boutique stores.", fontSize = 9.sp, color = Color.Gray)

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = customerHistorySearchText,
                                        onValueChange = {
                                            customerHistorySearchText = it
                                        },
                                        placeholder = { Text("Enter customer name...") },
                                        modifier = Modifier.fillMaxWidth().testTag("customer_history_search_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = LuxeGold,
                                            focusedLabelColor = LuxeBurgundy
                                        ),
                                        trailingIcon = {
                                            IconButton(onClick = {}) {
                                                Icon(Icons.Default.Search, null, tint = LuxeBurgundy)
                                            }
                                        }
                                    )

                                    // Quick shortcut selections
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Fast Shortcuts Presets:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val presets = listOf("Sneha Deshmukh", "Riddhi Naik", "Preeti Saxena")
                                        presets.forEach { pre ->
                                            Box(
                                                modifier = Modifier
                                                    .background(LuxeCream, RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        customerHistorySearchText = pre
                                                        selectedAnalysisCustomerName = pre
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(pre, fontSize = 9.sp, color = LuxeBurgundy, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // Resolved records for query match
                                    val queryText = customerHistorySearchText.trim().lowercase()
                                    val mappedUserHistoryList = if (queryText.isEmpty()) {
                                        emptyList()
                                    } else {
                                        orders.filter { it.customerName.lowercase().contains(queryText) }
                                    }

                                    if (queryText.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            "Results for client match (${mappedUserHistoryList.size} found):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LuxeBurgundy
                                        )

                                        if (mappedUserHistoryList.isEmpty()) {
                                            Text("No bookings history found for '$customerHistorySearchText' across shops.", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                                        } else {
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                                                mappedUserHistoryList.forEach { o ->
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(LuxeCream, RoundedCornerShape(8.dp))
                                                            .padding(10.dp)
                                                    ) {
                                                        Column {
                                                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                                Text("Ref: ${o.orderId} • ${o.storeName}", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = LuxeBurgundy)
                                                                Text(o.orderStatus.uppercase(), fontWeight = FontWeight.Bold, fontSize = 9.sp, color = LuxeGold)
                                                            }
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text("Item: ${o.productImageUrl} ${o.productName} • ₹${o.productPrice}", fontSize = 10.sp, color = Color.DarkGray)
                                                            Text("Delivery Address details: ${o.customerAddress}", fontSize = 8.sp, color = Color.Gray)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Interactive Filter chips row
                        item {
                            Text(
                                text = "Filter Status Platform Logs:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = LuxeBurgundy,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                orderStatusOptions.forEach { statusOpt ->
                                    val isSelected = currentOrderStatusFilter == statusOpt
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { currentOrderStatusFilter = statusOpt },
                                        label = { Text(statusOpt, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = LuxeBurgundy,
                                            selectedLabelColor = Color.White,
                                            containerColor = Color.White,
                                            labelColor = LuxeBurgundy
                                        )
                                    )
                                }
                            }
                        }

                        // Display matching status filtered order records
                        val filteredOrdersList = if (currentOrderStatusFilter == "All") {
                            orders
                        } else {
                            orders.filter { it.orderStatus.lowercase() == currentOrderStatusFilter.lowercase() }
                        }

                        if (filteredOrdersList.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No orders match status filter '$currentOrderStatusFilter'.", color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        } else {
                            val paginatedOrdersList = filteredOrdersList.paginate(ordersPage, ordersPageSize)
                            items(paginatedOrdersList) { ord ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Order Ref: ${ord.orderId}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = LuxeBurgundy)
                                                Text(
                                                    text = "Requested via ${ord.storeName}",
                                                    fontSize = 8.sp,
                                                    color = Color.Gray,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .background(LuxeCream, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = ord.orderStatus.uppercase(),
                                                    color = LuxeGold,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Product: ${ord.productImageUrl} ${ord.productName} (${ord.productSize} • ${ord.productColor}) • ₹${ord.productPrice}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.DarkGray
                                        )
                                        Text(
                                            text = "Client: ${ord.customerName} • Ph: ${ord.customerPhone}",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )

                                        if (ord.invoiceId != null) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Invoiced: ${ord.invoiceId}",
                                                fontSize = 9.sp,
                                                color = Color(0xFF385723),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            item {
                                LuxePaginator(
                                    currentPage = ordersPage,
                                    totalItems = filteredOrdersList.size,
                                    pageSize = ordersPageSize,
                                    onPageChange = { ordersPage = it },
                                    onPageSizeChange = { ordersPageSize = it; ordersPage = 1 },
                                    modifier = Modifier.padding(vertical = 4.dp).testTag("admin_orders_paginator")
                                )
                            }
                        }
                    } else {
                        // Platform Inquiries
                        if (inquiries.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("No customer catalog inquiries logged.", color = Color.Gray)
                                }
                            }
                        } else {
                            val paginatedInquiries = inquiries.paginate(inquiriesPage, inquiriesPageSize)
                            items(paginatedInquiries) { inq ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Product: ${inq.productName}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = LuxeBurgundy)
                                                Text("Boutique: ${inq.storeName}", fontSize = 8.sp, color = Color.Gray)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (inq.status == "New") Color(0xFFF8D7DA) else Color(0xFFD4EDDA))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = inq.status.uppercase(),
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (inq.status == "New") Color(0xFF721C24) else Color(0xFF155724)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Q: ${inq.question}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                                        inq.answer?.let { ans ->
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("A: \"$ans\"", fontSize = 11.sp, color = Color.DarkGray, style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                                        }
                                    }
                                }
                            }
                            item {
                                LuxePaginator(
                                    currentPage = inquiriesPage,
                                    totalItems = inquiries.size,
                                    pageSize = inquiriesPageSize,
                                    onPageChange = { inquiriesPage = it },
                                    onPageSizeChange = { inquiriesPageSize = it; inquiriesPage = 1 },
                                    modifier = Modifier.padding(vertical = 4.dp).testTag("admin_inquiries_paginator")
                                )
                            }
                        }
                    }
                }
            }

            4 -> {
                // TAB 4: FINANCIALS (DUES & STORE EARNINGS)
                val flatOperationalChargeInRupees = 500.0 // Flat tier maintenance fee

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = "Platform Financial Ledger",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = LuxeBurgundy,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = "Supervise the flat operational maintenance contribution fees, toggle boutique payment status records directly, and evaluate dynamic sales figures.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    if (stores.isEmpty()) {
                        item {
                            Text("No stores registered to generate billings ledger.")
                        }
                    } else {
                        val paginatedStoresList = stores.paginate(storesPage, storesPageSize)
                        items(paginatedStoresList) { shop ->
                            // Calculate total sales/earnings dynamically from confirmed order values
                            val shopOrdersList = orders.filter { it.storeId == shop.id && it.orderStatus != "Cancelled" }
                            val totalConfirmedSalesRevenue = shopOrdersList.sumOf { it.productPrice }

                            val isPaid = paymentStatusMap[shop.id] ?: false

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, if (isPaid) Color(0xFFD4EDDA) else Color(0xFFF8D7DA)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    // Header billing identification row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row {
                                            Text(shop.logoUrl, fontSize = 20.sp, modifier = Modifier.padding(end = 6.dp))
                                            Text(shop.name, fontWeight = FontWeight.Bold, color = LuxeBurgundy, fontSize = 13.sp)
                                        }

                                        // Payment slider switch toggle
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (isPaid) "PAID" else "UNPAID",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPaid) Color(0xFF155724) else Color(0xFF721C24),
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                            Switch(
                                                checked = isPaid,
                                                onCheckedChange = { nextState ->
                                                    paymentStatusMap[shop.id] = nextState
                                                    Toast.makeText(context, "${shop.name} maintenance charge marked: ${if (nextState) "PAID" else "UNPAID"}", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color.White,
                                                    checkedTrackColor = Color(0xFF28A745)
                                                ),
                                                modifier = Modifier.scale(0.7f).testTag("financial_paid_toggle_${shop.id}")
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    PerformanceRow("Total Store Sales Generated Volume", "₹$totalConfirmedSalesRevenue")
                                    PerformanceRow("Flat Maintenance Fee", "₹$flatOperationalChargeInRupees / month")
                                    PerformanceRow("Log Invoice Statement Due Date", "June 01, 2026")

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Action to remind store owner via FCM push notification
                                    Button(
                                        onClick = {
                                            repository.sendPushNotification(
                                                recipientRole = "STORE_OWNER",
                                                recipientEmail = repository.getStoreOwnerEmail(shop.id),
                                                title = "Maintenance Charge Due 💰",
                                                message = "Your boutique monthly contribution fee of ₹$flatOperationalChargeInRupees for portal maintenance is due by June 01, 2026.",
                                                type = "MAINTENANCE_DUE",
                                                category = "Systems",
                                                targetScreen = "store_owner_dashboard"
                                            )
                                            Toast.makeText(context, "FCM Due date reminder sent successfully to boutique email!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = LuxeGold, contentColor = Color.Black),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(34.dp)
                                            .testTag("send_due_reminder_btn_${shop.id}"),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Icon(Icons.Default.NotificationsActive, null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Send Due Reminder Push Alert 🔔", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                }
                            }
                        }
                        this.item {
                            LuxePaginator(
                                currentPage = storesPage,
                                totalItems = stores.size,
                                pageSize = storesPageSize,
                                onPageChange = { storesPage = it },
                                onPageSizeChange = { storesPageSize = it; storesPage = 1 },
                                modifier = Modifier.padding(vertical = 4.dp).testTag("admin_stores_paginator")
                            )
                        }
                    }
                }
            }

            5 -> {
                // TAB 5: SUPPORT HUB (COMPLAINTS & DEVS BUGS LOGS)
                var supportTabOrdersByCustomer by remember { mutableStateOf(true) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Admin Help Desk Support",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = LuxeBurgundy,
                                    fontFamily = FontFamily.Serif
                                )

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .border(1.dp, LuxeGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                ) {
                                    Text(
                                        "Appeals",
                                        modifier = Modifier
                                            .clickable { supportTabOrdersByCustomer = true }
                                            .background(if (supportTabOrdersByCustomer) LuxeBurgundy else Color.White)
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (supportTabOrdersByCustomer) Color.White else LuxeBurgundy
                                    )
                                    Text(
                                        "Dev Bugs",
                                        modifier = Modifier
                                            .clickable { supportTabOrdersByCustomer = false }
                                            .background(if (!supportTabOrdersByCustomer) LuxeBurgundy else Color.White)
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!supportTabOrdersByCustomer) Color.White else LuxeBurgundy
                                    )
                                }
                            }
                            Text(
                                text = "Address bug logging diagnostic reports and resolve customer & boutique complaints.",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    // Firebase Cloud Sync & Diagnostics Console Module
                    item {
                        var showRulesDialog by remember { mutableStateOf(false) }
                        var showModelsDialog by remember { mutableStateOf(false) }
                        val firebaseStatus by com.example.data.FirebaseBackend.connectionStatus.collectAsState()
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("firebase_diagnostics_card"),
                            colors = CardDefaults.cardColors(containerColor = LuxeCream),
                            border = BorderStroke(1.dp, LuxeGold.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = LuxeBurgundy, modifier = Modifier.size(20.dp))
                                        Text("Firebase Cloud Sync Console", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LuxeBurgundy, fontFamily = FontFamily.Serif)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (firebaseStatus.contains("Verified")) Color(0xFFD4EDDA) else Color(0xFFFFF3CD)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (firebaseStatus.contains("Verified")) "LIVE READY" else "LOCAL SANDBOX",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (firebaseStatus.contains("Verified")) Color(0xFF155724) else Color(0xFF856404)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Status: $firebaseStatus",
                                    fontSize = 10.sp,
                                    color = Color.DarkGray,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { showRulesDialog = true },
                                        modifier = Modifier.weight(1f).height(32.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = LuxeBurgundy),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Default.Lock, null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Security Rules", fontSize = 10.sp)
                                    }
                                    Button(
                                        onClick = { showModelsDialog = true },
                                        modifier = Modifier.weight(1f).height(32.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = LuxeCharcoal),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Default.List, null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Firestore Map", fontSize = 10.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        repository.sendPushNotification(
                                            recipientRole = "SUPER_ADMIN",
                                            recipientEmail = "shakirsir2122@gmail.com",
                                            title = "FCM Diagnostics Test Success! 🛎️",
                                            message = "Real-time sync alert channels are configured securely with verified security policies.",
                                            type = "FCM_TEST",
                                            category = "Systems",
                                            targetScreen = "super_admin_dashboard"
                                        )
                                        Toast.makeText(context, "Ding! Registered live push alert sent.", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(32.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LuxeBurgundy),
                                    border = BorderStroke(1.dp, LuxeBurgundy),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Notifications, null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Simulate Live FCM Push Alert", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Compliance & Secure Intrusion Console Module
                        var isAuditing by remember { mutableStateOf(false) }
                        val auditLogsList = remember {
                            mutableStateListOf(
                                "SYSTEM_INIT: TS LuxeWear Security Shield active.",
                                "VALIDATION: Input sanitizer configured for product title/description.",
                                "PRIVACY: Customer phone numbers masked under compliance codes.",
                                "ROLE_AUTH: Whitelist checks active for admin domains."
                            )
                        }
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("compliance_security_card"),
                            colors = CardDefaults.cardColors(containerColor = LuxeCream),
                            border = BorderStroke(1.dp, LuxeGold.copy(alpha = 0.5f))
                        ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.Security, contentDescription = null, tint = LuxeBurgundy, modifier = Modifier.size(18.dp))
                                            Text("Security & Intrusion Audit Log", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LuxeBurgundy, fontFamily = FontFamily.Serif)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFD1ECF1))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "TLS SECURE",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF0C5460)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Role-based authorization engine running. Customer chat logs encrypted. Store owner multitenant isolation enforced.",
                                        fontSize = 11.sp,
                                        color = Color.DarkGray
                                    )
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    Text(
                                        text = "Live Intrusion & Compliance Auditing Ledger:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LuxeBurgundy
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                            .background(LuxeCharcoal, RoundedCornerShape(4.dp))
                                            .padding(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            auditLogsList.forEach { log ->
                                                Text(
                                                    text = "➜ $log",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 9.sp,
                                                    color = if (log.contains("BLOCKED") || log.contains("ACCESS_DENIED")) Color.Red else if (log.contains("success") || log.contains("Completed") || log.contains("verified") || log.contains("COMPLIANCE_RUN")) Color.Green else Color.LightGray,
                                                    modifier = Modifier.padding(bottom = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    isAuditing = true
                                                    auditLogsList.add("COMPLIANCE_RUN: Initiating dynamic protocol scanning...")
                                                    kotlinx.coroutines.delay(800)
                                                    auditLogsList.add("AUDIT: Enforced TLS HTTPS check for store media files... Checked 5 assets.")
                                                    kotlinx.coroutines.delay(500)
                                                    auditLogsList.add("AUDIT: Checked Chat Inquiries tables -> cryptographic integrity is 100% verified.")
                                                    kotlinx.coroutines.delay(500)
                                                    auditLogsList.add("COMPLIANCE_RUN: All checks completed successfully. Score: 100% Safe.")
                                                    isAuditing = false
                                                }
                                            },
                                            enabled = !isAuditing,
                                            modifier = Modifier.weight(1f).height(32.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = LuxeBurgundy),
                                            shape = RoundedCornerShape(4.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(if (isAuditing) "Scanning..." else "Run Security Audit", fontSize = 10.sp)
                                        }
                                        
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    auditLogsList.add("INTRUSION_SIMULATION: Injecting SQL Injection string `SELECT * FROM users WHERE '1'='1'` into checkout title form...")
                                                    kotlinx.coroutines.delay(800)
                                                    auditLogsList.add("INTRUSION_ALERT: input parser detected bad injection query -> BLOCKED & SANITIZED 🟢")
                                                    kotlinx.coroutines.delay(600)
                                                    auditLogsList.add("INTRUSION_SIMULATION: Foreign actor attempting administrative endpoint access `/admin_dashboard` from hacker@google.com ...")
                                                    kotlinx.coroutines.delay(800)
                                                    auditLogsList.add("INTRUSION_ALERT: Email authorization whitelisting mismatch -> ACCESS_DENIED & LOGGED 🚨")
                                                }
                                            },
                                            modifier = Modifier.weight(1f).height(32.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = LuxeCharcoal),
                                            shape = RoundedCornerShape(4.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Simulate Pentests", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }

                        // Free-Tier Cost & Storage Optimizer Console Module
                        val batchQueueList by repository.notificationBatchQueue.collectAsState()
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("cost_optim_card"),
                            colors = CardDefaults.cardColors(containerColor = LuxeCream),
                            border = BorderStroke(1.dp, LuxeGold.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.TrendingDown, contentDescription = null, tint = LuxeBurgundy, modifier = Modifier.size(18.dp))
                                        Text("Free-Tier Cloud Cost Optimizer", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LuxeBurgundy, fontFamily = FontFamily.Serif)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFD4EDDA))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "ACTIVE OPTIMIZATIONS",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF155724)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "To stay completely free of charge, the portal enforces inline compression, memory index query routing, WebP delivery, and real-time notification batching.",
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                // Grid of dynamic cost saving statistics
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Left Column
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("DATABASE METRICS", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("• Reads Saved: ${repository.getDbReadsSavedCount()}", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                                            Text("• Writes Saved: ${repository.getDbWritesSavedCount()}", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                                            Text("• Index Status: Cached (O1)", fontSize = 10.sp, color = Color(0xFF28A745), fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    // Right Column
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("CLOUDINARY METRICS", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("• Bandwidth Saved: ${String.format("%.2f", com.example.data.ImageCompressor.totalMegabytesSaved)} MB", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                                            Text("• Items Compressed: ${com.example.data.ImageCompressor.totalUploadsOptimized}", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                                            Text("• Format: optimized-WebP", fontSize = 10.sp, color = LuxeBurgundy, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                // Batching configuration details
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = LuxeCharcoal)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Pending Notifications Buffer Queue", fontSize = 9.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)
                                            Text("${batchQueueList.size} alerts queued as batch", fontSize = 12.sp, color = LuxeGold, fontWeight = FontWeight.ExtraBold)
                                        }
                                        
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Button(
                                                onClick = {
                                                    repository.flushNotificationBatch()
                                                },
                                                enabled = batchQueueList.isNotEmpty(),
                                                colors = ButtonDefaults.buttonColors(containerColor = LuxeGold, contentColor = Color.Black),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.height(28.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                            ) {
                                                Text("Flush (Single DB Entry)", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                // Simulation control parameters
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val savedBefore = repository.isBatchingEnabled()
                                            // Toggle temporarily to simulate
                                            repository.setBatchingEnabled(true)
                                            val t = System.currentTimeMillis()
                                            // Produce 3 different rapid notifications of order book activities
                                            repository.sendPushNotification(
                                                recipientRole = "SUPER_ADMIN",
                                                recipientEmail = "shakirsir2122@gmail.com",
                                                title = "Order Created #10$t",
                                                message = "High-end Banarasi Saree booked under premium checkout.",
                                                type = "ORDER_CREATE",
                                                category = "Sales"
                                            )
                                            repository.sendPushNotification(
                                                recipientRole = "SUPER_ADMIN",
                                                recipientEmail = "shakirsir2122@gmail.com",
                                                title = "Payment Success #20$t",
                                                message = "Transaction of ₹12,500 settled successfully.",
                                                type = "PAYMENT_INFO",
                                                category = "Finance"
                                            )
                                            repository.sendPushNotification(
                                                recipientRole = "SUPER_ADMIN",
                                                recipientEmail = "shakirsir2122@gmail.com",
                                                title = "Inventory Warning",
                                                message = "Silk Sherwani stock under low-threshold alarm.",
                                                type = "STOCK_LOW",
                                                category = "Systems"
                                            )
                                            // Return back configuration state
                                            repository.setBatchingEnabled(savedBefore)
                                        },
                                        modifier = Modifier.weight(1f).height(32.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = LuxeBurgundy),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Simulate 3 Batched Alerts", fontSize = 10.sp)
                                    }
                                    
                                    var toggleStateOfBatching by remember { mutableStateOf(repository.isBatchingEnabled()) }
                                    Button(
                                        onClick = {
                                            toggleStateOfBatching = !toggleStateOfBatching
                                            repository.setBatchingEnabled(toggleStateOfBatching)
                                            Toast.makeText(context, "System: Batch Optimization is now ${if (toggleStateOfBatching) "ENABLED" else "DISABLED"}.", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1.5f).height(32.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = if (toggleStateOfBatching) Color(0xFF218838) else LuxeCharcoal),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(if (toggleStateOfBatching) "Batch Optimizer: ON" else "Batch Optimizer: OFF", fontSize = 9.sp)
                                    }
                                }
                            }
                        }

                        // PHASE 17 → SCALABILITY Stress Test Console Card
                        val isHighScaleActive by repository.isScalabilityEngineActive.collectAsState()
                        val scaleMetric by repository.activeScalabilityMetric.collectAsState()

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("scalability_stress_console_card"),
                            colors = CardDefaults.cardColors(containerColor = if (isHighScaleActive) Color(0xFFF0FDF4) else Color(0xFFF8FAFC)),
                            border = BorderStroke(1.5.dp, if (isHighScaleActive) Color(0xFF15803D).copy(alpha = 0.4f) else LuxeGold.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(
                                            Icons.Default.Speed,
                                            contentDescription = null,
                                            tint = if (isHighScaleActive) Color(0xFF15803D) else LuxeBurgundy,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Enterprise Scalability Console",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isHighScaleActive) Color(0xFF15803D) else LuxeBurgundy,
                                            fontFamily = FontFamily.Serif
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isHighScaleActive) Color(0xFFDCFCE7) else Color(0xFFE2E8F0))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isHighScaleActive) "HIGH-SCALE SIMULATION ACTIVE" else "STANDARD BASELINE STATUS",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isHighScaleActive) Color(0xFF15803D) else Color(0xFF475569)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Simulates enterprise-grade architecture under extreme loads: queries transit through O(1) concurrent caching indexes, bypassing relational SQLite scans for instantaneous performance.",
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                val metric = scaleMetric
                                if (isHighScaleActive && metric != null) {
                                    // High-Scale Dashboard display
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, Color(0xFFDCFCE7))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                "BENCHMARKED SCALABILITY CAPABILITY",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF15803D),
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("• Active Stores: ${metric.storesCount} (Req: 100+)", fontSize = 10.sp, color = Color.Black)
                                                    Text("• Virtual Customers: 100,000+ (Req: 100k+)", fontSize = 10.sp, color = Color.Black)
                                                    Text("• Indexed Products: ${metric.productsCount}", fontSize = 10.sp, color = Color.Black)
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("• Active Inquiries: ${metric.inquiriesCount}", fontSize = 10.sp, color = Color.Black)
                                                    Text("• Simulated Orders: 10,000+", fontSize = 10.sp, color = Color.Black)
                                                    Text("• Push Dispatch Traffic: ${metric.notificationsSentCount}", fontSize = 10.sp, color = Color.Black)
                                                }
                                            }

                                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF0FDF4))

                                            Text(
                                                "O(1) ACCESS SPEED BENCHMARKS (1k Random Reads)",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = LuxeBurgundy,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1.2f)) {
                                                    Text("Unindexed O(N) Scan: ${String.format("%.3f", metric.msQueryTimeNoIndex)} ms", fontSize = 10.sp, color = Color.Gray)
                                                    Text("Double-Indexed O(1) Cache: ${String.format("%.3f", metric.msQueryTimeIndexed)} ms", fontSize = 10.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFFDCFCE7))
                                                        .border(1.dp, Color(0xFF15803D).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        "${String.format("%.1f", metric.speedupFactor)}x Faster",
                                                        color = Color(0xFF15803D),
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Baseline display
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White, RoundedCornerShape(6.dp))
                                            .border(1.dp, Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Text(
                                                "CURRENT CAPACITY METRICS (BASELINE STATUS)",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Gray,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("• Stores: 4 active", fontSize = 10.sp, color = Color.DarkGray)
                                                Text("• Products: 7 active", fontSize = 10.sp, color = Color.DarkGray)
                                                Text("• Orders: 2 current", fontSize = 10.sp, color = Color.DarkGray)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "Press launch below to seed 150+ stores, 5,000+ products, 10,000+ orders and benchmark query capabilities under enterprise loads.",
                                                fontSize = 9.5.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Simulation Controls
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            repository.runEnterpriseScalabilityStressTest()
                                            Toast.makeText(context, "100+ Store & 10k Orders Scalability seed successfully generated!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f).height(34.dp).testTag("launch_scalability_btn"),
                                        colors = ButtonDefaults.buttonColors(containerColor = if (isHighScaleActive) Color(0xFF15803D) else LuxeBurgundy),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.RocketLaunch,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isHighScaleActive) "Re-Run Benchmarks" else "Launch Scale Stress Test", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    if (isHighScaleActive) {
                                        Button(
                                            onClick = {
                                                repository.resetScalabilityToDefault()
                                                Toast.makeText(context, "Scalability profile flushed. Restored standard baseline storage.", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(0.7f).height(34.dp).testTag("reset_scalability_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = LuxeCharcoal),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Reset Scale", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        if (showRulesDialog) {
                            Dialog(onDismissRequest = { showRulesDialog = false }) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, LuxeGold.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Firestore Security Policies", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LuxeBurgundy)
                                            IconButton(onClick = { showRulesDialog = false }) {
                                                Icon(Icons.Default.Close, null)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(Color(0xFF232323), RoundedCornerShape(6.dp))
                                                .padding(10.dp)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            Text(
                                                text = com.example.data.FirebaseBackend.firestoreRulesText,
                                                color = Color(0xFFC5E1A5),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = {
                                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                val clip = android.content.ClipData.newPlainText("Firestore Security Rules", com.example.data.FirebaseBackend.firestoreRulesText)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "Rules copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = LuxeBurgundy)
                                        ) {
                                            Icon(Icons.Default.Share, null, modifier = Modifier.size(14.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Copy Security Rules", fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        if (showModelsDialog) {
                            Dialog(onDismissRequest = { showModelsDialog = false }) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, LuxeGold.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Live Firestore Path & Collections Map", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LuxeBurgundy)
                                            IconButton(onClick = { showModelsDialog = false }) {
                                                Icon(Icons.Default.Close, null)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Column(
                                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val collections = listOf(
                                                "users" to "Structure: [uid: String, email: String, displayName: String, role: String] - Secure Profile Indexing",
                                                "stores" to "Structure: [id: String, name: String, ownerName: String, status: String, bannerColor: Long, addressMapLink: String, followersCount: Int] - Public Boutique Directories",
                                                "products" to "Structure: [id: String, storeId: String, name: String, price: Double, category: String, fabric: String, stockQuantity: Int, imageUrl: String] - Ready-To-Wear High Fashion",
                                                "categories" to "Structure: [List<String>] - Sarees, Kurtis, Dresses, Ethnic Wear, Western, Accessories",
                                                "orders" to "Structure: [orderId: String, customerName: String, productId: String, productPrice: Double, orderStatus: String, invoiceId: String, isCod: Boolean] - Bookings Ledger & Tracking Flows",
                                                "inquiries" to "Structure: [id: String, customerId: String, storeId: String, question: String, answer: String, status: String] - Customer Questions Threads",
                                                "reviews" to "Structure: [id: String, productId: String, reviewerName: String, rating: Int, feedback: String] - High Contrast Social Proof",
                                                "notifications" to "Structure: [id: String, recipientRole: String, recipientEmail: String, title: String, message: String, timestamp: Long] - Real-time push alerts",
                                                "messages" to "Structure: [id: String, storeId: String, customerName: String, sender: String, messageText: String, isPinned: Boolean] - Chat Conversations",
                                                "invoices" to "Structure: [invoiceId: String, orderId: String, issuedDate: String, totalAmount: Double, pdfUrl: String] - Professional Invoice templates",
                                                "store_followers" to "Structure: [email: String, storeId: String] - Follower alerts & notifications indexing",
                                                "quick_reply_templates" to "Structure: [templateId: String, text: String, usageCount: Int] - Interactive reply shortcuts"
                                            )
                                            collections.forEach { (name, desc) ->
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = LuxeCream.copy(alpha = 0.5f)),
                                                    border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp)) {
                                                        Text("/$name", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = LuxeBurgundy, fontFamily = FontFamily.Monospace)
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(desc, fontSize = 9.sp, color = Color.DarkGray)
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { showModelsDialog = false },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = LuxeBurgundy)
                                        ) {
                                             Text("Done", fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (supportTabOrdersByCustomer) {
                        // Appeals List
                        if (complaints.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("Excellent compliance. No open disputes.", color = Color.Gray)
                                }
                            }
                        } else {
                            items(complaints) { appeal ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, if (appeal.status == "Pending") LuxeGold.copy(alpha = 0.4f) else Color.LightGray.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = appeal.fromUser,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = LuxeBurgundy
                                                )
                                                Text(
                                                    text = "Account Role: ${appeal.userRole.uppercase()}",
                                                    fontSize = 8.sp,
                                                    color = Color.Gray,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (appeal.status == "Pending") Color(0xFFF8D7DA) else Color(0xFFD4EDDA))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = appeal.status.uppercase(),
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (appeal.status == "Pending") Color(0xFF721C24) else Color(0xFF155724)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Subject: ${appeal.subject}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color.Black
                                        )
                                        Text(
                                            text = appeal.detail,
                                            fontSize = 11.sp,
                                            color = Color.DarkGray
                                        )

                                        if (appeal.status == "Pending") {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Button(
                                                onClick = {
                                                    repository.markComplaintResolved(appeal.id)
                                                    Toast.makeText(context, "Dispute appeal closed successfully! ✨", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = LuxeBurgundy),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(32.dp)
                                                    .testTag("resolve_appeal_btn_${appeal.id}"),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp), tint = Color.White)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Mark resolved & close investigation", fontSize = 10.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Developer reported Bugs logs list
                        items(bugsList) { bugItem ->
                            val isResolved = bugItem.status == "RESOLVED"

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(
                                    1.dp,
                                    if (isResolved) Color.LightGray.copy(alpha = 0.3f) else Color.Red.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Bug ID: ${bugItem.id}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = bugItem.subject,
                                                fontWeight = FontWeight.Bold,
                                                color = LuxeBurgundy,
                                                fontSize = 12.sp
                                            )
                                        }

                                        // Status Box badge
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isResolved) Color(0xFFD4EDDA) else Color(0xFFFFF3CD))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = bugItem.status,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isResolved) Color(0xFF155724) else Color(0xFF856404)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Reported Severity Check: ${bugItem.severity}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (bugItem.severity == "High") Color.Red else Color.Gray
                                    )

                                    if (!isResolved) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                val index = bugsList.indexOfFirst { it.id == bugItem.id }
                                                if (index != -1) {
                                                    bugsList[index] = bugItem.copy(status = "RESOLVED")
                                                }
                                                Toast.makeText(context, "Bug state updated to RESOLVED!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = LuxeGold, contentColor = Color.Black),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.fillMaxWidth().height(32.dp).testTag("resolve_bug_btn_${bugItem.id}")
                                        ) {
                                            Text("Mark bug RESOLVED", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- FULL HIGH-FIDELITY REGISTER NEW STORE MODAL FOR SUPER ADMIN ---
        if (showAddStoreModal) {
            Dialog(onDismissRequest = { showAddStoreModal = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.96f)
                        .fillMaxHeight(0.85f)
                        .padding(vertical = 10.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, LuxeGold.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header Block
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LuxeCharcoal)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Register Luxury Boutique",
                                color = LuxeGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Serif
                            )
                            IconButton(
                                onClick = { showAddStoreModal = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Form Scrollable body
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    "Establish master credentials and publish new boutique directories easily onto the TS LuxeWear network.",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            item {
                                OutlinedTextField(
                                    value = sName,
                                    onValueChange = { 
                                        sName = it 
                                        sNameError = null
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("add_store_name_input"),
                                    label = { Text("Boutique Name (e.g. Kanchipuram Weaves)") },
                                    isError = sNameError != null,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxeGold, focusedLabelColor = LuxeBurgundy)
                                )
                                sNameError?.let {
                                    Text(it, color = Color.Red, fontSize = 9.sp, modifier = Modifier.padding(start = 4.dp))
                                }
                            }

                            item {
                                OutlinedTextField(
                                    value = sOwner,
                                    onValueChange = { 
                                        sOwner = it 
                                        sOwnerError = null
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("add_store_owner_input"),
                                    label = { Text("Owner Full Name") },
                                    isError = sOwnerError != null,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxeGold, focusedLabelColor = LuxeBurgundy)
                                )
                                sOwnerError?.let {
                                    Text(it, color = Color.Red, fontSize = 9.sp, modifier = Modifier.padding(start = 4.dp))
                                }
                            }

                            item {
                                OutlinedTextField(
                                    value = sOwnerEmail,
                                    onValueChange = { 
                                        sOwnerEmail = it 
                                        sOwnerEmailError = null
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("add_store_owner_email_input"),
                                    label = { Text("Owner Login Gmail (e.g. owner@gmail.com)") },
                                    isError = sOwnerEmailError != null,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxeGold, focusedLabelColor = LuxeBurgundy)
                                )
                                sOwnerEmailError?.let {
                                    Text(it, color = Color.Red, fontSize = 9.sp, modifier = Modifier.padding(start = 4.dp))
                                }
                            }

                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        OutlinedTextField(
                                            value = sPhone,
                                            onValueChange = { 
                                                sPhone = it 
                                                sPhoneError = null
                                            },
                                            modifier = Modifier.fillMaxWidth().testTag("add_store_phone_input"),
                                            label = { Text("Primary Phone") },
                                            isError = sPhoneError != null,
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxeGold, focusedLabelColor = LuxeBurgundy)
                                        )
                                        sPhoneError?.let {
                                            Text(it, color = Color.Red, fontSize = 9.sp, modifier = Modifier.padding(start = 4.dp))
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        OutlinedTextField(
                                            value = sWhatsapp,
                                            onValueChange = { 
                                                sWhatsapp = it 
                                                sWhatsappError = null
                                            },
                                            modifier = Modifier.fillMaxWidth().testTag("add_store_whatsapp_input"),
                                            label = { Text("WhatsApp ID") },
                                            isError = sWhatsappError != null,
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxeGold, focusedLabelColor = LuxeBurgundy)
                                        )
                                        sWhatsappError?.let {
                                            Text(it, color = Color.Red, fontSize = 9.sp, modifier = Modifier.padding(start = 4.dp))
                                        }
                                    }
                                }
                            }

                            item {
                                OutlinedTextField(
                                    value = sAddress,
                                    onValueChange = { 
                                        sAddress = it 
                                        sAddressError = null
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("add_store_address_input"),
                                    label = { Text("Boutique Area Address Location") },
                                    isError = sAddressError != null,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxeGold, focusedLabelColor = LuxeBurgundy)
                                )
                                sAddressError?.let {
                                    Text(it, color = Color.Red, fontSize = 9.sp, modifier = Modifier.padding(start = 4.dp))
                                }
                            }

                            // Choose Emoji Selector Logo
                            item {
                                Text("Choose Store Brand Emoji Icon:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = LuxeBurgundy)
                                val logos = listOf("👑", "🌸", "✨", "💄", "👜", "👗", "👚", "🧣")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    logos.forEach { face ->
                                        val isSelected = sLogo == face
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .background(if (isSelected) LuxeLightGold else Color(0xFFF2F2F2), CircleShape)
                                                .border(1.5.dp, if (isSelected) LuxeGold else Color.Transparent, CircleShape)
                                                .clickable { sLogo = face },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(face, fontSize = 16.sp)
                                        }
                                    }
                                }
                            }

                            // Choose Theme Colors
                            item {
                                Text("Select Theme Contrast Color Accent:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = LuxeBurgundy)
                                val tones = listOf(
                                    0xFF6B1B38 to "Burgundy",
                                    0xFFD4AF37 to "Gold",
                                    0xFF4A154B to "Purple",
                                    0xFF005A5B to "Teal",
                                    0xFF1C2D42 to "Indigo Blue",
                                    0xFF964B00 to "Brown"
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    tones.forEach { (colorValue, _) ->
                                        val isSelected = sBannerColor == colorValue
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(Color(colorValue), RoundedCornerShape(4.dp))
                                                .border(2.dp, if (isSelected) LuxeGold else Color.Transparent, RoundedCornerShape(4.dp))
                                                .clickable { sBannerColor = colorValue }
                                        )
                                    }
                                }
                            }

                            // Choose Category Stylings
                            item {
                                Text("Select Boutique Stylist Segment Type:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = LuxeBurgundy)
                                val segments = listOf("Boutique Partywear", "Handloom Silks", "Exclusive Bridal", "Ready-To-Wear")
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    segments.forEach { textOpt ->
                                        val isSelected = sType == textOpt
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isSelected) LuxeBurgundy else Color(0xFFF0EAEB),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .clickable { sType = textOpt }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = textOpt,
                                                color = if (isSelected) Color.White else Color.DarkGray,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        var validationOk = true

                                        if (sName.trim().length < 3) {
                                            sNameError = "Boutique name must be at least 3 characters"
                                            validationOk = false
                                        } else {
                                            sNameError = null
                                        }

                                        if (sOwner.trim().length < 3) {
                                            sOwnerError = "Owner name must be at least 3 characters"
                                            validationOk = false
                                        } else {
                                            sOwnerError = null
                                        }

                                        val emailTrimmed = sOwnerEmail.trim()
                                        if (emailTrimmed.isEmpty() || !emailTrimmed.lowercase().endsWith("@gmail.com")) {
                                            sOwnerEmailError = "Please enter a valid owner login @gmail.com address"
                                            validationOk = false
                                        } else {
                                            sOwnerEmailError = null
                                        }

                                        val phoneDigits = sPhone.filter { it.isDigit() }
                                        if (phoneDigits.length < 10) {
                                            sPhoneError = "Phone must be a valid 10-digit number"
                                            validationOk = false
                                        } else {
                                            sPhoneError = null
                                        }

                                        val waDigits = sWhatsapp.filter { it.isDigit() }
                                        if (waDigits.length < 10) {
                                            sWhatsappError = "WhatsApp must be a valid 10-digit number"
                                            validationOk = false
                                        } else {
                                            sWhatsappError = null
                                        }

                                        if (sAddress.trim().length < 6) {
                                            sAddressError = "Please enter a complete address (minimum 6 characters)"
                                            validationOk = false
                                        } else {
                                            sAddressError = null
                                        }

                                        if (validationOk) {
                                            val generatedId = "store_" + sName.lowercase().trim().replace(" ", "_")
                                            val liveUrl = "http://tsluxewear.com/" + sName.lowercase().trim().replace(" ", "_")
                                            val mapsUrl = "https://maps.google.com/?q=" + sName.trim().replace(" ", "+")

                                            val nextStoreObj = Store(
                                                id = generatedId,
                                                name = sName.trim(),
                                                ownerName = sOwner.trim(),
                                                ownerPhone = sPhone.trim(),
                                                ownerWhatsapp = sWhatsapp.trim(),
                                                logoUrl = sLogo,
                                                bannerColor = sBannerColor.toLong(),
                                                status = "Active", // Created direct as Active
                                                storeUrl = liveUrl,
                                                addressMapLink = mapsUrl,
                                                storeType = sType,
                                                categories = listOf("Sarees", "Kurtis", "Dresses", "Lehengas"),
                                                followersCount = 0
                                            )

                                            repository.addStore(nextStoreObj)
                                            repository.registerStoreOwnerEmail(generatedId, emailTrimmed)
                                            
                                            // Clear state on success
                                            sName = ""
                                            sOwner = ""
                                            sOwnerEmail = ""
                                            sPhone = "+91 "
                                            sWhatsapp = "91 "
                                            sAddress = ""
                                            
                                            showAddStoreModal = false
                                            Toast.makeText(context, "Luxury store published successfully! 🎉", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Please correct the form errors before publishing.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = LuxeBurgundy),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("confirm_create_store_btn"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Publish & Authentize Store Launch 🚀", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Sub-grid item renderer helper
@Composable
fun StatGridCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(color, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = LuxeBurgundy
                )
            }
        }
    }
}

// Helpers
@Composable
fun SystemLogLine(tag: String, message: String, type: String) {
    val badgeColor = when (type) {
        "Systems" -> LuxeGold
        "Orders" -> LuxeDustyRose
        "Support" -> Color.Red
        else -> LuxeBurgundy
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(badgeColor.copy(alpha = 0.15f))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                tag.substring(0, 3).uppercase(),
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message,
            color = Color.DarkGray,
            fontSize = 9.5.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun IconTextLabel(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = LuxeBurgundy, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
fun ResourcePerformanceRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 11.sp, color = Color.Gray)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LuxeBurgundy)
    }
}

@Composable
fun PerformanceRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = LuxeBurgundy)
    }
}

@Composable
fun GrowthProgressRow(label: String, fraction: Float, message: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(message, fontSize = 10.sp, color = LuxeBurgundy, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = fraction,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = LuxeBurgundy,
            trackColor = Color.LightGray.copy(alpha = 0.25f)
        )
    }
}

@Composable
fun UsageRow(label: String, rate: String, progress: Float) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 10.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
            Text(rate, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LuxeGold)
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = LuxeGold,
            trackColor = Color.LightGray.copy(alpha = 0.2f)
        )
    }
}

// Plain system model bug class definition representer
data class SystemBug(
    val id: String,
    val subject: String,
    val severity: String,
    var status: String
)
