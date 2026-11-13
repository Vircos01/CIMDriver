package com.cimdriver.app.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cimdriver.app.ui.theme.AppIconSizes
import com.cimdriver.app.ui.theme.AppShapes
import com.cimdriver.app.ui.theme.AppSpacing

/**
 * Standardized bottom action bar for form screens (Add/Edit screens).
 * Provides a clear visual hierarchy with a prominent primary button, optional secondary/cancel button,
 * and optional destructive/delete button.
 */
@Composable
fun CimDriverPrimaryActionBar(
    primaryText: String,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryIcon: ImageVector? = null,
    primaryEnabled: Boolean = true,
    secondaryText: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    destructiveText: String? = null,
    onDestructiveClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(AppSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!secondaryText.isNullOrBlank() && onSecondaryClick != null) {
                    OutlinedButton(
                        onClick = onSecondaryClick,
                        modifier = Modifier.weight(1f),
                        shape = AppShapes.button
                    ) {
                        Text(text = secondaryText)
                    }
                }

                Button(
                    onClick = onPrimaryClick,
                    enabled = primaryEnabled,
                    modifier = Modifier.weight(if (secondaryText != null) 1.5f else 1f),
                    shape = AppShapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (primaryIcon != null) {
                        Icon(
                            imageVector = primaryIcon,
                            contentDescription = null,
                            modifier = Modifier.size(AppIconSizes.sm)
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.sm))
                    }
                    Text(
                        text = primaryText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!destructiveText.isNullOrBlank() && onDestructiveClick != null) {
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                TextButton(
                    onClick = onDestructiveClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(text = destructiveText)
                }
            }
        }
    }
}

