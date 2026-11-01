package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SimulatedBadgePill
import com.example.ui.theme.ZenBlack
import com.example.ui.theme.ZenCardBorder
import com.example.ui.theme.ZenDeepPurple
import com.example.ui.theme.ZenElectricBlue
import com.example.ui.theme.ZenErrorRed
import com.example.ui.theme.ZenPurpleGlow
import com.example.ui.theme.ZenRoyalPurple
import com.example.ui.theme.ZenSimAmber
import com.example.ui.theme.ZenSuccessGreen
import com.example.ui.theme.ZenSurfaceDark
import com.example.ui.theme.ZenSurfaceElevated
import com.example.ui.theme.ZenTextMuted
import com.example.ui.theme.ZenTextPrimary
import com.example.ui.theme.ZenTextSecondary
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.delay

private enum class CustomBalancePhase {
  ENTER_AMOUNT,
  READY_FOR_PROTOCOL,
  PROTOCOL_MATRIX_ANIMATION,
  SYNCING_FIRESTORE,
  COMPLETED,
  ERROR,
}

private val MatrixDarkBlack = Color(0xFF020804)
private val MatrixDeepGreen = Color(0xFF041B0B)
private val MatrixNeonGreen = Color(0xFF00FF66)
private val MatrixBrightHead = Color(0xFFB9F6CA)
private val MatrixEmeraldGlow = Color(0xFF00E676)

