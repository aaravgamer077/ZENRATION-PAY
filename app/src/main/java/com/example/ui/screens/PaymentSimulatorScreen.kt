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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.PaymentStyle
import com.example.data.model.SimulatedTransaction
import com.example.data.model.UpiQrPayload
import com.example.ui.components.SimulatedBadgePill
import com.example.ui.components.ZenrationLogoEmblem
import com.example.ui.components.formatTransactionTimestamp
import com.example.ui.theme.ZenBlack
import com.example.ui.theme.ZenCardBorder
import com.example.ui.theme.ZenDeepPurple
import com.example.ui.theme.ZenElectricBlue
import com.example.ui.theme.ZenPurpleGlow
import com.example.ui.theme.ZenRoyalPurple
import com.example.ui.theme.ZenSimAmber
import com.example.ui.theme.ZenSurfaceDark
import com.example.ui.theme.ZenSurfaceElevated
import com.example.ui.theme.ZenTextMuted
import com.example.ui.theme.ZenTextPrimary
import com.example.ui.theme.ZenTextSecondary
import java.util.Locale

private val PaytmDeepBlue = Color(0xFF002E6E)
private val PaytmElectricCyan = Color(0xFF00B9F1)
private val PaytmLightCyan = Color(0xFF00E5FF)

private val PhonePeDeepPurple = Color(0xFF3B0A75)
private val PhonePeRichPurple = Color(0xFF5F259F)
private val PhonePeLightViolet = Color(0xFFB388FF)

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
        text = "${payload.recipientMobile} • ${payload.recipientUpiId}",
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
 * 9 & 10. PAYMENT RESULT SCREEN – PAYTM STYLE & PHONEPE STYLE
 * - Supports both PAYTM-INSPIRED (Blue & Light-Cyan) and PHONEPE-INSPIRED (Purple) result designs
 * - Features original ZENRATION PAY branding
 * - Features a clearly visible diagonal & badge SIMULATED watermark
 * - Clearly labels Status: SIMULATED – NOT PAID
 * - Displays the automatic Hindi Voice Announcement banner ("<amount> rupaye ki simulated payment hui.")
 */
