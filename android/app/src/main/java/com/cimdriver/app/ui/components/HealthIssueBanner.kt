package com.cimdriver.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cimdriver.app.util.HealthIssue
import com.cimdriver.app.util.HealthMonitorHelper

@Composable
fun HealthIssueBanner(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var issues by remember { mutableStateOf<List<HealthIssue>>(emptyList()) }

    LaunchedEffect(Unit) {
        issues = HealthMonitorHelper.checkHealth(context)
    }

    if (issues.isNotEmpty()) {
        val primaryIssue = issues.first()
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (primaryIssue.isCritical) MaterialTheme.colorScheme.errorContainer else Color(0xFFFEF3C7)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (primaryIssue.id == "battery_optimization") Icons.Filled.BatteryAlert else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (primaryIssue.isCritical) MaterialTheme.colorScheme.error else Color(0xFFD97706),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = primaryIssue.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (primaryIssue.isCritical) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF92400E)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = primaryIssue.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (primaryIssue.isCritical) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF78350F)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            try {
                                val intent = primaryIssue.createIntent(context)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.util.Log.e("HealthIssueBanner", "Could not start intent", e)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (primaryIssue.isCritical) MaterialTheme.colorScheme.error else Color(0xFFD97706),
                            contentColor = Color.White
                        )
                    ) {
                        Text(primaryIssue.actionLabel)
                    }
                }
            }
        }
    }
}