@Composable
fun CustomBalanceScreen(
  currentBalance: Double?,
  onCommitCustomBalance: (Double, (Result<Double>) -> Unit) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val focusManager = LocalFocusManager.current
  var phase by rememberSaveable { mutableStateOf(CustomBalancePhase.ENTER_AMOUNT) }
  var amountInput by rememberSaveable { mutableStateOf("") }
  var validatedAmount by rememberSaveable { mutableStateOf(0.0) }
  var validationError by rememberSaveable { mutableStateOf<String?>(null) }
  var syncErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }
  var countdownSeconds by rememberSaveable { mutableIntStateOf(10) }
  var elapsedFraction by remember { mutableFloatStateOf(0f) }

  BackHandler {
    when (phase) {
      CustomBalancePhase.ENTER_AMOUNT -> onClose()
      CustomBalancePhase.READY_FOR_PROTOCOL -> phase = CustomBalancePhase.ENTER_AMOUNT
      CustomBalancePhase.PROTOCOL_MATRIX_ANIMATION -> {
        // Allow user to abort protocol and return to READY_FOR_PROTOCOL
        phase = CustomBalancePhase.READY_FOR_PROTOCOL
      }
      CustomBalancePhase.SYNCING_FIRESTORE -> {
        // Wait for Firestore sync
      }
      CustomBalancePhase.COMPLETED -> onClose()
      CustomBalancePhase.ERROR -> phase = CustomBalancePhase.READY_FOR_PROTOCOL
    }
  }

  fun validateAndProceed() {
    focusManager.clearFocus()
    val sanitized = amountInput.replace(",", "").replace("₹", "").trim()
    if (sanitized.isEmpty()) {
      validationError = "Please enter a custom simulated balance amount in ₹."
      return
    }
    val parsed = sanitized.toDoubleOrNull()
    if (parsed == null || parsed.isNaN() || parsed.isInfinite()) {
      validationError = "Invalid format. Please enter a numeric amount in ₹ (e.g., 50000)."
      return
    }
    if (parsed < 0.0) {
      validationError = "Simulated balance cannot be negative."
      return
    }
    if (parsed > 1_000_000_000.0) {
      validationError = "Maximum allowed simulated balance is ₹1,00,00,00,000."
      return
    }
    validationError = null
    validatedAmount = (parsed * 100.0).roundToInt() / 100.0
    phase = CustomBalancePhase.READY_FOR_PROTOCOL
  }

  fun triggerFirestoreSync(amountToSave: Double) {
    phase = CustomBalancePhase.SYNCING_FIRESTORE
    syncErrorMessage = null
    onCommitCustomBalance(amountToSave) { result ->
      result
        .onSuccess {
          phase = CustomBalancePhase.COMPLETED
        }
        .onFailure { err ->
          syncErrorMessage =
            err.localizedMessage ?: "Failed to update simulated balance in Cloud Firestore."
          phase = CustomBalancePhase.ERROR
        }
    }
  }

  // 10-second countdown timer when in PROTOCOL_MATRIX_ANIMATION
  LaunchedEffect(phase) {
    if (phase == CustomBalancePhase.PROTOCOL_MATRIX_ANIMATION) {
      countdownSeconds = 10
      elapsedFraction = 0f
      val totalDurationMs = 10_000L
      val tickMs = 50L
      var elapsedMs = 0L
      while (elapsedMs < totalDurationMs) {
        delay(tickMs)
        elapsedMs += tickMs
        elapsedFraction = (elapsedMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
        val remaining = ((totalDurationMs - elapsedMs + 999L) / 1000L).toInt().coerceIn(0, 10)
        countdownSeconds = remaining
      }
      countdownSeconds = 0
      elapsedFraction = 1f
      delay(250L)
      triggerFirestoreSync(validatedAmount)
    }
  }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(
          if (phase == CustomBalancePhase.PROTOCOL_MATRIX_ANIMATION) MatrixDarkBlack else ZenBlack
        )
        .testTag("custom_balance_screen")
  ) {
    AnimatedContent(
      targetState = phase,
      label = "CustomBalancePhaseTransition",
    ) { currentPhase ->
      when (currentPhase) {
        CustomBalancePhase.ENTER_AMOUNT,
        CustomBalancePhase.READY_FOR_PROTOCOL -> {
          CustomBalanceConfigView(
            isReadyForProtocol = currentPhase == CustomBalancePhase.READY_FOR_PROTOCOL,
            currentBalance = currentBalance,
            amountInput = amountInput,
            validatedAmount = validatedAmount,
            validationError = validationError,
            onAmountChange = {
              amountInput = it
              if (validationError != null) validationError = null
              if (currentPhase == CustomBalancePhase.READY_FOR_PROTOCOL) {
                phase = CustomBalancePhase.ENTER_AMOUNT
              }
            },
            onNextClick = ::validateAndProceed,
            onStartProtocolClick = {
              phase = CustomBalancePhase.PROTOCOL_MATRIX_ANIMATION
            },
            onEditAmountClick = {
              phase = CustomBalancePhase.ENTER_AMOUNT
            },
            onCancelClick = onClose,
          )
        }

        CustomBalancePhase.PROTOCOL_MATRIX_ANIMATION -> {
          MatrixBinaryProtocolScreen(
            countdownSeconds = countdownSeconds,
            progressFraction = elapsedFraction,
            targetAmount = validatedAmount,
            onAbortClick = { phase = CustomBalancePhase.READY_FOR_PROTOCOL },
          )
        }

        CustomBalancePhase.SYNCING_FIRESTORE -> {
          ProtocolSyncingView(targetAmount = validatedAmount)
        }

        CustomBalancePhase.COMPLETED -> {
          CustomBalanceCompletedView(
            updatedAmount = validatedAmount,
            onReturnHomeClick = onClose,
          )
        }

        CustomBalancePhase.ERROR -> {
          CustomBalanceErrorView(
            errorMessage = syncErrorMessage ?: "Unable to sync balance with Firestore.",
            targetAmount = validatedAmount,
            onRetryClick = { triggerFirestoreSync(validatedAmount) },
            onCancelClick = onClose,
          )
        }
      }
    }
  }
}

