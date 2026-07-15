package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TSLuxeWearRepository
import com.example.ui.theme.LuxeBurgundy
import com.example.ui.theme.LuxeGold
import com.example.ui.theme.LuxeLightGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A beautiful, Material 3 compliant global error boundary wrapper.
 * Intercepts network/sync runtime request errors and presents high-fidelity, user-friendly
 * error recovery states with retry handshakes and diagnostic options.
 */
@Composable
fun LuxeErrorBoundary(
    repository: TSLuxeWearRepository,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val errorState by repository.networkErrorState.collectAsState()
    val scope = rememberCoroutineScope()
    var isRetrying by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (errorState == null) {
            content()
            
            // Helpful testing anchor: Simulated "Fail Connection" button at the bottom of the screens/footer
            // Only displayed when error is NOT active so the user can trigger it.
            // Styled as a very small subtle overlay trigger so they can test it.
        } else {
            AnimatedVisibility(
                visible = errorState != null,
                enter = fadeIn(animationSpec = tween(500)) + slideInVertically(initialOffsetY = { -100 }),
                exit = fadeOut(animationSpec = tween(400))
            ) {
                Surface(
                    color = Color(0xFF141416), // Premium obsidian dark canvas
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("global_error_boundary_screen")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Flashing Error Glow Ring
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(110.dp)
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse_error")
                            val scaleRing by infiniteTransition.animateFloat(
                                initialValue = 0.85f,
                                targetValue = 1.25f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1400, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "scale"
                            )
                            val alphaRing by infiniteTransition.animateFloat(
                                initialValue = 0.4f,
                                targetValue = 0.05f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1400, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "alpha"
                            )

                            // Outer Pulse Ring
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .background(
                                        color = LuxeGold.copy(alpha = alphaRing),
                                        shape = CircleShape
                                    )
                                    .border(2.dp, LuxeGold.copy(alpha = alphaRing * 2), CircleShape)
                            )

                            // Inner Glow Ring
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .background(
                                        color = Color(0xFFC70039).copy(alpha = 0.15f),
                                        shape = CircleShape
                                    )
                                    .border(1.dp, Color(0xFFC70039).copy(alpha = 0.4f), CircleShape)
                            )

                            // Center Icon
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "Network Disconnected",
                                tint = LuxeGold,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Category Badge
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = Color(0xFFE53935).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFFE53935), CircleShape)
                                )
                                Text(
                                    text = "SECURE NETWORK EXCEPTION",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF5350),
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Main Error Headings
                        Text(
                            text = "Connection Desynced",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Serif
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // User-Friendly Explanation Box
                        Surface(
                            color = Color.White.copy(alpha = 0.04f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = errorState ?: "The TS LuxeWear Cloud Relay failed to acknowledge packet transmission. Please ensure your device is connected to a fast Wi-Fi grid or LTE data stream.",
                                    fontSize = 12.sp,
                                    color = Color.LightGray,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LockReset,
                                        contentDescription = "Safe Lock",
                                        tint = LuxeGold,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Offline Sandbox safe-mode active. Data preserved locally.",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LuxeGold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Action Buttons: Try Again & Diagnostics Hub
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Retry Action
                            Button(
                                onClick = {
                                    if (!isRetrying) {
                                        isRetrying = true
                                        scope.launch {
                                            delay(1400) // Simulated packet re-allocation
                                            isRetrying = false
                                            repository.clearNetworkError()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(44.dp)
                                    .testTag("err_retry_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = LuxeGold),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isRetrying) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF141416)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Re-establishing...",
                                        color = Color(0xFF141416),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Retry Connection",
                                        tint = Color(0xFF141416),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Retry Action Request",
                                        color = Color(0xFF141416),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Quick Offline Local Mode Bypass Action
                            OutlinedButton(
                                onClick = {
                                    repository.clearNetworkError()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text(
                                    text = "Go Offline",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A beautiful developer test tool floating anchor to simulate real cloud drops.
 */
@Composable
fun SimulatedNetworkFaultOverlay(
    repository: TSLuxeWearRepository,
    modifier: Modifier = Modifier
) {
    val errorState by repository.networkErrorState.collectAsState()
    
    // Only show if not currently in error state so the user can trigger it easily to test
    if (errorState == null) {
        Box(
            modifier = modifier
                .padding(16.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Color(0xFFE53935).copy(alpha = 0.85f))
                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(30.dp))
                .clickable {
                    repository.triggerNetworkError("Simulated server handshake timeout: The cloud service failed to reply within the safety window of 10000ms.")
                }
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("simulate_fault_btn")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Simulate",
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = "FAULT SIMULATOR",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
