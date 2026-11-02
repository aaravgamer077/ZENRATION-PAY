package com.example.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentStyle
import com.example.data.repository.ZenrationRepository
import com.example.ui.components.SimulatedBadgePill
import com.example.ui.components.ZenrationLogoEmblem
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
import kotlin.random.Random
import kotlinx.coroutines.delay

enum class OnboardingStage {
  OWNER_LOGIN,
  MATRIX_ANIMATION,
  STYLE_SELECTION,
}

private val MatrixBgBlack = Color(0xFF010703)
private val MatrixBgDeepGreen = Color(0xFF041D0C)
private val MatrixNeonGreen = Color(0xFF00FF66)
private val MatrixGlowHead = Color(0xFFC8FFD8)

@Composable
fun ZenOwnerLoginAndStyleFlow(
  initialUsername: String,
  initialStage: OnboardingStage = OnboardingStage.OWNER_LOGIN,
  onCompleted: (username: String, selectedStyle: PaymentStyle) -> Unit,
  modifier: Modifier = Modifier,
) {
  var stage by rememberSaveable { mutableStateOf(initialStage) }
  var usernameInput by rememberSaveable { mutableStateOf(initialUsername) }

  AnimatedContent(
    targetState = stage,
    label = "ZenOwnerOnboardingTransition",
    modifier = modifier.fillMaxSize(),
  ) { currentStage ->
    when (currentStage) {
      OnboardingStage.OWNER_LOGIN -> {
        ZenOwnerLoginView(
          username = usernameInput,
          onUsernameChange = { usernameInput = it },
          onLoginVerified = { verifiedUsername ->
            usernameInput = verifiedUsername
            stage = OnboardingStage.MATRIX_ANIMATION
          },
        )
      }

      OnboardingStage.MATRIX_ANIMATION -> {
        MatrixLoginAnimationView(
          username = usernameInput.ifBlank { "Operator" },
          onAnimationFinished = {
            stage = OnboardingStage.STYLE_SELECTION
          },
        )
      }

      OnboardingStage.STYLE_SELECTION -> {
        PaymentStyleSelectionScreen(
          username = usernameInput.ifBlank { "Zenration Player" },
          onSelectStyle = { chosenStyle ->
            onCompleted(usernameInput.trim().ifBlank { "Zenration Player" }, chosenStyle)
          },
        )
      }
    }
  }
}

@Composable
private fun ZenOwnerLoginView(
  username: String,
  onUsernameChange: (String) -> Unit,
  onLoginVerified: (String) -> Unit,
) {
  val focusManager = LocalFocusManager.current
  var passwordInput by rememberSaveable { mutableStateOf("") }
  var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
  var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

  fun attemptLogin() {
    focusManager.clearFocus()
    val cleanUser = username.trim()
    if (cleanUser.isEmpty()) {
      errorMessage = "Please enter a username to continue."
      return
    }
    if (passwordInput.isBlank()) {
      errorMessage = "Please enter the owner access password."
      return
    }
    if (!ZenrationRepository.verifyOwnerPassword(passwordInput)) {
      errorMessage = "Incorrect owner password. Access denied."
      return
    }
    errorMessage = null
    onLoginVerified(cleanUser)
  }

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(
          brush =
            Brush.verticalGradient(
              colors = listOf(Color(0xFF0A0618), ZenBlack, Color(0xFF050B16))
            )
        )
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .testTag("owner_login_screen"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .widthIn(max = 460.dp)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      SimulatedBadgePill(
        text = "SIMULATED FINTECH SANDBOX • NOT REAL MONEY",
        accentColor = ZenElectricBlue,
      )

      ZenrationLogoEmblem(size = 84.dp)

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "ZENRATION PAY",
          style = MaterialTheme.typography.headlineLarge,
          color = ZenTextPrimary,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 1.8.sp,
          modifier = Modifier.testTag("login_app_title"),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Secure Operator Login • Remembered on Device",
          style = MaterialTheme.typography.bodySmall,
          color = ZenPurpleGlow,
        )
      }

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
        border = BorderStroke(1.2.dp, ZenElectricBlue.copy(alpha = 0.45f)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(22.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Text(
            text = "Player Credentials",
            style = MaterialTheme.typography.titleMedium,
            color = ZenTextPrimary,
            fontWeight = FontWeight.Bold,
          )

          OutlinedTextField(
            value = username,
            onValueChange = {
              onUsernameChange(it)
              if (errorMessage != null) errorMessage = null
            },
            label = { Text("Username") },
            placeholder = { Text("Choose any player username") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Username",
                tint = ZenElectricBlue,
              )
            },
            singleLine = true,
            keyboardOptions =
              KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
              ),
            modifier = Modifier.fillMaxWidth().testTag("login_username_input"),
            shape = RoundedCornerShape(16.dp),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ZenElectricBlue,
                unfocusedBorderColor = ZenCardBorder,
                focusedTextColor = ZenTextPrimary,
                unfocusedTextColor = ZenTextPrimary,
              ),
          )

          OutlinedTextField(
            value = passwordInput,
            onValueChange = {
              passwordInput = it
              if (errorMessage != null) errorMessage = null
            },
            label = { Text("Owner Password") },
            placeholder = { Text("Enter owner-controlled password") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Owner Password",
                tint = ZenPurpleGlow,
              )
            },
            trailingIcon = {
              IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                Icon(
                  imageVector =
                    if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription =
                    if (isPasswordVisible) "Hide password" else "Show password",
                  tint = ZenTextSecondary,
                )
              }
            },
            visualTransformation =
              if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            isError = errorMessage != null,
            keyboardOptions =
              KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
              ),
            keyboardActions = KeyboardActions(onDone = { attemptLogin() }),
            modifier = Modifier.fillMaxWidth().testTag("login_password_input"),
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

          if (errorMessage != null) {
            Surface(
              modifier = Modifier.fillMaxWidth().testTag("login_error_banner"),
              shape = RoundedCornerShape(14.dp),
              color = ZenErrorRed.copy(alpha = 0.15f),
              border = BorderStroke(1.dp, ZenErrorRed.copy(alpha = 0.6f)),
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Default.ErrorOutline,
                  contentDescription = "Login error",
                  tint = ZenErrorRed,
                  modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = errorMessage.orEmpty(),
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFFFF8A80),
                )
              }
            }
          }

          Button(
            onClick = ::attemptLogin,
            modifier =
              Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("login_submit_button"),
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
                  text = "LOGIN",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.ExtraBold,
                  color = ZenTextPrimary,
                  letterSpacing = 1.5.sp,
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
        }
      }

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
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text =
              "SIMULATED ENVIRONMENT: Your chosen username is remembered locally. All balances and transfers are 100% simulated.",
            style = MaterialTheme.typography.bodySmall,
            color = ZenTextSecondary,
          )
        }
      }
    }
  }
}

