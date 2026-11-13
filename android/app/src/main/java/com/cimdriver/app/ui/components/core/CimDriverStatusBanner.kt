package com.cimdriver.app.ui.components.core

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cimdriver.app.ui.theme.AppIconSizes
import com.cimdriver.app.ui.theme.AppShapes
import com.cimdriver.app.ui.theme.AppSpacing

enum class BannerType {
    INFO,
    WARNING,
    DANGER,
    SUCCESS
}

/**
 * Standardized status banner for notifications, health alerts, and fiscal warnings.
 */
@Composable
fun CimDriverStatusBanner(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    type: BannerType = BannerType.INFO,
    icon: ImageVector? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    val (containerColor, contentColor, defaultIcon) = when (type) {
        BannerType.INFO -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Filled.Info
        )
        BannerType.WARNING -> Triple(
            Color(0xFFFEF3C7),
            Color(0xFF92400E),
            Icons.Filled.Warning
        )
        BannerType.DANGER -> Triple(
            Color(0xFFFEE2E2),
            Color(0xFFB91C1C),
            Icons.Filled.Error
        )
        BannerType.SUCCESS -> Triple(
            Color(0xFFDCFCE7),
            Color(0xFF15803D),
            Icons.Filled.Info
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.card,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon ?: defaultIcon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(AppIconSizes.lg)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.sm))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }

                if (onDismiss != null) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(AppIconSizes.lg)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Sluiten",
                            tint = contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(AppIconSizes.sm)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.9f)
            )

            if (!actionText.isNullOrBlank() && onActionClick != null) {
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                Button(
                    onClick = onActionClick,
                    shape = AppShapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = contentColor,
                        contentColor = containerColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = actionText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

