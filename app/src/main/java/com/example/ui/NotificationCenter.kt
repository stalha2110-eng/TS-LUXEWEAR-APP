package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Renders a floating visual push notification card when a new FCM alert arrives in real-time.
 * Features animated slide-in, professional sound confirmation, and click-to-navigation.
 */
@Composable
fun RealtimeNotificationBanner(
    repository: TSLuxeWearRepository,
    onNavigate: (String) -> Unit
) {
    val activePush by repository.activeRealtimePush.collectAsState()

    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(activePush) {
        if (activePush != null) {
            visible = true
            delay(4000) // Stay visible for 4 seconds
            visible = false
            delay(300) // Wait for fade-out animation to complete
            repository.clearActiveRealtimePush()
        }
    }

    AnimatedVisibility(
        visible = visible && activePush != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(16.dp)
            .testTag("realtime_fcm_banner")
    ) {
        val push = activePush ?: return@AnimatedVisibility
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    // Mark as read and deep-link click on click-through
                    repository.updateNotificationReadState(push.id, true)
                    push.targetScreen?.let { onNavigate(it) }
                    visible = false
                    repository.clearActiveRealtimePush()
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B1C)),
            border = BorderStroke(1.5.dp, LuxeGold),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Emoji Badge
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(LuxeGold.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val emoji = when (push.category) {
                        "Orders" -> "📦"
                        "Inquiries" -> "💬"
                        "Systems" -> "⚙️"
                        "Marketing" -> "🏷️"
                        else -> "🔔"
                    }
                    Text(emoji, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = push.title,
                        color = LuxeGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = push.message,
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        visible = false
                        repository.clearActiveRealtimePush()
                    }
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/**
 * Fully loaded elegant overlay presenting the In-App Notification Center and active sandbox simulator.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationCenterDialog(
    repository: TSLuxeWearRepository,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val currentUser by AuthManager.currentUserFlow.collectAsState()
    val context = LocalContext.current

    // Safely retrieve live database notification log
    val notificationsList by if (currentUser != null) {
        repository.getNotificationsFlow(currentUser!!.role.name, currentUser!!.email)
            ?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    var selectedCategoryFilter by remember { mutableStateOf("All") }

    // Optimization configurations
    var isThrottlingOn by remember { mutableStateOf(repository.isThrottlingEnabled()) }
    var isSmartTimingOn by remember { mutableStateOf(repository.isSmartTimingEnabled()) }
    var isBatchingOn by remember { mutableStateOf(repository.isBatchingEnabled()) }
    var isSoundOn by remember { mutableStateOf(repository.isSoundEnabled()) }

    // Custom simulated notification trigger states
    var simulationTabActive by remember { mutableStateOf(false) }
    var simCustomTitle by remember { mutableStateOf("") }
    var simCustomMsg by remember { mutableStateOf("") }
    var simCustomType by remember { mutableStateOf("ORDER_STATUS") }
    var simCustomCategory by remember { mutableStateOf("Orders") }

    // Category mappings for display
    val categories = listOf("All", "Orders", "Inquiries", "Systems", "Marketing")

    val filteredList = if (selectedCategoryFilter == "All") {
        notificationsList
    } else {
        notificationsList.filter { it.category == selectedCategoryFilter }
    }

    val unreadCount = notificationsList.count { it.isRead == 0 }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .testTag("notification_center_card"),
            colors = CardDefaults.cardColors(containerColor = LuxeCream),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, LuxeGold.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header block
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1B1C))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Notification Center",
                            color = LuxeGold,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                        val loggedInAs = currentUser?.email ?: "Guest User"
                        Text(
                            text = "Role: ${currentUser?.role?.displayName ?: "Guest"} • $loggedInAs",
                            color = Color.LightGray.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Badge count display
                        if (unreadCount > 0) {
                            Badge(
                                containerColor = LuxeBurgundy,
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Text("$unreadCount unread", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(2.dp))
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    }
                }

                // Sub-tabs: Inbox vs Simulator
                TabRow(
                    selectedTabIndex = if (simulationTabActive) 1 else 0,
                    containerColor = Color(0xFF2C2729),
                    contentColor = LuxeGold,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[if (simulationTabActive) 1 else 0]),
                            color = LuxeGold
                        )
                    }
                ) {
                    Tab(
                        selected = !simulationTabActive,
                        onClick = { simulationTabActive = false }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Inbox, null, tint = if (!simulationTabActive) LuxeGold else Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Inbox Alert Hub", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (!simulationTabActive) LuxeGold else Color.Gray)
                        }
                    }
                    Tab(
                        selected = simulationTabActive,
                        onClick = { simulationTabActive = true }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, null, tint = if (simulationTabActive) LuxeGold else Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("FCM Simulation Panel", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (simulationTabActive) LuxeGold else Color.Gray)
                        }
                    }
                }

                if (!simulationTabActive) {
                    // Category list chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val selected = selectedCategoryFilter == cat
                            FilterChip(
                                selected = selected,
                                onClick = { selectedCategoryFilter = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LuxeGold,
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color.White.copy(alpha = 0.5f),
                                    labelColor = LuxeBurgundy
                                )
                            )
                        }
                    }

                    // Top toolbar alerts controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredList.size} Notifications",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )

                        if (filteredList.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Mark all as read Check",
                                    color = LuxeBurgundy,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clickable {
                                            currentUser?.let {
                                                repository.markAllNotificationsAsRead(it.role.name, it.email)
                                            }
                                        }
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = LuxeGold.copy(alpha = 0.2f))

                    // Alert list
                    if (filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📭", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No Notifications Found",
                                    fontWeight = FontWeight.Bold,
                                    color = LuxeBurgundy,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Serif
                                )
                                Text(
                                    text = "Any push notification matches for role and filter category will appear in this feed.",
                                    textAlign = TextAlign.Center,
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredList) { item ->
                                val isUnread = item.isRead == 0
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("notification_item_${item.id}"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isUnread) LuxeGold.copy(alpha = 0.08f) else Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (isUnread) LuxeGold.copy(alpha = 0.4f) else Color.LightGray.copy(alpha = 0.4f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Status Icon representation
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .background(
                                                        if (isUnread) LuxeGold.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.15f),
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val char = when (item.type) {
                                                    "ORDER_STATUS", "ORDER_CANCEL", "NEW_ORDER" -> "📦"
                                                    "NEW_INQUIRY", "INQUIRY_REPLY" -> "💬"
                                                    "LOW_STOCK" -> "⚠️"
                                                    "STORE_REG" -> "🆕"
                                                    "SUSPICIOUS_ACT" -> "🚨"
                                                    "NEW_FOLLOWER" -> "✨"
                                                    else -> "🔔"
                                                }
                                                Text(char, fontSize = 15.sp)
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.title,
                                                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                                                    color = LuxeBurgundy,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = "Category: ${item.category} • ${android.text.format.DateUtils.getRelativeTimeSpanString(item.timestamp)}",
                                                    fontSize = 9.sp,
                                                    color = Color.Gray
                                                )
                                            }

                                            // Options actions
                                            IconButton(
                                                onClick = {
                                                    repository.updateNotificationReadState(item.id, !isUnread)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isUnread) Icons.Default.MarkChatRead else Icons.Default.MarkChatUnread,
                                                    contentDescription = "Mark Read status toggle",
                                                    tint = if (isUnread) LuxeBurgundy else Color.Gray,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    repository.deleteNotification(item.id)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete notification log",
                                                    tint = Color.Red.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.message,
                                            color = Color.DarkGray,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(start = 42.dp, end = 8.dp)
                                        )

                                        if (item.targetScreen != null) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Click to jump navigate ➔",
                                                color = LuxeGold,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .padding(start = 42.dp)
                                                    .clickable {
                                                        repository.updateNotificationReadState(item.id, true)
                                                        onNavigate(item.targetScreen)
                                                        onDismiss()
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // FCM Simulation pane
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "Fine-tuned FCM Optimization Panel",
                                fontWeight = FontWeight.Bold,
                                color = LuxeBurgundy,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Serif
                            )
                            Text(
                                text = "Experiment with core cloud network latency mockups, anti-spam protections & timing safety switches.",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        item {
                            // Switches grid
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Play Interactive Sound Tone 🔔", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LuxeBurgundy)
                                            Text("Plays standard systems audio ringtone when a push is received.", fontSize = 9.sp, color = Color.Gray)
                                        }
                                        Switch(
                                            checked = isSoundOn,
                                            onCheckedChange = {
                                                isSoundOn = it
                                                repository.setSoundEnabled(it)
                                            },
                                            modifier = Modifier.testTag("sound_toggle")
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Throttling Guard Controls (Repeats Prevention) 🛡️", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LuxeBurgundy)
                                            Text("Locks delivery of identical push notifications within 4 seconds interval.", fontSize = 9.sp, color = Color.Gray)
                                        }
                                        Switch(
                                            checked = isThrottlingOn,
                                            onCheckedChange = {
                                                isThrottlingOn = it
                                                repository.setThrottlingEnabled(it)
                                            },
                                            modifier = Modifier.testTag("throttling_toggle")
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Smart Timing Mode (Midnight Silent Rule) ⏰", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LuxeBurgundy)
                                            Text("Automatically queues notifications received late-night (10 PM to 7 AM) to the morning queue.", fontSize = 9.sp, color = Color.Gray)
                                        }
                                        Switch(
                                            checked = isSmartTimingOn,
                                            onCheckedChange = {
                                                isSmartTimingOn = it
                                                repository.setSmartTimingEnabled(it)
                                            },
                                            modifier = Modifier.testTag("smart_timing_toggle")
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Batch Optimization Combines 📦", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LuxeBurgundy)
                                            Text("Compresses metadata attributes and appends batched labels automatically.", fontSize = 9.sp, color = Color.Gray)
                                        }
                                        Switch(
                                            checked = isBatchingOn,
                                            onCheckedChange = {
                                                isBatchingOn = it
                                                repository.setBatchingEnabled(it)
                                            },
                                            modifier = Modifier.testTag("batching_toggle")
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Trigger Custom Simulated Push Notification",
                                fontWeight = FontWeight.Bold,
                                color = LuxeBurgundy,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Serif
                            )
                            Text(
                                text = "Craft custom fields to test permissions gating, preference checks, database writing, and sound effects instantly.",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = simCustomTitle,
                                    onValueChange = { simCustomTitle = it },
                                    label = { Text("Display Header (Title)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = LuxeGold,
                                        focusedLabelColor = LuxeBurgundy
                                    )
                                )

                                OutlinedTextField(
                                    value = simCustomMsg,
                                    onValueChange = { simCustomMsg = it },
                                    label = { Text("Alert Information Details (Message)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = LuxeGold,
                                        focusedLabelColor = LuxeBurgundy
                                    )
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("FCM Notification Type", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        val typeOptions = listOf("ORDER_STATUS", "LOW_STOCK", "STORE_REG", "SUSPICIOUS_ACT")
                                        var typeExpanded by remember { mutableStateOf(false) }
                                        Box {
                                            Button(
                                                onClick = { typeExpanded = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = LuxeGold, contentColor = Color.Black),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(simCustomType, fontSize = 10.sp)
                                            }
                                            DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                                                typeOptions.forEach { op ->
                                                    DropdownMenuItem(
                                                        text = { Text(op, fontSize = 11.sp) },
                                                        onClick = {
                                                            simCustomType = op
                                                            typeExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Filter Category", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        val catOptions = listOf("Orders", "Inquiries", "Systems", "Marketing")
                                        var catExpanded by remember { mutableStateOf(false) }
                                        Box {
                                            Button(
                                                onClick = { catExpanded = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = LuxeGold, contentColor = Color.Black),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(simCustomCategory, fontSize = 10.sp)
                                            }
                                            DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                                                catOptions.forEach { op ->
                                                    DropdownMenuItem(
                                                        text = { Text(op, fontSize = 11.sp) },
                                                        onClick = {
                                                            simCustomCategory = op
                                                            catExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        val user = currentUser
                                        if (user != null) {
                                            val title = if (simCustomTitle.trim().isEmpty()) "Test Push Dispatcher" else simCustomTitle
                                            val msg = if (simCustomMsg.trim().isEmpty()) "Simulating live cloud deliveries through FCM network relays." else simCustomMsg
                                            repository.sendPushNotification(
                                                recipientRole = user.role.name,
                                                recipientEmail = user.email,
                                                title = title,
                                                message = msg,
                                                type = simCustomType,
                                                category = simCustomCategory,
                                                targetScreen = when (user.role) {
                                                    UserRole.SUPER_ADMIN -> "super_admin_dashboard"
                                                    UserRole.STORE_OWNER -> "store_owner_dashboard"
                                                    else -> "customer_home"
                                                }
                                            )
                                        } else {
                                            Toast.makeText(context, "Please log in as an active profile to test pushes!", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = LuxeBurgundy, contentColor = Color.White)
                                ) {
                                    Text("Simulate Live FCM Dispatch 🚀", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Preset Scenarios
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Fast Scenario Presets", fontWeight = FontWeight.Bold, color = LuxeBurgundy, fontSize = 12.sp)
                            Text("Quickly simulate real-world Phase 4 push events to the logged-in profile.", color = Color.Gray, fontSize = 9.sp)
                            Spacer(modifier = Modifier.height(6.dp))

                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        currentUser?.let { user ->
                                            repository.sendPushNotification(
                                                recipientRole = user.role.name,
                                                recipientEmail = user.email,
                                                title = "Maintenance Payment Received 💳",
                                                message = "Billing alert: payment of ₹2500 received for invoice REF-901-A.",
                                                type = "FEE_RECEIVED",
                                                category = "Systems"
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Admin: Fee Paid", fontSize = 10.sp)
                                }

                                Button(
                                    onClick = {
                                        currentUser?.let { user ->
                                            repository.sendPushNotification(
                                                recipientRole = user.role.name,
                                                recipientEmail = user.email,
                                                title = "Suspicious Device Access! 🚨",
                                                message = "Audit threat: multiple concurrent sessions detected from outside whitelisted region.",
                                                type = "SUSPICIOUS_ACT",
                                                category = "Systems"
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Admin: Suspicious Auth", fontSize = 10.sp)
                                }

                                Button(
                                    onClick = {
                                        currentUser?.let { user ->
                                            repository.sendPushNotification(
                                                recipientRole = user.role.name,
                                                recipientEmail = user.email,
                                                title = "Boutique Return Request 📦",
                                                message = "Refund request filed by customer order #order_1003 for handloom silk saree because of color shade variance.",
                                                type = "RETURN_REQUEST",
                                                category = "Orders"
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Owner: Return Requested", fontSize = 10.sp)
                                }

                                Button(
                                    onClick = {
                                        currentUser?.let { user ->
                                            repository.sendPushNotification(
                                                recipientRole = user.role.name,
                                                recipientEmail = user.email,
                                                title = "Season Festival Campaign! 🏷️",
                                                message = "Couture Extravaganza: enjoy flat 15% discount across all followed stores using coupon FESTIVE15.",
                                                type = "FESTIVAL_OFFER",
                                                category = "Marketing"
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Customer: Holiday Promo", fontSize = 10.sp)
                                }

                                Button(
                                    onClick = {
                                        currentUser?.let { user ->
                                            repository.sendPushNotification(
                                                recipientRole = user.role.name,
                                                recipientEmail = user.email,
                                                title = "Wishlist Restocked! 🎉",
                                                message = "The catalog item 'Exquisite Jamdani Silk Outfit' is back on shelves. Fast Checkout before stock drains!",
                                                type = "RESTOCKED",
                                                category = "Marketing"
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Customer: Item Restocked", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