private class LoginMatrixStream(
  val xFraction: Float,
  val speedPxPerSec: Float,
  val spacingPx: Float,
  val length: Int,
  val offsetPx: Float,
  val seed: Int,
)

/**
 * 2. MATRIX LOGIN ANIMATION
 * Full-screen dark green and black background with glowing green binary digits (1 0 1 0 1 0 0 0 1 0)
 * continuously falling from top to bottom before opening the payment style selection / main dashboard.
 */
@Composable
fun MatrixLoginAnimationView(
  username: String,
  onAnimationFinished: () -> Unit,
) {
  var frameTimeMs by remember { mutableLongStateOf(0L) }
  var startFrameMs by remember { mutableLongStateOf(0L) }
  var progress by remember { mutableFloatStateOf(0f) }

  LaunchedEffect(Unit) {
    while (true) {
      withFrameMillis { now ->
        if (startFrameMs == 0L) startFrameMs = now
        frameTimeMs = now - startFrameMs
      }
    }
  }

  LaunchedEffect(Unit) {
    val totalMs = 2600L
    val stepMs = 40L
    var elapsed = 0L
    while (elapsed < totalMs) {
      delay(stepMs)
      elapsed += stepMs
      progress = (elapsed.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    }
    progress = 1f
    onAnimationFinished()
  }

  val streams = remember {
    val rng = Random(998877)
    List(42) { idx ->
      LoginMatrixStream(
        xFraction = (idx + 0.5f) / 42f,
        speedPxPerSec = rng.nextFloat() * 360f + 260f,
        spacingPx = rng.nextFloat() * 8f + 34f,
        length = rng.nextInt(14, 28),
        offsetPx = rng.nextFloat() * 1600f,
        seed = rng.nextInt(100, 99999),
      )
    }
  }

  val binaryPattern = remember { charArrayOf('1', '0', '1', '0', '1', '0', '0', '0', '1', '0') }

  val animatedProgress by
    animateFloatAsState(
      targetValue = progress,
      animationSpec = tween(durationMillis = 60, easing = LinearEasing),
      label = "MatrixLoginProgress",
    )

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(
          brush =
            Brush.verticalGradient(
              colors = listOf(MatrixBgBlack, MatrixBgDeepGreen, MatrixBgBlack)
            )
        )
        .clickable { onAnimationFinished() }
        .testTag("matrix_login_animation_screen")
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      if (w <= 0f || h <= 0f) return@Canvas

      val elapsedSec = frameTimeMs / 1000f
      val colWidth = (w / streams.size).coerceAtLeast(18f)
      val fontSizePx = (colWidth * 0.84f).coerceIn(22f, 38f)

      val paint =
        android.graphics.Paint().apply {
          isAntiAlias = true
          typeface = android.graphics.Typeface.MONOSPACE
          isFakeBoldText = true
          textSize = fontSizePx
        }

      streams.forEachIndexed { colIdx, stream ->
        val x = stream.xFraction * w
        val cycleH = h + stream.length * stream.spacingPx + 220f
        val headY = ((stream.offsetPx + elapsedSec * stream.speedPxPerSec) % cycleH) - 40f

        for (i in 0 until stream.length) {
          val y = headY - i * stream.spacingPx
          if (y < -fontSizePx || y > h + fontSizePx) continue

          val patternIdx =
            Math.floorMod(((frameTimeMs / 95L).toInt() + i + colIdx + stream.seed), binaryPattern.size)
          val digit = binaryPattern[patternIdx].toString()

          when (i) {
            0 -> {
              drawCircle(
                color = MatrixNeonGreen.copy(alpha = 0.26f),
                radius = fontSizePx * 0.72f,
                center = Offset(x + fontSizePx * 0.28f, y - fontSizePx * 0.32f),
              )
              paint.color = android.graphics.Color.argb(255, 210, 255, 224)
              paint.setShadowLayer(18f, 0f, 0f, android.graphics.Color.argb(255, 0, 255, 102))
            }
            in 1..3 -> {
              paint.color = android.graphics.Color.argb(235, 0, 255, 102)
              paint.setShadowLayer(12f, 0f, 0f, android.graphics.Color.argb(200, 0, 230, 118))
            }
            else -> {
              val fade = (1f - (i.toFloat() / stream.length.toFloat())).coerceIn(0.1f, 0.85f)
              val alpha = (fade * 210f).toInt().coerceIn(20, 210)
              paint.color = android.graphics.Color.argb(alpha, 0, 215, 90)
              paint.clearShadowLayer()
            }
          }

          drawContext.canvas.nativeCanvas.drawText(digit, x, y, paint)
        }
      }
    }

    // Center Matrix HUD
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Card(
        modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MatrixBgBlack.copy(alpha = 0.86f)),
        border = BorderStroke(1.5.dp, MatrixNeonGreen.copy(alpha = 0.75f)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(26.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Text(
            text = "1 0 1 0 1 0 0 0 1 0",
            style =
              MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Monospace,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.ExtraBold,
              ),
            color = MatrixNeonGreen,
          )

          Text(
            text = "ZENRATION PAY MATRIX CORE",
            style =
              MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
              ),
            color = MatrixGlowHead,
            textAlign = TextAlign.Center,
          )

          Text(
            text = "OPERATOR VERIFIED: ${username.uppercase()}",
            style =
              MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
              ),
            color = MatrixNeonGreen,
          )

          LinearProgressIndicator(
            progress = { animatedProgress },
            modifier =
              Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = MatrixNeonGreen,
            trackColor = MatrixNeonGreen.copy(alpha = 0.2f),
          )

          Text(
            text = "INITIALIZING SIMULATED SANDBOX PROTOCOLS...",
            style =
              MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
              ),
            color = MatrixGlowHead.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
          )
        }
      }
    }
  }
}

