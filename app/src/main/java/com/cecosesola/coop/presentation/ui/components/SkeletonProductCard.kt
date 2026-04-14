package com.cecosesola.coop.presentation.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/**
 * Placeholder skeleton que imita exactamente la geometría de [ProductoCard].
 * Shimmer usa los mismos colores de superficie del tema activo.
 */
@Composable
fun SkeletonProductCard(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX = transition.animateFloat(
        initialValue  = -800f,
        targetValue   = 1200f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label         = "tx"
    )

    val shimmer = Brush.linearGradient(
        colorStops = arrayOf(
            0.0f  to MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            0.35f to MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            0.5f  to MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            0.65f to MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            1.0f  to MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        ),
        start = Offset(translateX.value, 0f),
        end   = Offset(translateX.value + 800f, 300f)
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape    = MaterialTheme.shapes.extraLarge,
        color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Placeholder imagen
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(shimmer)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(Modifier.fillMaxWidth(0.6f).height(14.dp).clip(MaterialTheme.shapes.small).background(shimmer))
                Box(Modifier.fillMaxWidth(0.37f).height(11.dp).clip(MaterialTheme.shapes.small).background(shimmer))
                Spacer(Modifier.height(2.dp))
                Box(Modifier.width(64.dp).height(14.dp).clip(MaterialTheme.shapes.small).background(shimmer))
            }

            // Placeholder botón
            Box(Modifier.size(40.dp).clip(MaterialTheme.shapes.medium).background(shimmer))
        }
    }
}
