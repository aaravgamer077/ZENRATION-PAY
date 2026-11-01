package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.SimulatedTransaction
import com.example.data.model.UpiQrPayload
import com.example.ui.components.SimulatedBadgePill
import com.example.ui.components.formatTransactionTimestamp
import com.example.ui.theme.ZenBlack
import com.example.ui.theme.ZenCardBorder
import com.example.ui.theme.ZenDeepPurple
import com.example.ui.theme.ZenElectricBlue
import com.example.ui.theme.ZenPurpleGlow
import com.example.ui.theme.ZenRoyalPurple
import com.example.ui.theme.ZenSimAmber
import com.example.ui.theme.ZenSurfaceDark
import com.example.ui.theme.ZenTextMuted
import com.example.ui.theme.ZenTextPrimary
import com.example.ui.theme.ZenTextSecondary
import java.util.Locale

@Composable
fun PaymentProcessingContent(
  payload: UpiQrPayload,
  amount: Double,
  stageMessage: String,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "sim_processing")
  val rotation by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(1400, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "ring_rotation",
    )
  val pulse by
    infiniteTransition.animateFloat(
      initialValue = 0.82f,
      targetValue = 1.15f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(850, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "ring_pulse",
    )

  Box(
    modifier = modifier.fillMaxSize().background(ZenBlack).padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      SimulatedBadgePill(
        text = "SANDBOX PAYMENT SIMULATOR • NO REAL MONEY",
        accentColor = ZenSimAmber,
      )

      Spacer(modifier = Modifier.height(36.dp))

      Box(
        modifier = Modifier.size(160.dp),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val center = Offset(size.width / 2f, size.height / 2f)
          val radius = size.minDimension * 0.38f

          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    ZenElectricBlue.copy(alpha = 0.25f * pulse),
                    ZenDeepPurple.copy(alpha = 0.15f),
                    Color.Transparent,
                  ),
                center = center,
                radius = radius * 1.4f,
              ),
            radius = radius * 1.4f,
            center = center,
          )

          drawCircle(
            color = ZenCardBorder,
            radius = radius,
            center = center,
            style = Stroke(width = 6.dp.toPx()),
          )

          drawArc(
            brush =
              Brush.sweepGradient(
                colors = listOf(Color.Transparent, ZenDeepPurple, ZenElectricBlue)
              ),
            startAngle = rotation,
            sweepAngle = 250f,
            useCenter = false,
            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
          )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "SIM",
            style = MaterialTheme.typography.titleLarge,
            color = ZenElectricBlue,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "ENGINE",
            style = MaterialTheme.typography.labelSmall,
            color = ZenPurpleGlow,
          )
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      Text(
        text = "₹${String.format(Locale.US, "%,.2f", amount)}",
        style = MaterialTheme.typography.displayMedium,
        color = ZenTextPrimary,
        fontWeight = FontWeight.Bold,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Simulating transfer to ${payload.recipientName}",
        style = MaterialTheme.typography.titleMedium,
        color = ZenTextSecondary,
        textAlign = TextAlign.Center,
      )

      Text(
        text = payload.recipientUpiId,
        style = MaterialTheme.typography.bodySmall,
        color = ZenPurpleGlow,
      )

      Spacer(modifier = Modifier.height(24.dp))

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = ZenSurfaceDark,
        border = BorderStroke(1.dp, ZenElectricBlue.copy(alpha = 0.4f)),
      ) {
        Text(
          text = stageMessage,
          style = MaterialTheme.typography.bodyMedium,
          color = ZenElectricBlue,
          modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}

/**
 * Simulated Payment Success Screen:
 * - Clearly displays "SIMULATED PAYMENT – NOT PAID"
 * - Displays the automatic Voice Announcement badge with the exact spoken phrase
 * - Never claims real money was transferred
 * - Includes a button labelled "FAKE" returning to the Home screen,
 *   positioned approximately 10 mm (~63.dp on 160dpi baseline) above the bottom navigation bar.
 */
@Composable
fun SimulatedPaymentSuccessContent(
  transaction: SimulatedTransaction,
  announcedSentence: String?,
  onFakeReturnHomeClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(ZenBlack)
        .padding(horizontal = 20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Scrollable upper receipt area
    Column(
      modifier =
        Modifier
          .weight(1f)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(top = 14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Prominent "SIMULATED PAYMENT – NOT PAID" Banner
      Surface(
        modifier = Modifier.fillMaxWidth().testTag("simulated_payment_not_paid_banner"),
        shape = RoundedCornerShape(18.dp),
        color = ZenSimAmber.copy(alpha = 0.16f),
        border = BorderStroke(1.5.dp, ZenSimAmber),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          Icon(
            imageVector = Icons.Default.WarningAmber,
            contentDescription = null,
            tint = ZenSimAmber,
            modifier = Modifier.size(24.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = stringResource(R.string.simulated_payment_banner),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = ZenSimAmber,
            textAlign = TextAlign.Center,
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Animated Simulated Checkmark Medallion
      Box(
        modifier =
          Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
              brush =
                Brush.linearGradient(
                  colors = listOf(ZenDeepPurple, ZenElectricBlue)
                )
            ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = "Simulation completed",
          tint = ZenBlack,
          modifier = Modifier.size(42.dp),
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "₹${String.format(Locale.US, "%,.2f", transaction.amount)}",
        style = MaterialTheme.typography.displayMedium,
        color = ZenTextPrimary,
        fontWeight = FontWeight.Bold,
      )

      Spacer(modifier = Modifier.height(6.dp))

      SimulatedBadgePill(
        text = "STATUS: SIMULATED • NOT PAID",
        accentColor = ZenElectricBlue,
      )

      // Voice Announcement Confirmation Strip
      if (!announcedSentence.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
          modifier = Modifier.fillMaxWidth().testTag("voice_announcement_banner"),
          shape = RoundedCornerShape(14.dp),
          color = ZenElectricBlue.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, ZenElectricBlue.copy(alpha = 0.45f)),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.VolumeUp,
              contentDescription = "Voice announcement played",
              tint = ZenElectricBlue,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "ZEN SOUNDBOX VOICE ANNOUNCEMENT:",
                style = MaterialTheme.typography.labelSmall,
                color = ZenElectricBlue,
              )
              Text(
                text = "\"$announcedSentence\"",
                style = MaterialTheme.typography.bodySmall,
                color = ZenTextPrimary,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Simulated Receipt Breakdown Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
        border = BorderStroke(1.dp, ZenCardBorder),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          ReceiptFieldRow(label = "Recipient Name", value = transaction.recipientName)
          ReceiptFieldRow(label = "Recipient UPI ID", value = transaction.recipientUpiId)
          HorizontalDivider(color = ZenCardBorder.copy(alpha = 0.6f))
          ReceiptFieldRow(
            label = "Payment Status",
            value = "SIMULATED PAYMENT – NOT PAID",
            valueColor = ZenSimAmber,
          )
          ReceiptFieldRow(
            label = "Simulation Ref",
            value = transaction.referenceId.ifBlank { transaction.id },
          )
          ReceiptFieldRow(
            label = "Date & Time",
            value = formatTransactionTimestamp(transaction.createdAt),
          )
          if (!transaction.note.isNullOrBlank()) {
            ReceiptFieldRow(label = "Note", value = transaction.note)
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = ZenDeepPurple.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, ZenRoyalPurple.copy(alpha = 0.4f)),
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = ZenElectricBlue,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.simulated_payment_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = ZenTextSecondary,
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
    }

    // Button labelled "FAKE" that returns to the home screen,
    // positioned approximately 10 mm (~63.dp on 160dpi baseline) above the bottom navigation bar.
    Button(
      onClick = onFakeReturnHomeClick,
      modifier =
        Modifier
          .fillMaxWidth()
          .height(56.dp)
          .testTag("fake_return_home_button"),
      shape = RoundedCornerShape(18.dp),
      colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(),
    ) {
      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .background(
              brush =
                Brush.horizontalGradient(
                  colors = listOf(ZenDeepPurple, ZenRoyalPurple, ZenElectricBlue)
                ),
              shape = RoundedCornerShape(18.dp),
            ),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = stringResource(R.string.fake_button_label),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 2.sp,
          color = ZenTextPrimary,
        )
      }
    }

    // 10 mm spacer (~63.dp) directly above the bottom navigation bar
    Spacer(modifier = Modifier.height(63.dp))
  }
}

@Composable
private fun ReceiptFieldRow(
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