/**
 * 3. PAYMENT STYLE SELECTION
 * Shows two large payment-style cards:
 * - OPTION 1: PAYTM-INSPIRED (Blue & Light-Cyan theme)
 * - OPTION 2: PHONEPE-INSPIRED (Royal Purple theme)
 * Uses original ZENRATION PAY branding and custom icons.
 */
@Composable
fun PaymentStyleSelectionScreen(
  username: String,
  onSelectStyle: (PaymentStyle) -> Unit,
  modifier: Modifier = Modifier,
) {
  val PaytmBluePrimary = Color(0xFF002E6E)
  val PaytmCyanAccent = Color(0xFF00B9F1)
  val PaytmLightCyan = Color(0xFF00E5FF)

  val PhonePePurplePrimary = Color(0xFF3B0A75)
  val PhonePeVioletAccent = Color(0xFF5F259F)
  val PhonePeLightPurple = Color(0xFFB388FF)

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(ZenBlack)
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .testTag("payment_style_selection_screen")
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 18.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().widthIn(max = 540.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          ZenrationLogoEmblem(size = 44.dp)
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "ZENRATION PAY",
              style = MaterialTheme.typography.titleMedium,
              color = ZenTextPrimary,
              fontWeight = FontWeight.ExtraBold,
            )
            Text(
              text = "Operator: $username",
              style = MaterialTheme.typography.labelSmall,
              color = ZenElectricBlue,
            )
          }
        }
        SimulatedBadgePill(
          text = "SIMULATED – NOT PAID",
          accentColor = ZenSimAmber,
        )
      }

      Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 540.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = "CHOOSE PAYMENT SIMULATOR STYLE",
          style = MaterialTheme.typography.headlineSmall,
          color = ZenTextPrimary,
          fontWeight = FontWeight.ExtraBold,
        )
        Text(
          text =
            "Select your preferred ZENRATION PAY sandbox interface. You can switch between both styles anytime from the dashboard.",
          style = MaterialTheme.typography.bodySmall,
          color = ZenTextSecondary,
        )
      }

      // OPTION 1: PAYTM-INSPIRED CARD
      StyleOptionHeroCard(
        optionBadge = "OPTION 1",
        title = "PAYTM-INSPIRED",
        subtitle = "Blue & Light-Cyan Fintech Interface • Original ZENRATION PAY Branding",
        features =
          listOf(
            "Transfer Money: Mobile Number, UPI ID, Scan QR & Bank Transfer",
            "Recent Demo Contacts, Search Bar & Local Transaction History",
            "Blue & Light-Cyan Simulated Result Screen with SIMULATED Watermark",
          ),
        gradientColors = listOf(PaytmBluePrimary, Color(0xFF004B93), PaytmCyanAccent),
        borderColor = PaytmLightCyan,
        accentColor = PaytmLightCyan,
        icon = Icons.Default.AccountBalance,
        testTag = "select_style_paytm_card",
        onClick = { onSelectStyle(PaymentStyle.PAYTM_INSPIRED) },
      )

      // OPTION 2: PHONEPE-INSPIRED CARD
      StyleOptionHeroCard(
        optionBadge = "OPTION 2",
        title = "PHONEPE-INSPIRED",
        subtitle = "Royal Purple Fintech Interface • Original ZENRATION PAY Branding",
        features =
          listOf(
            "Transfer Money: Mobile Number, UPI ID, Scan QR & Bank Account",
            "Recent Demo Contacts, Search Bar & Local Transaction History",
            "Purple-Themed Simulated Result Screen with Sender Details & Watermark",
          ),
        gradientColors = listOf(PhonePePurplePrimary, PhonePeVioletAccent, Color(0xFF7C4DFF)),
        borderColor = PhonePeLightPurple,
        accentColor = PhonePeLightPurple,
        icon = Icons.Default.PhoneAndroid,
        testTag = "select_style_phonepe_card",
        onClick = { onSelectStyle(PaymentStyle.PHONEPE_INSPIRED) },
      )

      Surface(
        modifier = Modifier.fillMaxWidth().widthIn(max = 540.dp),
        shape = RoundedCornerShape(16.dp),
        color = ZenSurfaceDark,
        border = BorderStroke(1.dp, ZenCardBorder),
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
              "100% Original ZENRATION PAY Simulator • Uses dummy contacts and simulated balances only. Never connects to real banks.",
            style = MaterialTheme.typography.bodySmall,
            color = ZenTextSecondary,
          )
        }
      }
    }
  }
}

