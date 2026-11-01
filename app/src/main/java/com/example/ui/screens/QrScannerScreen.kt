package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.DemoBalance
import com.example.data.model.UpiQrPayload
import com.example.ui.components.SimulatedBadgePill
import com.example.ui.theme.ZenBlack
import com.example.ui.theme.ZenCardBorder
import com.example.ui.theme.ZenDeepPurple
import com.example.ui.theme.ZenElectricBlue
import com.example.ui.theme.ZenErrorRed
import com.example.ui.theme.ZenPurpleGlow
import com.example.ui.theme.ZenRoyalPurple
import com.example.ui.theme.ZenSimAmber
import com.example.ui.theme.ZenSurfaceDark
import com.example.ui.theme.ZenSurfaceElevated
import com.example.ui.theme.ZenTextMuted
import com.example.ui.theme.ZenTextPrimary
import com.example.ui.theme.ZenTextSecondary
import com.example.ui.viewmodel.PaymentFlowState
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.Locale
import java.util.concurrent.Executors

@Composable
fun QrScannerScreen(
  paymentFlowState: PaymentFlowState,
  demoBalance: DemoBalance?,
  onQrDetected: (String) -> Unit,
  onSelectPreset: (UpiQrPayload) -> Unit,
  onSimulatePayment: (UpiQrPayload, String, String) -> Unit,
  onResetScanner: () -> Unit,
  onFakeReturnHome: () -> Unit,
  modifier: Modifier = Modifier,
) {
  when (paymentFlowState) {
    is PaymentFlowState.IdleScanning -> {
      CameraQrScannerView(
        errorBanner = null,
        onQrDetected = onQrDetected,
        onSelectPreset = onSelectPreset,
        modifier = modifier,
      )
    }

    is PaymentFlowState.Error -> {
      if (paymentFlowState.previousPayload != null) {
        UpiAmountEntryView(
          payload = paymentFlowState.previousPayload,
          demoBalance = demoBalance,
          errorMessage = paymentFlowState.message,
          onBackToScanner = onResetScanner,
          onSimulatePayment = onSimulatePayment,
          modifier = modifier,
        )
      } else {
        CameraQrScannerView(
          errorBanner = paymentFlowState.message,
          onQrDetected = onQrDetected,
          onSelectPreset = onSelectPreset,
          modifier = modifier,
        )
      }
    }

    is PaymentFlowState.EnterAmount -> {
      UpiAmountEntryView(
        payload = paymentFlowState.payload,
        demoBalance = demoBalance,
        errorMessage = null,
        onBackToScanner = onResetScanner,
        onSimulatePayment = onSimulatePayment,
        modifier = modifier,
      )
    }

    is PaymentFlowState.Processing -> {
      PaymentProcessingContent(
        payload = paymentFlowState.payload,
        amount = paymentFlowState.amount,
        stageMessage = paymentFlowState.stageMessage,
        modifier = modifier,
      )
    }

    is PaymentFlowState.SimulatedSuccess -> {
      SimulatedPaymentSuccessContent(
        transaction = paymentFlowState.transaction,
        announcedSentence = paymentFlowState.announcedSentence,
        onFakeReturnHomeClick = onFakeReturnHome,
        modifier = modifier,
      )
    }
  }
}

