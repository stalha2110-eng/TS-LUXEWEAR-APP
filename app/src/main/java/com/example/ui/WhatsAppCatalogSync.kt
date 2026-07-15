package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TSLuxeWearRepository
import com.example.model.Product
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * A state-of-the-art WhatsApp Catalog Sync screen for boutique owners.
 * Features cloud sync animations, detailed log outputs, and product linking.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppCatalogSyncScreen(
    repository: TSLuxeWearRepository,
    storeId: String,
    storeProducts: List<Product>,
    ownerWhatsapp: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val syncedProductIds by repository.whatsappSyncedProductsFlow.collectAsState()
    val syncLogs by repository.whatsappSyncLogsFlow.collectAsState()
    val isSyncing by repository.isWhatsappSyncingFlow.collectAsState()
    val lastSyncTime by repository.whatsappLastSyncTimeFlow.collectAsState()

    var inputPhone by remember(ownerWhatsapp) { mutableStateOf(ownerWhatsapp.ifBlank { "919012345678" }) }
    var configCollapse by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9FB))
            .testTag("whatsapp_sync_panel"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // META BRANDING CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF25D366).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SyncAlt,
                                contentDescription = null,
                                tint = Color(0xFF128C7E),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "WhatsApp Catalog Integrator",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = LuxeBurgundy,
                                fontFamily = FontFamily.Serif
                            )
                            Text(
                                text = "Powered by Meta Business Cloud Catalog Handshakes",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Synchronize your app boutique listings with WhatsApp Business instantly. Customers will be able to browse items on whatsapp catalogs, and check out directly via automated pre-filled messages sent directly to your phone.",
                        fontSize = 11.sp,
                        color = LuxeCharcoal,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // SETTINGS & HANDSHAKE ROUTER CONFIG
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, LuxeCream)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { configCollapse = !configCollapse },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, null, tint = LuxeGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Business Profile & Contact Settings",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = LuxeBurgundy
                            )
                        }
                        Icon(
                            imageVector = if (configCollapse) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = "Collapse",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (!configCollapse) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = inputPhone,
                            onValueChange = { inputPhone = it },
                            label = { Text("WhatsApp Phone (with Country Prefix, e.g. 9198XXX)") },
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("whatsapp_owner_phone_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LuxeGold,
                                focusedLabelColor = LuxeBurgundy
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick info regarding prefilled message structure
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(6.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Direct-to-chat message links are automatically optimized with sizing, colorways and platform order identifiers.",
                                fontSize = 9.sp,
                                color = Color(0xFF1B5E20),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // TRIGGER SYNC CONTROL PANEL
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Real-time Synchronization Engine",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = LuxeBurgundy
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Transmit metadata and deep schemas inside the cloud database sandbox to configure catalogs.",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isSyncing) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF25D366),
                            trackColor = Color(0xFFE0E0E0)
                        )
                    } else if (lastSyncTime != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.CloudDone, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                            val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
                            Text(
                                text = "Last synced: ${sdf.format(Date(lastSyncTime!!))}",
                                fontSize = 10.sp,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                repository.syncBoutiqueCatalogWithWhatsApp(storeId, storeProducts)
                                Toast.makeText(context, "WhatsApp Sync Handshake successful! 📲", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isSyncing) Color.Gray else Color(0xFF25D366)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("trigger_whatsapp_sync_btn"),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isSyncing && storeProducts.isNotEmpty()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("In Sync Transmission...", color = Color.White, fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Sync, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (lastSyncTime == null) "Broadcasting Initial Catalog [Sync Now]" else "Re-sync Boutique Catalog",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // LIVE LOGGER SHEETS
        if (syncLogs.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)), // Obsidian screen consoles
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (isSyncing) Color.Yellow else Color(0xFF25D366), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Handshake Console Monitor Logs",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            if (!isSyncing) {
                                Text(
                                    text = "LIVE FEED",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF25D366)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            syncLogs.forEach { log ->
                                Text(
                                    text = log,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = if (log.startsWith("✅")) Color(0xFF81C784) else Color.LightGray,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // ITEM LEVEL DETAIL STATUS SHEET
        item {
            Text(
                text = "Boutique Item Status & Link Generators (${storeProducts.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = LuxeBurgundy
            )
        }

        if (storeProducts.isEmpty()) {
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LuxeCream),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CloudOff, null, tint = LuxeGold, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No collection items found. Go to Catalog and Add products first.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(storeProducts) { prod ->
                val isSynced = syncedProductIds.contains(prod.id)
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("whatsapp_product_item_${prod.id}"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, if (isSynced) Color(0xFF25D366).copy(alpha = 0.3f) else LuxeCream)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = prod.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LuxeBurgundy
                                )
                                Text(
                                    text = "Category: ${prod.category} • ₹${prod.price.toInt()}",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }

                            // Synced status badge
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = if (isSynced) Color(0xFFE8F5E9) else Color(0xFFECEFF1),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSynced) Color(0xFF25D366).copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(if (isSynced) Color(0xFF25D366) else Color.Gray, CircleShape)
                                    )
                                    Text(
                                        text = if (isSynced) "WhatsApp Synced" else "Not Linked",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSynced) Color(0xFF1B5E20) else Color.Gray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Actions: Prefilled Link Copy / Simulate Link / Sync trigger
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isSynced) {
                                // Generate direct clipboard pre-filled link
                                OutlinedButton(
                                    onClick = {
                                        val productLink = "myapp.com/product?storeId=${prod.storeId}&productId=${prod.id}"
                                        val promoText = "Hello! I am browsing your WhatsApp Catalog, and I want to order this product:\n\n*Product:* ${prod.name}\n*Price:* ₹${prod.price.toInt()}\n*Weave:* ${prod.fabric}\n*App Catalog Link:* $productLink"
                                        val apiLink = "https://wa.me/$inputPhone?text=${Uri.encode(promoText)}"
                                        
                                        clipboardManager.setText(AnnotatedString(apiLink))
                                        Toast.makeText(context, "Direct WhatsApp Order Link copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(32.dp)
                                        .testTag("copy_whatsapp_link_${prod.id}"),
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF128C7E)),
                                    border = BorderStroke(1.dp, Color(0xFF128C7E)),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy Order Link", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }

                                // Quick desync
                                OutlinedButton(
                                    onClick = {
                                        repository.removeProductFromWhatsAppSync(prod.id)
                                        Toast.makeText(context, "Removed product from WhatsApp catalog indexing.", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(0.8f)
                                        .height(32.dp)
                                        .testTag("desync_whatsapp_item_${prod.id}"),
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.LinkOff, null, modifier = Modifier.size(12.dp), tint = Color.Red)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Disconnect", fontSize = 9.sp, color = Color.Red)
                                }
                            } else {
                                // Direct Link item to WhatsApp
                                Button(
                                    onClick = {
                                        repository.linkProductToWhatsAppSync(prod.id)
                                        Toast.makeText(context, "Product linked into WhatsApp index!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(32.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = LuxeBurgundy),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Link, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Link This Product Now", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
