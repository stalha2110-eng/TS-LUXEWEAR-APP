package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Highly polished, Material 3 styled Shimmer Skeleton loaders.
 * Creates clean pulsing placeholders that simulate network data-fetching.
 */

@Composable
fun getShimmerBrush(): Brush {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerColors = listOf(
        Color(0xFFE5DFE0), // Soft cream-gray base
        Color(0xFFF2ECEE), // Lighter highlight
        Color(0xFFE5DFE0)  // Soft cream-gray base
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset(x = translateAnim, y = translateAnim)
    )
}

@Composable
fun SkeletonBox(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush = getShimmerBrush())
    )
}

@Composable
fun SkeletonCircle(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(brush = getShimmerBrush())
    )
}

@Composable
fun SkeletonProductGridItem() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // Product Thumbnail Image Placeholder
        SkeletonBox(
            width = Dp.Unspecified,
            height = 160.dp,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 12.dp
        )
        Spacer(modifier = Modifier.height(10.dp))
        // Title line 1
        SkeletonBox(
            width = 130.dp,
            height = 14.dp,
            cornerRadius = 4.dp
        )
        Spacer(modifier = Modifier.height(6.dp))
        // Title line 2 (shorter)
        SkeletonBox(
            width = 90.dp,
            height = 11.dp,
            cornerRadius = 4.dp
        )
        Spacer(modifier = Modifier.height(10.dp))
        // Price and virtual try-on pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBox(width = 55.dp, height = 16.dp, cornerRadius = 4.dp)
            SkeletonBox(width = 75.dp, height = 24.dp, cornerRadius = 12.dp)
        }
    }
}

@Composable
fun SkeletonProductGrid(rowsCount: Int = 2) {
    Column(modifier = Modifier.fillMaxWidth()) {
        repeat(rowsCount) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SkeletonProductGridItem()
                }
                Box(modifier = Modifier.weight(1f)) {
                    SkeletonProductGridItem()
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun SkeletonStoreHorizontalItem() {
    Row(
        modifier = Modifier
            .padding(6.dp)
            .width(180.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonCircle(size = 36.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            SkeletonBox(width = 90.dp, height = 12.dp, cornerRadius = 3.dp)
            Spacer(modifier = Modifier.height(4.dp))
            SkeletonBox(width = 60.dp, height = 9.dp, cornerRadius = 3.dp)
        }
    }
}

@Composable
fun SkeletonStoreRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) {
            SkeletonStoreHorizontalItem()
        }
    }
}

@Composable
fun SkeletonListItem(hasIconPrefix: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasIconPrefix) {
            SkeletonCircle(size = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            SkeletonBox(width = 200.dp, height = 14.dp, cornerRadius = 4.dp)
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonBox(width = 140.dp, height = 10.dp, cornerRadius = 3.dp)
        }
        SkeletonBox(width = 70.dp, height = 20.dp, cornerRadius = 6.dp)
    }
}

@Composable
fun SkeletonList(itemsCount: Int = 3) {
    Column(modifier = Modifier.fillMaxWidth()) {
        repeat(itemsCount) {
            SkeletonListItem()
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun SkeletonChatBubbleItem(isUser: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            SkeletonCircle(size = 30.dp)
            Spacer(modifier = Modifier.width(8.dp))
        }
        SkeletonBox(
            width = if (isUser) 160.dp else 210.dp,
            height = 42.dp,
            cornerRadius = 14.dp
        )
        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            SkeletonCircle(size = 30.dp)
        }
    }
}

@Composable
fun SkeletonChatScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 60.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Chat Header Simulator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonCircle(size = 44.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                SkeletonBox(width = 140.dp, height = 15.dp, cornerRadius = 4.dp)
                Spacer(modifier = Modifier.height(4.dp))
                SkeletonBox(width = 80.dp, height = 10.dp, cornerRadius = 3.dp)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        // Interleaved Chat Bubbles
        SkeletonChatBubbleItem(isUser = false)
        SkeletonChatBubbleItem(isUser = true)
        SkeletonChatBubbleItem(isUser = false)
        SkeletonChatBubbleItem(isUser = true)
        SkeletonChatBubbleItem(isUser = false)
    }
}
