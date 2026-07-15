package com.example.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.window.DialogProperties
import com.example.data.LuxePermission
import com.example.data.PermissionManager
import com.example.data.PermissionState
import com.example.ui.theme.*

/**
 * A beautiful, highly-responsive Material 3 rationale dialog presenting privacy details.
 */
@Composable
fun PermissionRationaleDialog(
    permission: LuxePermission,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val privacyRationale = PermissionManager.getPrivacyRationale(permission)
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp)
                .testTag("permission_dialog_${permission.id}"),
            colors = CardDefaults.cardColors(containerColor = LuxeCream),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, LuxeGold.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(LuxeBurgundy.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = permission.iconEmoji,
                        fontSize = 32.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = permission.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LuxeBurgundy,
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Explain clearly WHY the permission is needed
                Text(
                    text = permission.description,
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Highlighted purpose card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Purpose Icon",
                            tint = LuxeBurgundy,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Intended App Purpose:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LuxeBurgundy
                            )
                            Text(
                                text = permission.purpose,
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Data flow encryption disclaimer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Privacy Icon",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Privacy Safeguard: Strict Local Encryption Active",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E7D32)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Beautiful custom interactive CTA options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_permission_decline_low"),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray)
                    ) {
                        Text("Not Now", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("btn_permission_approve_high"),
                        colors = ButtonDefaults.buttonColors(containerColor = LuxeBurgundy, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Allow Access", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * Instruction card if previous permissions are denied.
 * Allows user to go to Settings smoothly without locking features.
 */
@Composable
fun PermissionSettingsInstructionDialog(
    permission: LuxePermission,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("permission_settings_guide"),
            colors = CardDefaults.cardColors(containerColor = LuxeCream),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, LuxeGold.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Permission Setting Required",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LuxeBurgundy,
                    fontFamily = FontFamily.Serif
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Previously, access to ${permission.title} was declined. To utilize this option, please enable it manually in your device system settings:",
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Steps Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("1. ", fontWeight = FontWeight.Bold, color = LuxeBurgundy, fontSize = 12.sp)
                        Text("Tap the 'Open Settings' button below.", fontSize = 12.sp, color = Color.Gray)
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        Text("2. ", fontWeight = FontWeight.Bold, color = LuxeBurgundy, fontSize = 12.sp)
                        Text("Locate and press 'Permissions'.", fontSize = 12.sp, color = Color.Gray)
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        Text("3. ", fontWeight = FontWeight.Bold, color = LuxeBurgundy, fontSize = 12.sp)
                        Text("Toggle the 'Allow' switch for '${permission.title}'.", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color.Gray, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            // Launch native application details settings activity securely
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback info toast
                                val intent = Intent(Settings.ACTION_SETTINGS)
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("btn_go_to_settings"),
                        colors = ButtonDefaults.buttonColors(containerColor = LuxeBurgundy)
                    ) {
                        Text("Open Settings", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * A beautiful initial welcome card greeting users on first launch of dashboard.
 * Requests ONLY push notification (essential basic app update notification) permission upfront.
 */
@Composable
fun WelcomePermissionsOnboardSheet(
    onDismiss: () -> Unit,
    onNotificationGrant: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("privacy_welcome_sheet"),
            colors = CardDefaults.cardColors(containerColor = LuxeCream),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, LuxeGold.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Gold crown emoji or sparkle
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(LuxeGold.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👑", fontSize = 36.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome to TS LuxeWear",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = LuxeBurgundy,
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Exclusive Multitenant Boutique Showrooms",
                    fontSize = 11.sp,
                    color = LuxeLightGold,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "To deliver custom handloom alerts, dispatch orders, and boutique restocks, allow us to send occasional premium updates:",
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Beautiful privacy promise badge
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Privacy commitment: No spam. 100% secure.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        onNotificationGrant()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_welcome_grant_notifications"),
                    colors = ButtonDefaults.buttonColors(containerColor = LuxeBurgundy, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enable Handloom Alerts", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = {
                        PermissionManager.updatePermission(LuxePermission.NOTIFICATION, PermissionState.DENIED)
                        onDismiss()
                    },
                    modifier = Modifier.testTag("btn_welcome_skip_all")
                ) {
                    Text(
                        text = "I prefer reading updates manually",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
