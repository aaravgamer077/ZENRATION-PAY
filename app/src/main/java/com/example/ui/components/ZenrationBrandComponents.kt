package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DemoBalance
import com.example.data.model.SimulatedTransaction
import com.example.ui.theme.ZenBlack
import com.example.ui.theme.ZenCardBorder
import com.example.ui.theme.ZenDeepPurple
import com.example.ui.theme.ZenElectricBlue
import com.example.ui.theme.ZenPurpleGlow
import com.example.ui.theme.ZenRoyalPurple
import com.example.ui.theme.ZenSimAmber
import com.example.ui.theme.ZenSuccessGreen
import com.example.ui.theme.ZenSurfaceDark
import com.example.ui.theme.ZenSurfaceElevated
import com.example.ui.theme.ZenTextMuted
import com.example.ui.theme.ZenTextPrimary
import com.example.ui.theme.ZenTextSecondary
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ZenrationLogoEmblem(
  size: Dp = 56.dp,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "logo_glow")
  val glowAlpha by
    infiniteTransition.animateFloat(
      initialValue = 0.45f,
      targetValue = 0.9f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(2200, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "glow_alpha",
    )

  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = this.size.width
      val h = this.size.height
      val center = Offset(w / 2f, h / 2f)

      // Outer glowing halo
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                ZenElectricBlue.copy(alpha = 0.35f * glowAlpha),
                ZenDeepPurple.copy(alpha = 0.25f * glowAlpha),
                Color.Transparent,
              ),
            center = center,
            radius = w * 0.52f,
          ),
        radius = w * 0.52f,
        center = center,
      )

      // Dark obsidian circular medallion
      drawCircle(
        brush =
          Brush.linearGradient(
            colors = listOf(Color(0xFF150E2E), Color(0xFF070510)),
            start = Offset(0f, 0f),
            end = Offset(w, h),
          ),
        radius = w * 0.44f,
        center = center,
      )

      // Neon border ring
      drawCircle(
        brush =
          Brush.sweepGradient(
            colors = listOf(ZenElectricBlue, ZenRoyalPurple, ZenDeepPurple, ZenElectricBlue)
          ),
        radius = w * 0.44f,
        center = center,
        style = Stroke(width = w * 0.045f),
      )

      // Geometric Z-Lightning Bolt Path
      val zPath =
        Path().apply {
          moveTo(w * 0.28f, h * 0.26f)
          lineTo(w * 0.74f, h * 0.26f)
          lineTo(w * 0.50f, h * 0.50f)
          lineTo(w * 0.72f, h * 0.50f)
          lineTo(w * 0.34f, h * 0.78f)
          lineTo(w * 0.46f, h * 0.54f)
          lineTo(w * 0.26f, h * 0.54f)
          close()
        }

      drawPath(
        path = zPath,
        brush =
          Brush.linearGradient(
            colors = listOf(ZenElectricBlue, ZenRoyalPurple),
            start = Offset(w * 0.25f, h * 0.25f),
            end = Offset(w * 0.75f, h * 0.78f),
          ),
      )
    }
  }
}