@Composable
private fun CustomBalanceConfigView(
  isReadyForProtocol: Boolean,
  currentBalance: Double?,
  amountInput: String,
  validatedAmount: Double,
  validationError: String?,
  onAmountChange: (String) -> Unit,
  onNextClick: () -> Unit,
  onStartProtocolClick: () -> Unit,
  onEditAmountClick: () -> Unit,
  onCancelClick: () -> Unit,
) {
  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Top App Bar
    Row(
      modifier = Modifier.fillMaxWidth().widthIn(max = 540.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      IconButton(
        onClick = onCancelClick,
        modifier =
          Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(ZenSurfaceElevated)
            .testTag("custom_balance_back_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back to Home",
          tint = ZenTextPrimary,
        )
      }

      SimulatedBadgePill(
        text = "SIMULATED BALANCE – NOT REAL MONEY",
        accentColor = ZenSimAmber,
      )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Column(
      modifier = Modifier.fillMaxWidth().widthIn(max = 540.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      // Title & Subtitle Header
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = "CUSTOM YOUR BALANCE",
          style = MaterialTheme.typography.headlineMedium,
          color = ZenTextPrimary,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 1.2.sp,
          modifier = Modifier.testTag("custom_balance_title"),
        )
        Text(
          text =
            "Configure a custom simulated balance in ₹ and synchronize it live with Firebase Firestore and the ZENRATION PAY ADMIN website.",
          style = MaterialTheme.typography.bodyMedium,
          color = ZenTextSecondary,
        )
      }

      // Live Current Simulated Balance Reference Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
        border = BorderStroke(1.dp, ZenCardBorder),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier =
                Modifier
                  .size(44.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(ZenDeepPurple.copy(alpha = 0.35f))
                  .border(1.dp, ZenElectricBlue.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = ZenElectricBlue,
              )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
              Text(
                text = "Current Simulated Balance",
                style = MaterialTheme.typography.labelMedium,
                color = ZenTextSecondary,
              )
              Text(
                text =
                  if (currentBalance != null) {
                    "₹${String.format(Locale.US, "%,.2f", currentBalance)}"
                  } else {
                    "Syncing from Cloud..."
                  },
                style = MaterialTheme.typography.titleLarge,
                color = ZenElectricBlue,
                fontWeight = FontWeight.Bold,
              )
            }
          }
          SimulatedBadgePill(text = "DEMO", accentColor = ZenPurpleGlow)
        }
      }

      // Custom Amount Entry Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
        border =
          BorderStroke(
            width = 1.5.dp,
            color =
              if (isReadyForProtocol) MatrixNeonGreen.copy(alpha = 0.7f) else ZenCardBorder,
          ),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(22.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Text(
            text = "Enter Custom Simulated Amount (₹)",
            style = MaterialTheme.typography.titleMedium,
            color = ZenTextPrimary,
            fontWeight = FontWeight.Bold,
          )

          OutlinedTextField(
            value = amountInput,
            onValueChange = onAmountChange,
            label = { Text("Custom Amount in ₹") },
            placeholder = { Text("Enter amount in ₹") },
            prefix = {
              Text(
                text = "₹ ",
                style = MaterialTheme.typography.titleLarge,
                color = ZenElectricBlue,
                fontWeight = FontWeight.Bold,
              )
            },
            singleLine = true,
            isError = validationError != null,
            keyboardOptions =
              KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
              ),
            keyboardActions = KeyboardActions(onDone = { onNextClick() }),
            modifier =
              Modifier
                .fillMaxWidth()
                .testTag("custom_balance_amount_input"),
            textStyle =
              MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = ZenTextPrimary,
              ),
            shape = RoundedCornerShape(16.dp),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ZenElectricBlue,
                unfocusedBorderColor = ZenCardBorder,
                errorBorderColor = ZenErrorRed,
                focusedTextColor = ZenTextPrimary,
                unfocusedTextColor = ZenTextPrimary,
              ),
          )

          if (validationError != null) {
            Surface(
              modifier = Modifier.fillMaxWidth().testTag("custom_balance_error_banner"),
              shape = RoundedCornerShape(12.dp),
              color = ZenErrorRed.copy(alpha = 0.14f),
              border = BorderStroke(1.dp, ZenErrorRed.copy(alpha = 0.5f)),
            ) {
              Text(
                text = validationError,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFF8A80),
                modifier = Modifier.padding(12.dp),
              )
            }
          }

          if (!isReadyForProtocol) {
            // Step 1 Buttons: NEXT and CANCEL
            Button(
              onClick = onNextClick,
              modifier =
                Modifier
                  .fillMaxWidth()
                  .height(56.dp)
                  .testTag("custom_balance_next_button"),
              shape = RoundedCornerShape(16.dp),
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
                      shape = RoundedCornerShape(16.dp),
                    ),
                contentAlignment = Alignment.Center,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center,
                ) {
                  Text(
                    text = "NEXT",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = ZenTextPrimary,
                    letterSpacing = 1.2.sp,
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = ZenTextPrimary,
                  )
                }
              }
            }
          } else {
            // Step 2: Validated Summary + START PROTOCOL Button
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(16.dp),
              color = MatrixDeepGreen.copy(alpha = 0.75f),
              border = BorderStroke(1.dp, MatrixNeonGreen.copy(alpha = 0.65f)),
            ) {
              Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = MatrixNeonGreen,
                    modifier = Modifier.size(20.dp),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "PROTOCOL READY • TARGET SIMULATED BALANCE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MatrixNeonGreen,
                    fontWeight = FontWeight.Bold,
                  )
                }
                Text(
                  text = "₹${String.format(Locale.US, "%,.2f", validatedAmount)}",
                  style = MaterialTheme.typography.headlineMedium,
                  color = Color.White,
                  fontWeight = FontWeight.ExtraBold,
                )
                Text(
                  text =
                    "Press START PROTOCOL below to launch the 10-second digital matrix synchronization sequence and update Cloud Firestore.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MatrixBrightHead,
                )
              }
            }

            Button(
              onClick = onStartProtocolClick,
              modifier =
                Modifier
                  .fillMaxWidth()
                  .height(60.dp)
                  .testTag("start_protocol_button"),
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
                          colors =
                            listOf(
                              Color(0xFF004D25),
                              Color(0xFF00C853),
                              MatrixNeonGreen,
                            )
                        ),
                      shape = RoundedCornerShape(18.dp),
                    )
                    .border(
                      width = 1.5.dp,
                      color = MatrixBrightHead,
                      shape = RoundedCornerShape(18.dp),
                    ),
                contentAlignment = Alignment.Center,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center,
                ) {
                  Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MatrixDarkBlack,
                    modifier = Modifier.size(26.dp),
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = "START PROTOCOL",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MatrixDarkBlack,
                    letterSpacing = 1.5.sp,
                  )
                }
              }
            }

            OutlinedButton(
              onClick = onEditAmountClick,
              modifier = Modifier.fillMaxWidth().height(48.dp),
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, ZenCardBorder),
            ) {
              Text(
                text = "Edit Amount",
                color = ZenTextSecondary,
              )
            }
          }

          OutlinedButton(
            onClick = onCancelClick,
            modifier =
              Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("custom_balance_cancel_button"),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, ZenCardBorder),
          ) {
            Text(
              text = "Cancel & Return to Home",
              color = ZenTextSecondary,
            )
          }
        }
      }

      // Mandatory Simulator Disclaimer Card
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = ZenSurfaceDark,
        border = BorderStroke(1.dp, ZenSimAmber.copy(alpha = 0.45f)),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = ZenSimAmber,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text =
              "SIMULATED BALANCE – NOT REAL MONEY: Custom balances are stored in Firestore for sandbox simulation only and never represent real bank funds.",
            style = MaterialTheme.typography.bodySmall,
            color = ZenTextSecondary,
          )
        }
      }
    }
  }
}

