package com.cimdriver.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cimdriver.app.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cimdriver.app.ui.viewmodel.DistanceChartItem
import com.cimdriver.app.ui.viewmodel.WorkHoursChartItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun DistanceStackedChart(
    data: List<DistanceChartItem>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    var animationPlayed by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val animationProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000)
    )

    androidx.compose.runtime.LaunchedEffect(data) {
        animationPlayed = true
    }

    val maxTotal = data.maxOfOrNull { it.totalKm } ?: 0.0
    val maxValue = if (maxTotal > 0) maxTotal else 1.0

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(vertical = 16.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            data.forEachIndexed { index, item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    val totalFraction = (item.totalKm / maxValue).toFloat() * animationProgress
                    val spacerWeight = 1f - totalFraction
                    
                    if (spacerWeight > 0f) {
                        Spacer(modifier = Modifier.weight(spacerWeight.coerceAtLeast(0.001f)))
                    }

                    if (totalFraction > 0f) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(totalFraction.coerceAtLeast(0.001f))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            val zakelijkFraction = if (item.totalKm > 0) (item.zakelijkKm / item.totalKm).toFloat() else 0f
                            val priveFraction = if (item.totalKm > 0) (item.priveKm / item.totalKm).toFloat() else 0f
                            val woonWerkFraction = if (item.totalKm > 0) (item.woonWerkKm / item.totalKm).toFloat() else 0f

                            if (priveFraction > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(priveFraction)
                                        .background(Color(0xFFF57C00)) // Orange for Privé
                                )
                            }
                            if (woonWerkFraction > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(woonWerkFraction)
                                        .background(Color(0xFF0288D1)) // Blue for Woon-werk
                                )
                            }
                            if (zakelijkFraction > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(zakelijkFraction)
                                        .background(MaterialTheme.colorScheme.primary) // Navy/Primary for Zakelijk
                                )
                            }
                        }
                    } else {
                        // Empty placeholder to keep label at bottom
                        Spacer(modifier = Modifier.weight(0.001f))
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    val showLabel = data.size <= 15 || index == 0 || index == data.size - 1 || (index + 1) % 5 == 0
                    Text(
                        text = if (showLabel) item.label else " ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        softWrap = false,
                        modifier = Modifier.wrapContentWidth(unbounded = true)
                    )
                }
            }
        }
        
        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = MaterialTheme.colorScheme.primary, label = stringResource(R.string.chart_business))
            Spacer(modifier = Modifier.width(16.dp))
            LegendItem(color = Color(0xFF0288D1), label = stringResource(R.string.chart_commute))
            Spacer(modifier = Modifier.width(16.dp))
            LegendItem(color = Color(0xFFF57C00), label = stringResource(R.string.chart_private))
        }
    }
}

@Composable
fun WorkHoursBarChart(
    data: List<WorkHoursChartItem>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    var animationPlayed by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val animationProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000)
    )

    androidx.compose.runtime.LaunchedEffect(data) {
        animationPlayed = true
    }

    val maxTotal = data.maxOfOrNull { it.hours } ?: 0.0
    val maxValue = if (maxTotal > 0) maxTotal else 1.0

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(vertical = 16.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            data.forEachIndexed { index, item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    val barFraction = (item.hours / maxValue).toFloat() * animationProgress
                    val spacerWeight = 1f - barFraction
                    
                    if (spacerWeight > 0f) {
                        Spacer(modifier = Modifier.weight(spacerWeight.coerceAtLeast(0.001f)))
                    }

                    if (barFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(barFraction.coerceAtLeast(0.001f))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(0.001f))
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val showLabel = data.size <= 15 || index == 0 || index == data.size - 1 || (index + 1) % 5 == 0
                    Text(
                        text = if (showLabel) item.label else " ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        softWrap = false,
                        modifier = Modifier.wrapContentWidth(unbounded = true)
                    )
                }
            }
        }
        
        Text(
            text = stringResource(R.string.chart_hours_per_unit),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CategoryPieChart(
    data: List<com.cimdriver.app.ui.viewmodel.CategoryBreakdownItem>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    
    var animationPlayed by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val animationProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1500)
    )

    androidx.compose.runtime.LaunchedEffect(data) {
        animationPlayed = true
    }
    
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        Color(0xFF0288D1),
        Color(0xFF00796B),
        Color(0xFFF57C00),
        Color(0xFFD32F2F),
        Color(0xFF7B1FA2),
        Color(0xFF388E3C),
        Color(0xFFFBC02D)
    )
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp)
        ) {
            var startAngle = -90f
            data.forEachIndexed { index, item ->
                val sweepAngle = ((item.percentage / 100f) * 360f) * animationProgress
                val color = colors[index % colors.size]
                
                if (sweepAngle > 0) {
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        size = size
                    )
                    startAngle += sweepAngle
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            data.forEachIndexed { index, item ->
                val color = colors[index % colors.size]
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.categoryName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = String.format(java.util.Locale.getDefault(), "%.1f km (%.1f%%)", item.distanceKm, item.percentage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