@Composable
fun SimulatedBadgePill(
  text: String = "SIMULATED",
  accentColor: Color = ZenSimAmber,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(50),
    color = accentColor.copy(alpha = 0.14f),
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Box(
        modifier =
          Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(accentColor)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = accentColor,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
fun DemoBalanceHeroCard(
  demoBalance: DemoBalance?,
  isLoading: Boolean,
  onResetBalanceClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isBalanceVisible by remember { mutableStateOf(true) }
  val amount = demoBalance?.balance ?: 25000.0

  Card(
    modifier = modifier.fillMaxWidth().testTag("demo_balance_card"),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    border = BorderStroke(1.2.dp, ZenElectricBlue.copy(alpha = 0.45f)),
  ) {
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(
            brush =
              Brush.linearGradient(
                colors =
                  listOf(
                    Color(0xFF1E0E4A),
                    Color(0xFF120B2E),
                    Color(0xFF071C2C),
                  ),
                start = Offset(0f, 0f),
                end = Offset(900f, 500f),
              )
          )
          .padding(22.dp)
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          SimulatedBadgePill(
            text = "DEMO BALANCE • NOT REAL MONEY",
            accentColor = ZenElectricBlue,
          )

          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.CloudDone,
              contentDescription = "Real-time cloud sync active",
              tint = ZenSuccessGreen,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "LIVE SYNC",
              style = MaterialTheme.typography.labelSmall,
              color = ZenSuccessGreen,
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Available Sandbox Credits",
              style = MaterialTheme.typography.bodySmall,
              color = ZenTextSecondary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text =
                if (isLoading) {
                  "Syncing..."
                } else if (isBalanceVisible) {
                  "₹${String.format(Locale.US, "%,.2f", amount)}"
                } else {
                  "₹ • • • • • •"
                },
              style = MaterialTheme.typography.displayMedium,
              color = ZenTextPrimary,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.testTag("demo_balance_amount_text"),
            )
          }

          IconButton(
            onClick = { isBalanceVisible = !isBalanceVisible },
            modifier =
              Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(ZenSurfaceElevated.copy(alpha = 0.7f)),
          ) {
            Icon(
              imageVector =
                if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
              contentDescription =
                if (isBalanceVisible) "Hide demo balance" else "Show demo balance",
              tint = ZenElectricBlue,
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(color = ZenCardBorder.copy(alpha = 0.6f))

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Updates automatically from ZENRATION PAY Web",
            style = MaterialTheme.typography.bodySmall,
            color = ZenTextSecondary,
            modifier = Modifier.weight(1f),
          )

          Surface(
            modifier =
              Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onResetBalanceClick)
                .testTag("reset_demo_balance_chip"),
            color = ZenElectricBlue.copy(alpha = 0.14f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, ZenElectricBlue.copy(alpha = 0.4f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reset demo balance",
                tint = ZenElectricBlue,
                modifier = Modifier.size(14.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Reset ₹25K",
                style = MaterialTheme.typography.labelSmall,
                color = ZenElectricBlue,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun TransactionItemCard(
  transaction: SimulatedTransaction,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .clickable(onClick = onClick)
        .testTag("transaction_item_${transaction.id}"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
    border = BorderStroke(1.dp, ZenCardBorder),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
              brush =
                Brush.linearGradient(
                  colors =
                    listOf(
                      ZenDeepPurple.copy(alpha = 0.4f),
                      ZenElectricBlue.copy(alpha = 0.25f),
                    )
                )
            )
            .border(1.dp, ZenElectricBlue.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.CallMade,
          contentDescription = "Simulated outgoing transaction",
          tint = ZenElectricBlue,
          modifier = Modifier.size(22.dp),
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = transaction.recipientName,
            style = MaterialTheme.typography.titleMedium,
            color = ZenTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = transaction.recipientUpiId,
          style = MaterialTheme.typography.bodySmall,
          color = ZenPurpleGlow,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = formatTransactionTimestamp(transaction.createdAt),
          style = MaterialTheme.typography.bodySmall,
          color = ZenTextMuted,
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = "₹${String.format(Locale.US, "%,.2f", transaction.amount)}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = ZenTextPrimary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        SimulatedBadgePill(
          text = "SIMULATED",
          accentColor = ZenSimAmber,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsModal(
  transaction: SimulatedTransaction,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = ZenSurfaceDark,
    contentColor = ZenTextPrimary,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp)
          .padding(bottom = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        SimulatedBadgePill(
          text = "SIMULATED PAYMENT – NOT PAID",
          accentColor = ZenSimAmber,
        )
        IconButton(onClick = onDismiss) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close transaction details",
            tint = ZenTextSecondary,
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "₹${String.format(Locale.US, "%,.2f", transaction.amount)}",
        style = MaterialTheme.typography.displayMedium,
        color = ZenElectricBlue,
        fontWeight = FontWeight.Bold,
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "To ${transaction.recipientName}",
        style = MaterialTheme.typography.titleLarge,
        color = ZenTextPrimary,
        textAlign = TextAlign.Center,
      )

      Text(
        text = transaction.recipientUpiId,
        style = MaterialTheme.typography.bodyMedium,
        color = ZenPurpleGlow,
      )

      Spacer(modifier = Modifier.height(20.dp))

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ZenBlack.copy(alpha = 0.65f)),
        border = BorderStroke(1.dp, ZenCardBorder),
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          DetailRow(label = "Status", value = "SIMULATED (NOT PAID)", valueColor = ZenSimAmber)
          DetailRow(label = "Reference ID", value = transaction.referenceId.ifBlank { transaction.id })
          DetailRow(label = "Recipient Name", value = transaction.recipientName)
          DetailRow(label = "Recipient UPI ID", value = transaction.recipientUpiId)
          DetailRow(
            label = "Date & Time",
            value = formatTransactionTimestamp(transaction.createdAt),
          )
          if (!transaction.note.isNullOrBlank()) {
            DetailRow(label = "Simulation Note", value = transaction.note)
          }
          DetailRow(label = "Transfer Type", value = "Sandbox Demo Ledger (Zero Bank Value)")
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = ZenDeepPurple.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, ZenRoyalPurple.copy(alpha = 0.45f)),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = ZenElectricBlue,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text =
              "SIMULATED ENTRY: No real money was transferred. This receipt is strictly for ZENRATION PAY sandbox demonstration.",
            style = MaterialTheme.typography.bodySmall,
            color = ZenTextSecondary,
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Button(
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("close_tx_detail_button"),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ZenRoyalPurple),
      ) {
        Text(
          text = "Done",
          style = MaterialTheme.typography.titleMedium,
          color = Color.White,
        )
      }
    }
  }
}

@Composable
private fun DetailRow(
  label: String,
  value: String,
  valueColor: Color = ZenTextPrimary,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = ZenTextMuted,
      modifier = Modifier.weight(0.4f),
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.SemiBold,
      color = valueColor,
      textAlign = TextAlign.End,
      modifier = Modifier.weight(0.6f),
    )
  }
}

fun formatTransactionTimestamp(timestamp: Timestamp?): String {
  val date = (timestamp ?: Timestamp.now()).toDate()
  val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
  return formatter.format(date)
}
