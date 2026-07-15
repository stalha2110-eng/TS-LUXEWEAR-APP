package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Centralized Luxe style constants
private val LuxeBurgundy = Color(0xFF5C132E)
private val LuxeGold = Color(0xFFD4AF37)
private val LuxeCream = Color(0xFFFDFBF7)

/**
 * A highly accessible, beautifully animated pagination controller matching Material 3 patterns.
 * Designed to satisfy Phase 16 Free Tier optimizations by reducing memory pressure and DB fetch sizes.
 */
@Composable
fun LuxePaginator(
    currentPage: Int,
    totalItems: Int,
    pageSize: Int,
    onPageChange: (Int) -> Unit,
    onPageSizeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    pageSizeOptions: List<Int> = listOf(3, 5, 10, 20)
) {
    val totalPages = maxOf(1, kotlin.math.ceil(totalItems.toDouble() / pageSize).toInt())
    var dropdownExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = LuxeCream,
        shadowElevation = 1.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left portion: Page Size Customizer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Items / Page:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
                Box {
                    TextButton(
                        onClick = { dropdownExpanded = true },
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("page_size_trigger"),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text(
                            text = "$pageSize ▾",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = LuxeBurgundy
                        )
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        pageSizeOptions.forEach { size ->
                            DropdownMenuItem(
                                text = { Text("$size items", fontSize = 12.sp) },
                                onClick = {
                                    onPageSizeChange(size)
                                    dropdownExpanded = false
                                },
                                modifier = Modifier.testTag("page_size_option_$size")
                            )
                        }
                    }
                }
            }

            // Right portion: Navigation Arrow Buttons with compliant Touch Targets (>= 48dp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Back Arrow Button
                IconButton(
                    onClick = { if (currentPage > 1) onPageChange(currentPage - 1) },
                    enabled = currentPage > 1,
                    modifier = Modifier
                        .size(48.dp) // Accessibility Target Size >= 48dp
                        .testTag("prev_page_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Page",
                        tint = if (currentPage > 1) LuxeBurgundy else Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Page Indicator Text
                Text(
                    text = "Page $currentPage of $totalPages",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = LuxeBurgundy
                )

                // Forward Arrow Button
                IconButton(
                    onClick = { if (currentPage < totalPages) onPageChange(currentPage + 1) },
                    enabled = currentPage < totalPages,
                    modifier = Modifier
                        .size(48.dp) // Accessibility Target Size >= 48dp
                        .testTag("next_page_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Page",
                        tint = if (currentPage < totalPages) LuxeBurgundy else Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Helper slice function for local collection pagination.
 */
fun <T> List<T>.paginate(page: Int, size: Int): List<T> {
    if (this.isEmpty()) return emptyList()
    val fromIndex = (page - 1) * size
    if (fromIndex >= this.size) return emptyList()
    val toIndex = minOf(fromIndex + size, this.size)
    return this.subList(fromIndex, toIndex)
}