@Composable
fun SimulatedPaymentSuccessContent(
  transaction: SimulatedTransaction,
  announcedSentence: String?,
  onFakeReturnHomeClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var activeResultStyle by remember(transaction.paymentStyle) {
    mutableStateOf(PaymentStyle.fromId(transaction.paymentStyle))
  }

  val isPaytm = activeResultStyle == PaymentStyle.PAYTM_INSPIRED
  val primaryAccent = if (isPaytm) PaytmLightCyan else PhonePeLightViolet
  val secondaryAccent = if (isPaytm) PaytmElectricCyan else PhonePeRichPurple
  val headerGradient =
    if (isPaytm) {
      listOf(PaytmDeepBlue, Color(0xFF00529B), PaytmElectricCyan)
    } else {
      listOf(PhonePeDeepPurple, PhonePeRichPurple, Color(0xFF7C4DFF))
    }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(
          brush =
            Brush.verticalGradient(
              colors =
                if (isPaytm) {
                  listOf(Color(0xFF021329), ZenBlack, Color(0xFF021022))
                } else {
                  listOf(Color(0xFF1A0633), ZenBlack, Color(0xFF120424))
                }
            )
        )
        .testTag("simulated_result_screen")
  ) {
    // Clearly visible full-screen diagonal "SIMULATED – NOT PAID" Watermark
    SimulatedWatermarkOverlay(
      watermarkColor = primaryAccent.copy(alpha = 0.08f),
      modifier = Modifier.fillMaxSize(),
    )

    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(horizontal = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier =
          Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Style Toggle Row + ZENRATION PAY Branding Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            ZenrationLogoEmblem(size = 38.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "ZENRATION PAY",
                style = MaterialTheme.typography.titleSmall,
                color = ZenTextPrimary,
                fontWeight = FontWeight.ExtraBold,
              )
              Text(
                text =
                  if (isPaytm) {
                    "PAYTM-INSPIRED SIMULATOR"
                  } else {
                    "PHONEPE-INSPIRED SIMULATOR"
                  },
                style = MaterialTheme.typography.labelSmall,
                color = primaryAccent,
                fontWeight = FontWeight.Bold,
              )
            }
          }

          // Switch between Paytm Style & PhonePe Style Result View
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
              modifier =
                Modifier
                  .clip(RoundedCornerShape(50))
                  .clickable { activeResultStyle = PaymentStyle.PAYTM_INSPIRED }
                  .testTag("result_style_paytm_toggle"),
              shape = RoundedCornerShape(50),
              color =
                if (isPaytm) PaytmElectricCyan.copy(alpha = 0.28f) else ZenSurfaceElevated,
              border =
                BorderStroke(
                  1.dp,
                  if (isPaytm) PaytmLightCyan else ZenCardBorder,
                ),
            ) {
              Text(
                text = "Paytm Style",
                style = MaterialTheme.typography.labelSmall,
                color = if (isPaytm) PaytmLightCyan else ZenTextSecondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              )
            }

            Surface(
              modifier =
                Modifier
                  .clip(RoundedCornerShape(50))
                  .clickable { activeResultStyle = PaymentStyle.PHONEPE_INSPIRED }
                  .testTag("result_style_phonepe_toggle"),
              shape = RoundedCornerShape(50),
              color =
                if (!isPaytm) PhonePeRichPurple.copy(alpha = 0.35f) else ZenSurfaceElevated,
              border =
                BorderStroke(
                  1.dp,
                  if (!isPaytm) PhonePeLightViolet else ZenCardBorder,
                ),
            ) {
              Text(
                text = "PhonePe Style",
                style = MaterialTheme.typography.labelSmall,
                color = if (!isPaytm) PhonePeLightViolet else ZenTextSecondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              )
            }
          }
        }

        // Prominent "SIMULATED – NOT PAID" Banner
        Surface(
          modifier = Modifier.fillMaxWidth().testTag("simulated_payment_not_paid_banner"),
          shape = RoundedCornerShape(16.dp),
          color = ZenSimAmber.copy(alpha = 0.16f),
          border = BorderStroke(1.5.dp, ZenSimAmber),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            Icon(
              imageVector = Icons.Default.WarningAmber,
              contentDescription = null,
              tint = ZenSimAmber,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "STATUS: SIMULATED – NOT PAID (DEMO ONLY)",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.ExtraBold,
              color = ZenSimAmber,
              textAlign = TextAlign.Center,
            )
          }
        }

        // Main Hero Result Card (Paytm Blue/Cyan or PhonePe Purple)
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(26.dp),
          colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
          border = BorderStroke(1.8.dp, primaryAccent.copy(alpha = 0.75f)),
        ) {
          Box(modifier = Modifier.fillMaxWidth()) {
            // Card-level visible SIMULATED watermark pill
            Surface(
              modifier =
                Modifier
                  .align(Alignment.TopEnd)
                  .padding(14.dp)
                  .testTag("simulated_watermark_badge"),
              shape = RoundedCornerShape(8.dp),
              color = ZenSimAmber.copy(alpha = 0.18f),
              border = BorderStroke(1.dp, ZenSimAmber),
            ) {
              Text(
                text = "SIMULATED WATERMARK",
                style =
                  MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                  ),
                color = ZenSimAmber,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }

            Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              // Colored Top Banner inside Result Card
              Box(
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .background(brush = Brush.horizontalGradient(colors = headerGradient))
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center,
              ) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Box(
                    modifier =
                      Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.28f))
                        .border(2.dp, primaryAccent, CircleShape),
                    contentAlignment = Alignment.Center,
                  ) {
                    Icon(
                      imageVector = Icons.Default.CheckCircle,
                      contentDescription = "Simulation Completed",
                      tint = primaryAccent,
                      modifier = Modifier.size(36.dp),
                    )
                  }

                  Text(
                    text =
                      if (isPaytm) {
                        "ZENRATION PAY • PAYTM-STYLE SIMULATION"
                      } else {
                        "ZENRATION PAY • PHONEPE-STYLE SIMULATION"
                      },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold,
                  )

                  Text(
                    text = "₹${String.format(Locale.US, "%,.2f", transaction.amount)}",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                  )

                  SimulatedBadgePill(
                    text = "SIMULATED – NOT PAID",
                    accentColor = ZenSimAmber,
                  )
                }
              }

              // Detailed Fields for Paytm-Style vs PhonePe-Style
              Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                if (isPaytm) {
                  // 9. PAYMENT RESULT SCREEN – PAYTM STYLE
                  ReceiptFieldRow(
                    label = "Platform Branding",
                    value = "ZENRATION PAY (Paytm-Inspired)",
                    valueColor = PaytmLightCyan,
                  )
                  ReceiptFieldRow(
                    label = "Recipient Name",
                    value = transaction.recipientName,
                  )
                  ReceiptFieldRow(
                    label = "Recipient Mobile Number",
                    value = transaction.recipientMobile,
                    valueColor = PaytmLightCyan,
                  )
                  ReceiptFieldRow(
                    label = "Amount Entered",
                    value = "₹${String.format(Locale.US, "%,.2f", transaction.amount)} (SIMULATED)",
                  )
                  HorizontalDivider(color = ZenCardBorder.copy(alpha = 0.6f))
                  ReceiptFieldRow(
                    label = "Unique DEMO Transaction ID",
                    value = transaction.referenceId.ifBlank { transaction.id },
                    valueColor = PaytmLightCyan,
                  )
                  ReceiptFieldRow(
                    label = "Date & Time",
                    value = formatTransactionTimestamp(transaction.createdAt),
                  )
                  ReceiptFieldRow(
                    label = "Status",
                    value = "SIMULATED – NOT PAID",
                    valueColor = ZenSimAmber,
                  )
                } else {
                  // 10. PAYMENT RESULT SCREEN – PHONEPE STYLE
                  ReceiptFieldRow(
                    label = "Platform Branding",
                    value = "ZENRATION PAY (PhonePe-Inspired)",
                    valueColor = PhonePeLightViolet,
                  )
                  ReceiptFieldRow(
                    label = "Transaction ID",
                    value = transaction.referenceId.ifBlank { transaction.id },
                    valueColor = PhonePeLightViolet,
                  )
                  ReceiptFieldRow(
                    label = "Paid To (Simulated)",
                    value = transaction.recipientName,
                  )
                  ReceiptFieldRow(
                    label = "Recipient Mobile Number",
                    value = transaction.recipientMobile,
                    valueColor = PhonePeLightViolet,
                  )
                  ReceiptFieldRow(
                    label = "Simulated Amount",
                    value = "₹${String.format(Locale.US, "%,.2f", transaction.amount)}",
                  )
                  HorizontalDivider(color = ZenCardBorder.copy(alpha = 0.6f))
                  ReceiptFieldRow(
                    label = "Simulated Sender Details",
                    value = "${transaction.senderName} (${transaction.senderUpiId})",
                  )
                  ReceiptFieldRow(
                    label = "Date & Time",
                    value = formatTransactionTimestamp(transaction.createdAt),
                  )
                  ReceiptFieldRow(
                    label = "Status",
                    value = "SIMULATED – NOT PAID",
                    valueColor = ZenSimAmber,
                  )
                }

                if (!transaction.note.isNullOrBlank()) {
                  ReceiptFieldRow(label = "Note", value = transaction.note)
                }
              }
            }
          }
        }

        // Dynamic Voice Announcement Banner
        if (!announcedSentence.isNullOrBlank()) {
          Surface(
            modifier = Modifier.fillMaxWidth().testTag("voice_announcement_banner"),
            shape = RoundedCornerShape(14.dp),
            color = secondaryAccent.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.55f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Voice announcement played",
                tint = primaryAccent,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "DYNAMIC HINDI VOICE ANNOUNCEMENT:",
                  style = MaterialTheme.typography.labelSmall,
                  color = primaryAccent,
                  fontWeight = FontWeight.Bold,
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

        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          color = ZenSurfaceDark,
          border = BorderStroke(1.dp, ZenCardBorder),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = primaryAccent,
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

        Spacer(modifier = Modifier.height(8.dp))
      }

      // Return to Home button
      Button(
        onClick = onFakeReturnHomeClick,
        modifier =
          Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("fake_return_home_button"),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
      ) {
        Box(
          modifier =
            Modifier
              .fillMaxSize()
              .background(
                brush = Brush.horizontalGradient(colors = headerGradient),
                shape = RoundedCornerShape(18.dp),
              ),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "${stringResource(R.string.fake_button_label)} • DONE (SIMULATED)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp,
            color = Color.White,
          )
        }
      }

      Spacer(modifier = Modifier.height(63.dp))
    }
  }
}

@Composable
private fun SimulatedWatermarkOverlay(
  watermarkColor: Color,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    val paint =
      android.graphics.Paint().apply {
        isAntiAlias = true
        typeface = android.graphics.Typeface.MONOSPACE
        isFakeBoldText = true
        textSize = 42f
        color =
          android.graphics.Color.argb(
            (watermarkColor.alpha * 255).toInt().coerceIn(12, 60),
            (watermarkColor.red * 255).toInt(),
            (watermarkColor.green * 255).toInt(),
            (watermarkColor.blue * 255).toInt(),
          )
      }

    rotate(degrees = -26f) {
      val stepY = 180f
      val stepX = 460f
      var y = -200f
      while (y < size.height + 400f) {
        var x = -300f
        while (x < size.width + 400f) {
          drawContext.canvas.nativeCanvas.drawText(
            "SIMULATED – NOT PAID",
            x,
            y,
            paint,
          )
          x += stepX
        }
        y += stepY
      }
    }
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
      modifier = Modifier.weight(0.42f),
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.SemiBold,
      color = valueColor,
      textAlign = TextAlign.End,
      modifier = Modifier.weight(0.58f),
    )
  }
}
