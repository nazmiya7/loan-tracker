package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LoanTransaction
import com.example.data.TransactionType
import com.example.ui.LoanViewModel
import com.example.ui.theme.MoneyGreen
import com.example.ui.theme.MoneyRed
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun InterestTrendChartSection(
    transactions: List<LoanTransaction>,
    modifier: Modifier = Modifier
) {
    val interestTxs = remember(transactions) {
        transactions.filter { it.type == TransactionType.INTEREST_CHARGED }
    }

    if (interestTxs.isEmpty()) return

    var selectedIndex by remember(interestTxs) {
        mutableStateOf(interestTxs.lastIndex)
    }

    val selectedTx = interestTxs.getOrNull(selectedIndex) ?: interestTxs.last()
    val firstInterest = interestTxs.first().amount
    val lastInterest = interestTxs.last().amount
    val dropPercent = if (firstInterest > 0) {
        (((firstInterest - lastInterest) / firstInterest) * 100.0 * 10).roundToInt() / 10.0
    } else 0.0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("section_interest_trend_chart"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = null,
                        tint = MoneyRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Interest Charges Trend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Historical monthly debits over time",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = MoneyGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = MoneyGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "-$dropPercent% drop",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MoneyGreen
                        )
                    }
                }
            }

            // Interactive Point Detail Callout
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Selected: ${selectedTx.date}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = selectedTx.note.ifBlank { "Monthly Interest Debit" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = LoanViewModel.formatCurrency(selectedTx.amount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MoneyRed
                        )
                        Text(
                            text = "Bal: ${LoanViewModel.formatCurrency(selectedTx.resultingBalance)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Custom Smooth Canvas Trend Area Chart
            val maxAmount = remember(interestTxs) {
                kotlin.math.max(1.0, (interestTxs.maxOfOrNull { it.amount } ?: 10000.0) * 1.15)
            }
            val minAmount = 0.0

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(interestTxs) {
                            detectTapGestures { tapOffset ->
                                val count = interestTxs.size
                                if (count > 1) {
                                    val stepX = size.width / (count - 1)
                                    val tappedIndex = ((tapOffset.x + stepX / 2) / stepX)
                                        .toInt()
                                        .coerceIn(0, count - 1)
                                    selectedIndex = tappedIndex
                                }
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val count = interestTxs.size
                    if (count == 0) return@Canvas

                    // Draw 3 horizontal guideline reference lines
                    val gridLines = listOf(0.25f, 0.5f, 0.75f)
                    gridLines.forEach { ratio ->
                        val y = height * ratio
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.15f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Compute points
                    val points = interestTxs.mapIndexed { index, tx ->
                        val x = if (count > 1) (index.toFloat() / (count - 1)) * width else width / 2f
                        val normalized = ((tx.amount - minAmount) / (maxAmount - minAmount)).toFloat()
                        val y = height - (normalized * height).coerceIn(0f, height)
                        Offset(x, y)
                    }

                    // Build area path and line path
                    val strokePath = Path()
                    val fillPath = Path()

                    points.forEachIndexed { i, pt ->
                        if (i == 0) {
                            strokePath.moveTo(pt.x, pt.y)
                            fillPath.moveTo(pt.x, height)
                            fillPath.lineTo(pt.x, pt.y)
                        } else {
                            strokePath.lineTo(pt.x, pt.y)
                            fillPath.lineTo(pt.x, pt.y)
                        }
                    }

                    fillPath.lineTo(points.last().x, height)
                    fillPath.close()

                    // Draw gradient area fill
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MoneyRed.copy(alpha = 0.35f),
                                MoneyRed.copy(alpha = 0.03f)
                            ),
                            startY = 0f,
                            endY = height
                        )
                    )

                    // Draw smooth stroke line
                    drawPath(
                        path = strokePath,
                        color = MoneyRed,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )

                    // Draw point dots
                    points.forEachIndexed { index, pt ->
                        val isSelected = index == selectedIndex
                        val outerRadius = if (isSelected) 7.dp.toPx() else 4.5.dp.toPx()
                        val innerRadius = if (isSelected) 4.dp.toPx() else 2.5.dp.toPx()

                        // Outer ring
                        drawCircle(
                            color = if (isSelected) MoneyRed else Color.White,
                            radius = outerRadius,
                            center = pt
                        )
                        // Inner dot
                        drawCircle(
                            color = if (isSelected) Color.White else MoneyRed,
                            radius = innerRadius,
                            center = pt
                        )
                    }
                }
            }

            // X-Axis Labels Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                interestTxs.forEachIndexed { index, tx ->
                    val isSelected = index == selectedIndex
                    Text(
                        text = tx.date.split(" ").take(2).joinToString(" "),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MoneyRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Explanatory note below chart
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(MoneyGreen, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Prepayments reduced full-month interest charges from a peak of ₹9,884 (July) to ₹7,663 (August) — a genuine saving of ₹2,221/mo!",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}
