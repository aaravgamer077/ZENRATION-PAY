package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.ui.components.SimulatedBadgePill
import com.example.ui.components.ZenrationLogoEmblem
import com.example.ui.theme.ZenBlack
import com.example.ui.theme.ZenCardBorder
import com.example.ui.theme.ZenDeepPurple
import com.example.ui.theme.ZenElectricBlue
import com.example.ui.theme.ZenErrorRed
import com.example.ui.theme.ZenPurpleGlow
import com.example.ui.theme.ZenRoyalPurple
import com.example.ui.theme.ZenSurfaceDark
import com.example.ui.theme.ZenSurfaceElevated
import com.example.ui.theme.ZenTextMuted
import com.example.ui.theme.ZenTextPrimary
import com.example.ui.theme.ZenTextSecondary
import com.example.ui.theme.ZenWarningAmber

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var showLoginNoticeCard by remember { mutableStateOf(true) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = coroutineScope,
    )
  }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(ZenBlack)
        .drawBehind {
          // Top-right Deep Purple ambient radial glow
          drawCircle(
            brush =
              Brush.radialGradient(
                colors = listOf(ZenDeepPurple.copy(alpha = 0.42f), Color.Transparent),
                center = Offset(size.width * 0.85f, size.height * 0.12f),
                radius = size.width * 0.75f,
              ),
            radius = size.width * 0.75f,
            center = Offset(size.width * 0.85f, size.height * 0.12f),
          )
          // Bottom-left Electric Blue ambient radial glow
          drawCircle(
            brush =
              Brush.radialGradient(
                colors = listOf(ZenElectricBlue.copy(alpha = 0.24f), Color.Transparent),
                center = Offset(size.width * 0.15f, size.height * 0.82f),
                radius = size.width * 0.7f,
              ),
            radius = size.width * 0.7f,
            center = Offset(size.width * 0.15f, size.height * 0.82f),
          )
        }
        .windowInsetsPadding(WindowInsets.safeDrawing),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .widthIn(max = 480.dp)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      SimulatedBadgePill(text = stringResource(R.string.simulated_environment_badge))

      Spacer(modifier = Modifier.height(18.dp))

      ZenrationLogoEmblem(size = 82.dp)

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.headlineLarge,
        color = ZenTextPrimary,
        textAlign = TextAlign.Center,
        letterSpacing = 1.8.sp,
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = stringResource(R.string.app_tagline),
        style = MaterialTheme.typography.titleMedium,
        color = ZenElectricBlue,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = stringResource(R.string.sign_in_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = ZenTextSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 12.dp),
      )

      Spacer(modifier = Modifier.height(20.dp))

      // IMPORTANT LOGIN NOTICE (Shared Android App + Website Account Sync Notice)
      AnimatedVisibility(visible = showLoginNoticeCard) {
        Card(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(bottom = 18.dp)
              .testTag("important_login_notice_card"),
          shape = RoundedCornerShape(20.dp),
          colors =
            CardDefaults.cardColors(
              containerColor = ZenSurfaceDark.copy(alpha = 0.96f)
            ),
          border = BorderStroke(1.dp, ZenWarningAmber.copy(alpha = 0.65f)),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = "Important Login Notice",
                tint = ZenWarningAmber,
                modifier = Modifier.size(22.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "⚠️ IMPORTANT LOGIN NOTICE",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = ZenWarningAmber,
              )
            }

            Text(
              text =
                "Please log in using the SAME Gmail account that you use in your ZENRATION PAY mobile application and website.\n\n" +
                  "Using another Gmail account will open a different account, and your existing app data will not appear here.\n\n" +
                  "Please select your app's Gmail account.",
              style = MaterialTheme.typography.bodySmall,
              color = ZenTextPrimary,
              lineHeight = 18.sp,
            )

            Text(
              text = "Accounts are matched strictly by unique Firebase UID across Android & Web.",
              style = MaterialTheme.typography.labelSmall,
              color = ZenElectricBlue,
            )
          }
        }
      }

      // Feature Highlights Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors =
          CardDefaults.cardColors(
            containerColor = ZenSurfaceDark.copy(alpha = 0.92f)
          ),
        border = BorderStroke(1.dp, ZenCardBorder),
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          FeatureHighlightRow(
            icon = Icons.Default.QrCodeScanner,
            title = "Smart UPI QR Scanner",
            subtitle = "Scan standard UPI QR codes and parse recipient details instantly.",
            accentColor = ZenElectricBlue,
          )
          FeatureHighlightRow(
            icon = Icons.Default.CloudSync,
            title = "Shared Android & Web Cloud Sync",
            subtitle = "Real-time demoBalance & transaction sync with ZENRATION PAY ADMIN Website.",
            accentColor = ZenPurpleGlow,
          )
          FeatureHighlightRow(
            icon = Icons.Default.Shield,
            title = "100% Safe Payment Simulator",
            subtitle = "Zero real money transfers. Every transaction is labelled SIMULATED – NOT PAID.",
            accentColor = ZenElectricBlue,
          )
        }
      }

      Spacer(modifier = Modifier.height(22.dp))

      AnimatedVisibility(visible = errorMessage != null) {
        Surface(
          modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
          shape = RoundedCornerShape(16.dp),
          color = ZenErrorRed.copy(alpha = 0.14f),
          border = BorderStroke(1.dp, ZenErrorRed.copy(alpha = 0.5f)),
        ) {
          Text(
            text = errorMessage.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFFF8A80),
            modifier = Modifier.padding(14.dp),
            textAlign = TextAlign.Center,
          )
        }
      }

      // Primary Action: Continue with Google Button
      Button(
        onClick = {
          errorMessage = null
          isLoading = true
          onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
              isLoading = false
              onAuthSuccess()
            },
            onAuthError = { err ->
              isLoading = false
              errorMessage = err
            },
            scope = coroutineScope,
            onAuthCancelled = { isLoading = false },
          )
        },
        enabled = !isLoading,
        modifier =
          Modifier
            .fillMaxWidth()
            .height(58.dp)
            .testTag("continue_with_google_button"),
        shape = RoundedCornerShape(18.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = ZenSurfaceElevated,
          ),
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
          if (isLoading) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = ZenTextPrimary,
                strokeWidth = 2.5.dp,
              )
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = "Authenticating with Google...",
                style = MaterialTheme.typography.titleMedium,
                color = ZenTextPrimary,
              )
            }
          } else {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
            ) {
              Box(
                modifier =
                  Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = "G",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.ExtraBold,
                  color = ZenDeepPurple,
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = stringResource(R.string.continue_with_google),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ZenTextPrimary,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Secondary Action: Cancel button as specified in the login notice flow
      OutlinedButton(
        onClick = {
          isLoading = false
          errorMessage = null
          showLoginNoticeCard = !showLoginNoticeCard
        },
        modifier =
          Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("cancel_login_notice_button"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ZenCardBorder),
      ) {
        Text(
          text = if (showLoginNoticeCard) "Cancel" else "Show Important Login Notice",
          style = MaterialTheme.typography.labelLarge,
          color = ZenTextSecondary,
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        Icon(
          imageVector = Icons.Default.VerifiedUser,
          contentDescription = null,
          tint = ZenTextMuted,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Secured by Firebase Authentication • Shared UID Sync",
          style = MaterialTheme.typography.bodySmall,
          color = ZenTextMuted,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}

@Composable
private fun FeatureHighlightRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  accentColor: Color,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top,
  ) {
    Box(
      modifier =
        Modifier
          .size(42.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(accentColor.copy(alpha = 0.14f))
          .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = accentColor,
        modifier = Modifier.size(22.dp),
      )
    }
    Spacer(modifier = Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = ZenTextPrimary,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = ZenTextSecondary,
      )
    }
  }
}