private class BinaryRainColumn(
  val xFraction: Float,
  val speedPxPerSec: Float,
  val charSpacingPx: Float,
  val streamLength: Int,
  val initialOffsetPx: Float,
  val seed: Int,
)

/**
 * 10-second Full-Screen Digital Matrix Live Wallpaper Animation with glowing green binary digits (0 and 1)
 * falling continuously from top to bottom and a centered 10 -> 0 countdown HUD.
 */
@Composable
private fun MatrixBinaryProtocolScreen(
  countdownSeconds: Int,
  progressFraction: Float,
  targetAmount: Double,
  onAbortClick: () -> Unit,
) {
  var frameTimeMs by remember { mutableLongStateOf(0L) }
  var startFrameMs by remember { mutableLongStateOf(0L) }

  LaunchedEffect(Unit) {
    while (true) {
      withFrameMillis { now ->
        if (startFrameMs == 0L) startFrameMs = now
        frameTimeMs = now - startFrameMs
      }
    }
  }

  val columns = remember {
    val rng = Random(20261101)
    List(44) { index ->
      BinaryRainColumn(
        xFraction = (index + 0.5f) / 44f,
        speedPxPerSec = rng.nextFloat() * 320f + 220f,
        charSpacingPx = rng.nextFloat() * 8f + 34f,
        streamLength = rng.nextInt(14, 30),
        initialOffsetPx = rng.nextFloat() * 1800f,
        seed = rng.nextInt(1000, 99999),
      )
    }
  }

  val animatedProgress by
    animateFloatAsState(
      targetValue = progressFraction,
      animationSpec = tween(durationMillis = 80, easing = LinearEasing),
      label = "MatrixCountdownProgress",
    )

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(
          brush =
            Brush.verticalGradient(
              colors = listOf(MatrixDarkBlack, MatrixDeepGreen, MatrixDarkBlack)
            )
        )
        .testTag("matrix_protocol_animation_screen")
  ) {
    // Full-screen responsive Canvas rendering glowing falling 0 and 1 binary streams
    Canvas(modifier = Modifier.fillMaxSize()) {
      val canvasWidth = size.width
      val canvasHeight = size.height
      if (canvasWidth <= 0f || canvasHeight <= 0f) return@Canvas

      val elapsedSec = frameTimeMs / 1000f
      val columnStep = (canvasWidth / columns.size).coerceAtLeast(18f)
      val textSizePx = (columnStep * 0.82f).coerceIn(22f, 40f)

      val glowPaint =
        android.graphics.Paint().apply {
          isAntiAlias = true
          typeface = android.graphics.Typeface.MONOSPACE
          isFakeBoldText = true
          textSize = textSizePx
        }

      columns.forEachIndexed { colIndex, col ->
        val xPos = col.xFraction * canvasWidth
        val cycleHeight = canvasHeight + col.streamLength * col.charSpacingPx + 240f
        val headY =
          ((col.initialOffsetPx + elapsedSec * col.speedPxPerSec) % cycleHeight) - 60f

        for (glyphIndex in 0 until col.streamLength) {
          val yPos = headY - glyphIndex * col.charSpacingPx
          if (yPos < -textSizePx || yPos > canvasHeight + textSizePx) continue

          // Deterministic fast-flickering 0 or 1 binary digit
          val timeBucket = ((frameTimeMs / 110L) + glyphIndex * 7 + col.seed + colIndex).toInt()
          val binaryChar = if ((timeBucket and 1) == 0) "1" else "0"

          when (glyphIndex) {
            0 -> {
              // Glowing leading binary glyph
              drawCircle(
                color = MatrixNeonGreen.copy(alpha = 0.28f),
                radius = textSizePx * 0.75f,
                center = Offset(xPos + textSizePx * 0.28f, yPos - textSizePx * 0.32f),
              )
              glowPaint.color = android.graphics.Color.argb(255, 200, 255, 218)
              glowPaint.setShadowLayer(
                18f,
                0f,
                0f,
                android.graphics.Color.argb(255, 0, 255, 102),
              )
            }
            in 1..3 -> {
              glowPaint.color = android.graphics.Color.argb(235, 0, 255, 102)
              glowPaint.setShadowLayer(
                12f,
                0f,
                0f,
                android.graphics.Color.argb(200, 0, 230, 118),
              )
            }
            else -> {
              val fade =
                (1f - (glyphIndex.toFloat() / col.streamLength.toFloat())).coerceIn(0.08f, 0.85f)
              val alphaInt = (fade * 210f).toInt().coerceIn(18, 210)
              glowPaint.color = android.graphics.Color.argb(alphaInt, 0, 210, 88)
              glowPaint.clearShadowLayer()
            }
          }

          drawContext.canvas.nativeCanvas.drawText(binaryChar, xPos, yPos, glowPaint)
        }
      }
    }

    // Center Futuristic Countdown & Protocol Telemetry HUD
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top Pill
      Surface(
        shape = RoundedCornerShape(50),
        color = MatrixDarkBlack.copy(alpha = 0.84f),
        border = BorderStroke(1.dp, MatrixNeonGreen.copy(alpha = 0.7f)),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(MatrixNeonGreen)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "PROTOCOL ACTIVE • SIMULATED BALANCE – NOT REAL MONEY",
            style =
              MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
              ),
            color = MatrixBrightHead,
          )
        }
      }

      // Center Countdown Ring (10 -> 0)
      Card(
        modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
        shape = RoundedCornerShape(28.dp),
        colors =
          CardDefaults.cardColors(
            containerColor = MatrixDarkBlack.copy(alpha = 0.86f)
          ),
        border = BorderStroke(1.5.dp, MatrixNeonGreen.copy(alpha = 0.75f)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(28.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Text(
            text = "EXECUTING BALANCE PROTOCOL",
            style =
              MaterialTheme.typography.titleSmall.copy(
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.6.sp,
              ),
            color = MatrixNeonGreen,
            fontWeight = FontWeight.Bold,
          )

          Box(
            modifier = Modifier.size(168.dp),
            contentAlignment = Alignment.Center,
          ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              val strokeWidth = 10.dp.toPx()
              drawCircle(
                color = MatrixNeonGreen.copy(alpha = 0.18f),
                style = Stroke(width = strokeWidth),
              )
              drawArc(
                color = MatrixNeonGreen,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
              )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = countdownSeconds.toString(),
                style =
                  MaterialTheme.typography.displayLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 64.sp,
                  ),
                color = MatrixBrightHead,
                modifier = Modifier.testTag("protocol_countdown_timer"),
              )
              Text(
                text = "SECONDS",
                style =
                  MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                  ),
                color = MatrixNeonGreen,
              )
            }
          }

          Text(
            text = "TARGET: ₹${String.format(Locale.US, "%,.2f", targetAmount)}",
            style =
              MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
              ),
            color = Color.White,
          )

          Text(
            text = "Injecting simulated balance nodes into Cloud Firestore (balances/{uid})...",
            style =
              MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
              ),
            color = MatrixBrightHead.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
          )
        }
      }

      // Bottom Cancel/Abort button
      OutlinedButton(
        onClick = onAbortClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MatrixNeonGreen.copy(alpha = 0.55f)),
        colors =
          ButtonDefaults.outlinedButtonColors(
            containerColor = MatrixDarkBlack.copy(alpha = 0.75f)
          ),
      ) {
        Text(
          text = "ABORT PROTOCOL",
          style =
            MaterialTheme.typography.labelMedium.copy(
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
            ),
          color = MatrixBrightHead,
        )
      }
    }
  }
}