@Composable
private fun CameraQrScannerView(
  errorBanner: String?,
  onQrDetected: (String) -> Unit,
  onSelectPreset: (UpiQrPayload) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  val permissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
      hasCameraPermission = granted
    }

  LaunchedEffect(Unit) {
    if (!hasCameraPermission) {
      permissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  var manualUpiInput by remember { mutableStateOf("") }
  var isTorchEnabled by remember { mutableStateOf(false) }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(ZenBlack)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text(
          text = "Scan UPI QR Code",
          style = MaterialTheme.typography.headlineMedium,
          color = ZenTextPrimary,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Reads standard upi://pay QR codes • Zero real charges",
          style = MaterialTheme.typography.bodySmall,
          color = ZenTextSecondary,
        )
      }
      SimulatedBadgePill(text = "SIMULATOR", accentColor = ZenElectricBlue)
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (!errorBanner.isNullOrBlank()) {
      Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        shape = RoundedCornerShape(14.dp),
        color = ZenErrorRed.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, ZenErrorRed),
      ) {
        Text(
          text = errorBanner,
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFFFF8A80),
          modifier = Modifier.padding(12.dp),
        )
      }
    }

    // CameraX Viewfinder Box
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(270.dp)
          .clip(RoundedCornerShape(26.dp))
          .background(ZenSurfaceDark)
          .border(1.5.dp, ZenElectricBlue.copy(alpha = 0.55f), RoundedCornerShape(26.dp))
          .testTag("qr_camera_viewfinder"),
      contentAlignment = Alignment.Center,
    ) {
      if (hasCameraPermission) {
        CameraXQrPreview(
          isTorchOn = isTorchEnabled,
          onQrCodeScanned = onQrDetected,
          modifier = Modifier.fillMaxSize(),
        )
        ScannerReticleOverlay(modifier = Modifier.fillMaxSize())

        IconButton(
          onClick = { isTorchEnabled = !isTorchEnabled },
          modifier =
            Modifier
              .align(Alignment.TopEnd)
              .padding(12.dp)
              .size(44.dp)
              .clip(CircleShape)
              .background(ZenBlack.copy(alpha = 0.65f)),
        ) {
          Icon(
            imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
            contentDescription = "Toggle camera flash",
            tint = if (isTorchEnabled) ZenSimAmber else ZenTextPrimary,
          )
        }
      } else {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            tint = ZenElectricBlue,
            modifier = Modifier.size(44.dp),
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Camera Permission Required for Live QR Scanning",
            style = MaterialTheme.typography.titleSmall,
            color = ZenTextPrimary,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text =
              "Grant camera access to scan physical UPI QR codes, or use the simulated UPI QR presets below.",
            style = MaterialTheme.typography.bodySmall,
            color = ZenTextSecondary,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedButton(
            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            border = BorderStroke(1.dp, ZenElectricBlue),
          ) {
            Text(text = "Grant Camera Permission", color = ZenElectricBlue)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Simulated UPI QR Sample Cards for instant testing
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
      border = BorderStroke(1.dp, ZenCardBorder),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.QrCode2,
            contentDescription = null,
            tint = ZenElectricBlue,
            modifier = Modifier.size(22.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Tap a Sample UPI QR to Simulate Scan",
            style = MaterialTheme.typography.titleSmall,
            color = ZenTextPrimary,
          )
        }

        val sampleQrs =
          listOf(
            UpiQrPayload(
              recipientUpiId = "starlightcoffee@okaxis",
              recipientName = "Starlight Coffee Roasters",
              amount = "320",
              note = "Iced Pour Over",
              rawQrString =
                "upi://pay?pa=starlightcoffee@okaxis&pn=Starlight%20Coffee%20Roasters&am=320&cu=INR",
            ),
            UpiQrPayload(
              recipientUpiId = "nebulagaming@ybl",
              recipientName = "Nebula Esports Lounge",
              amount = "850",
              note = "VR Pass",
              rawQrString =
                "upi://pay?pa=nebulagaming@ybl&pn=Nebula%20Esports%20Lounge&am=850&cu=INR",
            ),
            UpiQrPayload(
              recipientUpiId = "urbanbistro@icici",
              recipientName = "Urban Spice Kitchen",
              amount = "",
              note = "",
              rawQrString = "upi://pay?pa=urbanbistro@icici&pn=Urban%20Spice%20Kitchen&cu=INR",
            ),
          )

        sampleQrs.forEachIndexed { index, sample ->
          Surface(
            modifier =
              Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onSelectPreset(sample) }
                .testTag("sample_qr_preset_$index"),
            shape = RoundedCornerShape(14.dp),
            color = ZenSurfaceElevated,
            border = BorderStroke(1.dp, ZenCardBorder),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = sample.recipientName,
                  style = MaterialTheme.typography.titleSmall,
                  color = ZenTextPrimary,
                )
                Text(
                  text = sample.recipientUpiId,
                  style = MaterialTheme.typography.bodySmall,
                  color = ZenPurpleGlow,
                )
              }
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = ZenElectricBlue.copy(alpha = 0.16f),
              ) {
                Text(
                  text = if (sample.amount.isNotBlank()) "Scan ₹${sample.amount}" else "Scan QR",
                  style = MaterialTheme.typography.labelSmall,
                  color = ZenElectricBlue,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Manual UPI QR URI or UPI ID Entry
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
        Text(
          text = "Or Paste UPI QR String / Enter UPI ID",
          style = MaterialTheme.typography.titleSmall,
          color = ZenTextPrimary,
        )
        OutlinedTextField(
          value = manualUpiInput,
          onValueChange = { manualUpiInput = it },
          placeholder = {
            Text(
              text = "e.g. merchant@okaxis or upi://pay?pa=...",
              color = ZenTextMuted,
            )
          },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("manual_upi_qr_input"),
          shape = RoundedCornerShape(14.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = ZenElectricBlue,
              unfocusedBorderColor = ZenCardBorder,
              focusedTextColor = ZenTextPrimary,
              unfocusedTextColor = ZenTextPrimary,
            ),
        )
        Button(
          onClick = {
            if (manualUpiInput.isNotBlank()) {
              onQrDetected(manualUpiInput)
            }
          },
          enabled = manualUpiInput.isNotBlank(),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("parse_upi_qr_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = ZenRoyalPurple),
        ) {
          Text(
            text = "Read UPI Information",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
          )
        }
      }
    }
  }
}

