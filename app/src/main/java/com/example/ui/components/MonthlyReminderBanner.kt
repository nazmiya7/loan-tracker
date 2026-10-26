package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LoanViewModel
import com.example.ui.ReminderStatusInfo
import com.example.ui.theme.MoneyBlue
import com.example.ui.theme.MoneyGold
import com.example.ui.theme.MoneyGreen
import com.example.ui.theme.MoneyRed

@Composable
fun MonthlyReminderBanner(
    status: ReminderStatusInfo,
    onLogEmiPaid: () -> Unit,
    onLogPreviousMonthInterest: () -> Unit,
    onOpenReminderSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("banner_monthly_reminders"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Reminders",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MONTHLY REMINDERS & ALERTS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    onClick = onOpenReminderSettings,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configure reminders",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Setup",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Two Status Pills: 1st Day (Interest Log) & 10th Day (EMI Payment)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1st Day Pill
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = if (status.isInterestLoggedForPreviousMonth) MoneyGreen.copy(alpha = 0.12f) else MoneyRed.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (status.isInterestLoggedForPreviousMonth) MoneyGreen.copy(alpha = 0.4f) else MoneyRed.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (status.isInterestLoggedForPreviousMonth) Icons.Default.CheckCircle else Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = if (status.isInterestLoggedForPreviousMonth) MoneyGreen else MoneyRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Month-End Interest",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (status.isInterestLoggedForPreviousMonth) MoneyGreen else MoneyRed
                            )
                        }
                        Text(
                            text = if (status.isInterestLoggedForPreviousMonth) {
                                "${status.previousMonthLabel} Interest Logged"
                            } else {
                                "Log ${status.previousMonthLabel} Interest"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!status.isInterestLoggedForPreviousMonth) {
                            Button(
                                onClick = onLogPreviousMonthInterest,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MoneyRed),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Log ₹${LoanViewModel.formatCurrency(status.pendingInterestAmount)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                // 10th Day Pill
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = if (status.isEmiPaidThisMonth) MoneyGreen.copy(alpha = 0.12f) else MoneyGold.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (status.isEmiPaidThisMonth) MoneyGreen.copy(alpha = 0.4f) else MoneyGold.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (status.isEmiPaidThisMonth) Icons.Default.CheckCircle else Icons.Default.Payment,
                                contentDescription = null,
                                tint = if (status.isEmiPaidThisMonth) MoneyGreen else MoneyGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "10th of Month",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (status.isEmiPaidThisMonth) MoneyGreen else MoneyGold
                            )
                        }
                        Text(
                            text = if (status.isEmiPaidThisMonth) {
                                "${status.currentMonthLabel} EMI Paid"
                            } else {
                                "EMI Payment Due"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!status.isEmiPaidThisMonth) {
                            Button(
                                onClick = onLogEmiPaid,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Mark ₹${LoanViewModel.formatCurrency(status.emiExpectedAmount)}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