@Composable
private fun ProtocolSyncingView(targetAmount: Double) {
  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(MatrixDarkBlack)
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      CircularProgressIndicator(
        color = MatrixNeonGreen,
        strokeWidth = 3.dp,
        modifier = Modifier.size(56.dp),
      )
      Text(
        text = "FINALIZING FIRESTORE SYNC...",
        style =
          MaterialTheme.typography.titleMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
          ),
        color = MatrixNeonGreen,
      )
      Text(
        text = "Updating simulated balance to ₹${String.format(Locale.US, "%,.2f", targetAmount)}",
        style = MaterialTheme.typography.bodyMedium,
        color = MatrixBrightHead,
      )
    }
  }
}

@Composable
private fun CustomBalanceCompletedView(
  updatedAmount: Double,
  onReturnHomeClick: () -> Unit,
) {
  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(ZenBlack)
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Card(
      modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
      shape = RoundedCornerShape(28.dp),
      colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
      border = BorderStroke(1.5.dp, MatrixEmeraldGlow.copy(alpha = 0.75f)),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Box(
          modifier =
            Modifier
              .size(76.dp)
              .clip(CircleShape)
              .background(MatrixEmeraldGlow.copy(alpha = 0.16f))
              .border(2.dp, MatrixEmeraldGlow, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Simulated Balance Updated",
            tint = MatrixEmeraldGlow,
            modifier = Modifier.size(44.dp),
          )
        }

        SimulatedBadgePill(
          text = "SIMULATED BALANCE – NOT REAL MONEY",
          accentColor = ZenSimAmber,
        )

        Text(
          text = "SIMULATED BALANCE UPDATED SUCCESSFULLY",
          style = MaterialTheme.typography.titleLarge,
          color = MatrixEmeraldGlow,
          fontWeight = FontWeight.ExtraBold,
          textAlign = TextAlign.Center,
          modifier = Modifier.testTag("simulated_balance_updated_text"),
        )

        Text(
          text = "₹${String.format(Locale.US, "%,.2f", updatedAmount)}",
          style = MaterialTheme.typography.displaySmall,
          color = ZenTextPrimary,
          fontWeight = FontWeight.ExtraBold,
          textAlign = TextAlign.Center,
        )

        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          color = ZenSurfaceElevated,
          border = BorderStroke(1.dp, ZenCardBorder),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.CloudDone,
                contentDescription = null,
                tint = ZenElectricBlue,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Synced with Firebase Firestore & ZENRATION PAY ADMIN",
                style = MaterialTheme.typography.labelMedium,
                color = ZenElectricBlue,
                fontWeight = FontWeight.Bold,
              )
            }
            Text(
              text = "SIMULATED BALANCE – NOT REAL MONEY",
              style = MaterialTheme.typography.labelSmall,
              color = ZenSimAmber,
              fontWeight = FontWeight.ExtraBold,
              modifier = Modifier.testTag("simulated_balance_not_real_money_text"),
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Button(
          onClick = onReturnHomeClick,
          modifier =
            Modifier
              .fillMaxWidth()
              .height(56.dp)
              .testTag("custom_balance_return_home_button"),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(containerColor = ZenRoyalPurple),
        ) {
          Text(
            text = "RETURN TO HOME",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
          )
        }
      }
    }
  }
}