@Composable
private fun UpiAmountEntryView(
  payload: UpiQrPayload,
  demoBalance: DemoBalance?,
  errorMessage: String?,
  onBackToScanner: () -> Unit,
  onSimulatePayment: (UpiQrPayload, String, String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var amountText by remember(payload) { mutableStateOf(payload.amount) }
  var noteText by remember(payload) { mutableStateOf(payload.note) }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(ZenBlack)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      IconButton(
        onClick = onBackToScanner,
        modifier =
          Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(ZenSurfaceElevated)
            .testTag("back_to_qr_scanner_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back to QR scanner",
          tint = ZenTextPrimary,
        )
      }

      SimulatedBadgePill(
        text = "SIMULATED UPI PAYMENT",
        accentColor = ZenSimAmber,
      )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Verified Recipient Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
      border = BorderStroke(1.2.dp, ZenElectricBlue.copy(alpha = 0.5f)),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(
          modifier =
            Modifier
              .size(56.dp)
              .clip(CircleShape)
              .background(
                brush = Brush.linearGradient(colors = listOf(ZenDeepPurple, ZenElectricBlue))
              ),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = payload.recipientName.take(2).uppercase(),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = payload.recipientName,
            style = MaterialTheme.typography.titleLarge,
            color = ZenTextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("parsed_recipient_name"),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Icon(
            imageVector = Icons.Default.Verified,
            contentDescription = "Parsed UPI QR recipient",
            tint = ZenElectricBlue,
            modifier = Modifier.size(18.dp),
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = "UPI ID: ${payload.recipientUpiId}",
          style = MaterialTheme.typography.bodyMedium,
          color = ZenPurpleGlow,
          modifier = Modifier.testTag("parsed_recipient_upi_id"),
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    if (!errorMessage.isNullOrBlank()) {
      Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        shape = RoundedCornerShape(14.dp),
        color = ZenErrorRed.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, ZenErrorRed),
      ) {
        Text(
          text = errorMessage,
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFFFF8A80),
          modifier = Modifier.padding(12.dp),
        )
      }
    }

    // Amount Entry Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
      border = BorderStroke(1.dp, ZenCardBorder),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = "Enter Simulation Amount (INR)",
          style = MaterialTheme.typography.labelMedium,
          color = ZenTextSecondary,
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = amountText,
          onValueChange = { input ->
            if (input.all { it.isDigit() || it == '.' }) {
              amountText = input
            }
          },
          prefix = {
            Text(
              text = "₹ ",
              style = MaterialTheme.typography.headlineLarge,
              color = ZenElectricBlue,
              fontWeight = FontWeight.Bold,
            )
          },
          placeholder = {
            Text(
              text = "0.00",
              style = MaterialTheme.typography.headlineLarge,
              color = ZenTextMuted,
            )
          },
          textStyle =
            MaterialTheme.typography.headlineLarge.copy(
              color = ZenTextPrimary,
              fontWeight = FontWeight.Bold,
            ),
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth().testTag("simulation_amount_input"),
          shape = RoundedCornerShape(18.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = ZenElectricBlue,
              unfocusedBorderColor = ZenCardBorder,
            ),
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Amount Chips
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          listOf("100", "250", "500", "2000").forEach { presetAmt ->
            Surface(
              modifier =
                Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { amountText = presetAmt },
              shape = RoundedCornerShape(12.dp),
              color = ZenSurfaceElevated,
              border = BorderStroke(1.dp, ZenCardBorder),
            ) {
              Text(
                text = "₹$presetAmt",
                style = MaterialTheme.typography.labelMedium,
                color = ZenElectricBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
          value = noteText,
          onValueChange = { noteText = it },
          label = { Text("Add Simulation Note (Optional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("simulation_note_input"),
          shape = RoundedCornerShape(14.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = ZenRoyalPurple,
              unfocusedBorderColor = ZenCardBorder,
              focusedTextColor = ZenTextPrimary,
              unfocusedTextColor = ZenTextPrimary,
            ),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text =
            "Available Demo Balance: ₹${String.format(Locale.US, "%,.2f", demoBalance?.balance ?: 25000.0)} (NOT REAL MONEY)",
          style = MaterialTheme.typography.bodySmall,
          color = ZenTextMuted,
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Button(
      onClick = { onSimulatePayment(payload, amountText, noteText) },
      modifier =
        Modifier
          .fillMaxWidth()
          .height(58.dp)
          .testTag("simulate_payment_submit_button"),
      shape = RoundedCornerShape(18.dp),
      colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
      contentPadding = PaddingValues(),
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
          text = "SIMULATE PAYMENT (NO REAL CHARGE)",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = ZenTextPrimary,
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = Icons.Default.Shield,
        contentDescription = null,
        tint = ZenSimAmber,
        modifier = Modifier.size(16.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "Do not initiate real UPI transactions • Sandbox Simulation Only",
        style = MaterialTheme.typography.bodySmall,
        color = ZenTextSecondary,
      )
    }
  }
}

@Composable
private fun ScannerReticleOverlay(modifier: Modifier = Modifier) {
  val infiniteTransition = rememberInfiniteTransition(label = "laser_scan")
  val scanProgress by
    infiniteTransition.animateFloat(
      initialValue = 0.15f,
      targetValue = 0.85f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(1800, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "laser_line",
    )

  Canvas(modifier = modifier) {
    val boxSize = size.minDimension * 0.68f
    val left = (size.width - boxSize) / 2f
    val top = (size.height - boxSize) / 2f

    // Reticle frame
    drawRoundRect(
      color = ZenElectricBlue.copy(alpha = 0.85f),
      topLeft = Offset(left, top),
      size = Size(boxSize, boxSize),
      cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
      style = Stroke(width = 3.dp.toPx()),
    )

    // Laser scan line
    val laserY = top + boxSize * scanProgress
    drawLine(
      brush =
        Brush.horizontalGradient(
          colors = listOf(Color.Transparent, ZenElectricBlue, ZenRoyalPurple, Color.Transparent),
          startX = left,
          endX = left + boxSize,
        ),
      start = Offset(left + 12f, laserY),
      end = Offset(left + boxSize - 12f, laserY),
      strokeWidth = 3.5.dp.toPx(),
    )
  }
}

@Composable
private fun CameraXQrPreview(
  isTorchOn: Boolean,
  onQrCodeScanned: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val previewView = remember { PreviewView(context) }
  val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
  var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }

  LaunchedEffect(isTorchOn, cameraControl) {
    cameraControl?.enableTorch(isTorchOn)
  }

  DisposableEffect(lifecycleOwner) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    val listener = Runnable {
      try {
        val cameraProvider = cameraProviderFuture.get()
        val preview =
          Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
          }

        val qrReader =
          MultiFormatReader().apply {
            setHints(
              mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true,
              )
            )
          }

        val imageAnalysis =
          ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
              analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                decodeQrFromImageProxy(imageProxy, qrReader)?.let { qrText ->
                  previewView.post { onQrCodeScanned(qrText) }
                }
              }
            }

        cameraProvider.unbindAll()
        val camera =
          cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageAnalysis,
          )
        cameraControl = camera.cameraControl
      } catch (e: Exception) {
        // Ignore camera bind errors on headless/virtual devices without physical camera
      }
    }
    cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

    onDispose {
      try {
        cameraProviderFuture.get().unbindAll()
      } catch (_: Exception) {}
      cameraExecutor.shutdown()
    }
  }

  AndroidView(
    factory = { previewView },
    modifier = modifier,
  )
}

private fun decodeQrFromImageProxy(
  imageProxy: ImageProxy,
  reader: MultiFormatReader,
): String? {
  return try {
    val plane = imageProxy.planes.firstOrNull() ?: return null
    val buffer = plane.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val width = imageProxy.width
    val height = imageProxy.height
    val source =
      PlanarYUVLuminanceSource(
        bytes,
        width,
        height,
        0,
        0,
        width,
        height,
        false,
      )
    val bitmap = BinaryBitmap(HybridBinarizer(source))
    val result = reader.decodeWithState(bitmap)
    result.text
  } catch (_: Exception) {
    null
  } finally {
    reader.reset()
    imageProxy.close()
  }
}