@Composable
private fun StyleOptionHeroCard(
  optionBadge: String,
  title: String,
  subtitle: String,
  features: List<String>,
  gradientColors: List<Color>,
  borderColor: Color,
  accentColor: Color,
  icon: ImageVector,
  testTag: String,
  onClick: () -> Unit,
) {
  Card(
    modifier =
      Modifier
        .fillMaxWidth()
        .widthIn(max = 540.dp)
        .clip(RoundedCornerShape(26.dp))
        .clickable(onClick = onClick)
        .testTag(testTag),
    shape = RoundedCornerShape(26.dp),
    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    border = BorderStroke(1.8.dp, borderColor.copy(alpha = 0.8f)),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(brush = Brush.linearGradient(colors = gradientColors))
          .padding(22.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(50),
          color = Color.Black.copy(alpha = 0.38f),
          border = BorderStroke(1.dp, accentColor.copy(alpha = 0.7f)),
        ) {
          Text(
            text = "$optionBadge • ZENRATION PAY",
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
          )
        }

        Box(
          modifier =
            Modifier
              .size(46.dp)
              .clip(CircleShape)
              .background(Color.Black.copy(alpha = 0.35f))
              .border(1.dp, accentColor, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
          )
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.headlineSmall,
          color = Color.White,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 1.sp,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = Color.White.copy(alpha = 0.88f),
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        features.forEach { line ->
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier =
                Modifier
                  .size(6.dp)
                  .clip(CircleShape)
                  .background(accentColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = line,
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.92f),
            )
          }
        }
      }

      Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = Color.Black.copy(alpha = 0.45f),
            contentColor = Color.White,
          ),
        border = BorderStroke(1.dp, accentColor),
      ) {
        Text(
          text = "LAUNCH $title SIMULATOR",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.ExtraBold,
          color = Color.White,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}