@Composable
private fun CustomBalanceErrorView(
  errorMessage: String,
  targetAmount: Double,
  onRetryClick: () -> Unit,
  onCancelClick: () -> Unit,
) {
  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(ZenBlack)
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Card(
      modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
      border = BorderStroke(1.dp, ZenErrorRed.copy(alpha = 0.6f)),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Icon(
          imageVector = Icons.Default.ErrorOutline,
          contentDescription = null,
          tint = ZenErrorRed,
          modifier = Modifier.size(48.dp),
        )
        Text(
          text = "Firestore Sync Error",
          style = MaterialTheme.typography.titleLarge,
          color = ZenTextPrimary,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = errorMessage,
          style = MaterialTheme.typography.bodyMedium,
          color = ZenTextSecondary,
          textAlign = TextAlign.Center,
        )
        Button(
          onClick = onRetryClick,
          modifier = Modifier.fillMaxWidth().height(52.dp),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = ZenElectricBlue),
        ) {
          Text(
            text = "Retry Sync (₹${String.format(Locale.US, "%,.2f", targetAmount)})",
            color = ZenBlack,
            fontWeight = FontWeight.Bold,
          )
        }
        OutlinedButton(
          onClick = onCancelClick,
          modifier = Modifier.fillMaxWidth().height(48.dp),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, ZenCardBorder),
        ) {
          Text(text = "Cancel", color = ZenTextSecondary)
        }
      }
    }
  }
}
