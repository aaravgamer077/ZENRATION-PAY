package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.DemoBalance
import com.example.data.model.UserProfile
import com.example.ui.components.SimulatedBadgePill
import com.example.ui.theme.ZenBlack
import com.example.ui.theme.ZenCardBorder
import com.example.ui.theme.ZenElectricBlue
import com.example.ui.theme.ZenErrorRed
import com.example.ui.theme.ZenPurpleGlow
import com.example.ui.theme.ZenRoyalPurple
import com.example.ui.theme.ZenSurfaceDark
import com.example.ui.theme.ZenSurfaceElevated
import com.example.ui.theme.ZenSuccessGreen
import com.example.ui.theme.ZenTextMuted
import com.example.ui.theme.ZenTextPrimary
import com.example.ui.theme.ZenTextSecondary
import com.example.ui.theme.ZenWarningAmber
import com.example.ui.viewmodel.UiState
import com.example.voice.VoicePreferences
import com.google.firebase.FirebaseApp
import java.util.Locale

@Composable
fun ProfileScreen(
  currentUserId: String,
  userProfileState: UiState<UserProfile?>,
  demoBalanceState: UiState<DemoBalance?>,
  voicePreferences: VoicePreferences,
  onOpenVoiceSettingsClick: () -> Unit,
  onSaveAccountSettings: (String, String, Boolean, Boolean) -> Unit,
  onAdjustDemoBalance: (Double) -> Unit,
  onLogoutClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val profile = (userProfileState as? UiState.Success)?.data
  val balance = (demoBalanceState as? UiState.Success)?.data
  val currentBalanceVal = balance?.demoBalance ?: balance?.balance ?: 25000.0

  var displayNameInput by remember { mutableStateOf("") }
  var upiIdInput by remember { mutableStateOf("") }
  var notificationsEnabled by remember { mutableStateOf(true) }
  var biometricSimEnabled by remember { mutableStateOf(false) }

  // Account mismatch verifier (compares by Firebase UID, never email alone)
  var websiteUidToVerify by remember { mutableStateOf("") }
  var showMismatchWarning by remember { mutableStateOf(false) }
  var showWebSdkSnippet by remember { mutableStateOf(false) }
  var copiedFeedback by remember { mutableStateOf<String?>(null) }

  val databaseId = remember(context) { context.getString(R.string.firestore_database_id) }
  val firebaseOptions = remember {
    runCatching { FirebaseApp.getInstance().options }.getOrNull()
  }
  val projectId = firebaseOptions?.projectId ?: "gen-lang-client-0366064861"
  val apiKey = firebaseOptions?.apiKey ?: "CONFIGURED_IN_GOOGLE_SERVICES"
  val storageBucket = firebaseOptions?.storageBucket ?: "$projectId.firebasestorage.app"

  val webSdkConfigSnippet =
    remember(projectId, databaseId, apiKey, storageBucket, currentUserId) {
      """
      // ZENRATION PAY ADMIN – Shared Firebase Web SDK Configuration
      import { initializeApp } from "firebase/app";
      import { getAuth, GoogleAuthProvider } from "firebase/auth";
      import { initializeFirestore } from "firebase/firestore";

      export const firebaseConfig = {
        apiKey: "$apiKey",
        authDomain: "$projectId.firebaseapp.com",
        projectId: "$projectId",
        storageBucket: "$storageBucket"
      };

      export const app = initializeApp(firebaseConfig);
      export const auth = getAuth(app);
      export const db = initializeFirestore(app, {}, "$databaseId");
      // Active App User UID: $currentUserId
      """.trimIndent()
    }

  LaunchedEffect(profile) {
    if (profile != null) {
      displayNameInput = profile.name.ifBlank { profile.displayName }
      upiIdInput = profile.upiId ?: "user@zenpay.sim"
      notificationsEnabled = profile.notificationsEnabled
      biometricSimEnabled = profile.biometricSimEnabled
    }
  }

  LazyColumn(
    modifier = modifier.fillMaxSize().background(ZenBlack).testTag("profile_screen_list"),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    // 1. Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "Profile & Account Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = ZenTextPrimary,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "Shared Firebase UID Identity & Web Admin Sync",
            style = MaterialTheme.typography.bodySmall,
            color = ZenTextSecondary,
          )
        }
        SimulatedBadgePill(text = "SHARED CLOUD", accentColor = ZenElectricBlue)
      }
    }

    // 2. User Identity Card
    item {
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
          UserAvatarBadge(
            photoUrl = profile?.photoURL ?: profile?.photoUrl,
            displayName = profile?.name?.ifBlank { profile.displayName } ?: "Zenration User",
            size = 72,
          )

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = profile?.name?.ifBlank { profile.displayName } ?: "Zenration User",
            style = MaterialTheme.typography.titleLarge,
            color = ZenTextPrimary,
            fontWeight = FontWeight.Bold,
          )

          Text(
            text = profile?.email ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = ZenPurpleGlow,
          )

          Spacer(modifier = Modifier.height(12.dp))

          HorizontalDivider(color = ZenCardBorder.copy(alpha = 0.6f))

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Unique Firebase UID (Shared with Website)",
                style = MaterialTheme.typography.labelSmall,
                color = ZenTextMuted,
              )
              Text(
                text = currentUserId,
                style = MaterialTheme.typography.bodySmall,
                color = ZenElectricBlue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            IconButton(
              onClick = {
                val clipboard =
                  context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("Firebase UID", currentUserId))
                copiedFeedback = "Firebase UID copied to clipboard"
              }
            ) {
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy Firebase UID",
                tint = ZenElectricBlue,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }
    }

    // 3. Shared Website & Account Mismatch Verification Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth().testTag("shared_website_sync_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
        border = BorderStroke(1.dp, ZenElectricBlue.copy(alpha = 0.5f)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.CloudSync,
              contentDescription = null,
              tint = ZenElectricBlue,
              modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "ZENRATION PAY ADMIN Website Sync",
                style = MaterialTheme.typography.titleMedium,
                color = ZenTextPrimary,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Project: $projectId • Real-time UID Link",
                style = MaterialTheme.typography.bodySmall,
                color = ZenPurpleGlow,
              )
            }
          }

          Text(
            text =
              "Log in to the ZENRATION PAY website using the exact same Google account (${profile?.email ?: "your Gmail"}) so your Firebase UID matches and your demoBalance & transactions sync live.",
            style = MaterialTheme.typography.bodySmall,
            color = ZenTextSecondary,
          )

          OutlinedTextField(
            value = websiteUidToVerify,
            onValueChange = {
              websiteUidToVerify = it
              val trimmed = it.trim()
              showMismatchWarning = trimmed.isNotEmpty() && trimmed != currentUserId
            },
            label = { Text("Verify Website Firebase UID (Optional Check)") },
            placeholder = { Text("Paste UID from Website to verify match") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("verify_website_uid_input"),
            shape = RoundedCornerShape(14.dp),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ZenElectricBlue,
                unfocusedBorderColor = ZenCardBorder,
                focusedTextColor = ZenTextPrimary,
                unfocusedTextColor = ZenTextPrimary,
              ),
          )

          if (websiteUidToVerify.trim() == currentUserId && currentUserId.isNotBlank()) {
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(14.dp),
              color = ZenSuccessGreen.copy(alpha = 0.14f),
              border = BorderStroke(1.dp, ZenSuccessGreen.copy(alpha = 0.5f)),
            ) {
              Text(
                text = "✓ UID MATCH VERIFIED: Connected to the same Google account across App & Website.",
                style = MaterialTheme.typography.bodySmall,
                color = ZenSuccessGreen,
                modifier = Modifier.padding(12.dp),
              )
            }
          }

          AnimatedVisibility(visible = showMismatchWarning) {
            Surface(
              modifier = Modifier.fillMaxWidth().testTag("account_mismatch_warning_banner"),
              shape = RoundedCornerShape(16.dp),
              color = ZenWarningAmber.copy(alpha = 0.14f),
              border = BorderStroke(1.dp, ZenWarningAmber.copy(alpha = 0.65f)),
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = "Account Mismatch Warning",
                    tint = ZenWarningAmber,
                    modifier = Modifier.size(20.dp),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "⚠️ ACCOUNT MISMATCH",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = ZenWarningAmber,
                  )
                }
                Text(
                  text =
                    "You are using a different Google account.\n\n" +
                      "Please log out and select the same Gmail account used in your ZENRATION PAY application.",
                  style = MaterialTheme.typography.bodySmall,
                  color = ZenTextPrimary,
                )
                OutlinedButton(
                  onClick = onLogoutClick,
                  border = BorderStroke(1.dp, ZenWarningAmber),
                  shape = RoundedCornerShape(12.dp),
                ) {
                  Text(
                    text = "Log Out & Switch Gmail Account",
                    color = ZenWarningAmber,
                    style = MaterialTheme.typography.labelMedium,
                  )
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedButton(
              onClick = { showWebSdkSnippet = !showWebSdkSnippet },
              modifier = Modifier.weight(1f).testTag("toggle_web_sdk_config_button"),
              border = BorderStroke(1.dp, ZenElectricBlue),
              shape = RoundedCornerShape(12.dp),
            ) {
              Text(
                text = if (showWebSdkSnippet) "Hide Web Config" else "View Web SDK Config",
                color = ZenElectricBlue,
                style = MaterialTheme.typography.labelMedium,
              )
            }

            OutlinedButton(
              onClick = {
                val clipboard =
                  context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(
                  ClipData.newPlainText("ZENRATION PAY Web Config", webSdkConfigSnippet)
                )
                copiedFeedback = "Shared Firebase Web SDK Config copied!"
              },
              modifier = Modifier.weight(1f).testTag("copy_web_sdk_config_button"),
              border = BorderStroke(1.dp, ZenPurpleGlow),
              shape = RoundedCornerShape(12.dp),
            ) {
              Text(
                text = "Copy Web Config",
                color = ZenPurpleGlow,
                style = MaterialTheme.typography.labelMedium,
              )
            }
          }

          if (copiedFeedback != null) {
            Text(
              text = copiedFeedback.orEmpty(),
              style = MaterialTheme.typography.labelSmall,
              color = ZenElectricBlue,
            )
          }

          AnimatedVisibility(visible = showWebSdkSnippet) {
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(14.dp),
              color = ZenSurfaceElevated,
              border = BorderStroke(1.dp, ZenCardBorder),
            ) {
              Text(
                text = webSdkConfigSnippet,
                style =
                  MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                  ),
                color = ZenTextPrimary,
                modifier = Modifier.padding(12.dp),
              )
            }
          }
        }
      }
    }

    // 4. Dynamic Voice Soundbox Settings Shortcut Card
    item {
      Card(
        modifier =
          Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onOpenVoiceSettingsClick)
            .testTag("profile_voice_settings_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
        border = BorderStroke(1.dp, ZenElectricBlue.copy(alpha = 0.45f)),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = null,
            tint = ZenElectricBlue,
            modifier = Modifier.size(28.dp),
          )
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Dynamic Voice Announcement Settings",
              style = MaterialTheme.typography.titleMedium,
              color = ZenTextPrimary,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text =
                "${if (voicePreferences.isEnabled) "Enabled" else "Disabled"} • ${voicePreferences.language.displayName} • ${(voicePreferences.volume * 100).toInt()}% Vol",
              style = MaterialTheme.typography.bodySmall,
              color = ZenPurpleGlow,
            )
          }
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Open Voice Settings",
            tint = ZenElectricBlue,
          )
        }
      }
    }

    // 5. Sandbox Demo Balance Controls Card
    item {
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
              imageVector = Icons.Default.AccountBalanceWallet,
              contentDescription = null,
              tint = ZenElectricBlue,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Real-Time demoBalance Synchronization",
                style = MaterialTheme.typography.titleMedium,
                color = ZenTextPrimary,
              )
              Text(
                text =
                  "demoBalance: ₹${String.format(Locale.US, "%,.2f", currentBalanceVal)} (Simulated • Not Bank Funds)",
                style = MaterialTheme.typography.bodySmall,
                color = ZenElectricBlue,
              )
            }
          }

          Text(
            text =
              "Listening in real time to Firestore document 'balances/$currentUserId'. When an authorised administrator updates demoBalance on the ZENRATION PAY ADMIN website, this balance updates automatically without restarting the app.",
            style = MaterialTheme.typography.bodySmall,
            color = ZenTextSecondary,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedButton(
              onClick = { onAdjustDemoBalance(25000.0) },
              modifier = Modifier.weight(1f).testTag("profile_reset_balance_button"),
              border = BorderStroke(1.dp, ZenElectricBlue),
              shape = RoundedCornerShape(12.dp),
            ) {
              Text(text = "Reset ₹25,000", color = ZenElectricBlue)
            }

            OutlinedButton(
              onClick = { onAdjustDemoBalance(currentBalanceVal + 5000.0) },
              modifier = Modifier.weight(1f).testTag("profile_topup_balance_button"),
              border = BorderStroke(1.dp, ZenPurpleGlow),
              shape = RoundedCornerShape(12.dp),
            ) {
              Text(text = "+₹5,000 Credits", color = ZenPurpleGlow)
            }
          }
        }
      }
    }

    // 6. Editable Account Settings Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
        border = BorderStroke(1.dp, ZenCardBorder),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = null,
              tint = ZenPurpleGlow,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = stringResource(R.string.account_settings),
              style = MaterialTheme.typography.titleMedium,
              color = ZenTextPrimary,
            )
          }

          OutlinedTextField(
            value = displayNameInput,
            onValueChange = { displayNameInput = it },
            label = { Text("Name (users/{uid}.name)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("settings_display_name_input"),
            shape = RoundedCornerShape(14.dp),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ZenElectricBlue,
                unfocusedBorderColor = ZenCardBorder,
                focusedTextColor = ZenTextPrimary,
                unfocusedTextColor = ZenTextPrimary,
              ),
          )

          OutlinedTextField(
            value = upiIdInput,
            onValueChange = { upiIdInput = it },
            label = { Text("Simulated UPI ID Handle") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("settings_upi_id_input"),
            shape = RoundedCornerShape(14.dp),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ZenElectricBlue,
                unfocusedBorderColor = ZenCardBorder,
                focusedTextColor = ZenTextPrimary,
                unfocusedTextColor = ZenTextPrimary,
              ),
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Simulated Payment Alerts",
                style = MaterialTheme.typography.titleSmall,
                color = ZenTextPrimary,
              )
              Text(
                text = "Show instant confirmation banners on sandbox transactions",
                style = MaterialTheme.typography.bodySmall,
                color = ZenTextSecondary,
              )
            }
            Switch(
              checked = notificationsEnabled,
              onCheckedChange = { notificationsEnabled = it },
              colors =
                SwitchDefaults.colors(
                  checkedThumbColor = ZenBlack,
                  checkedTrackColor = ZenElectricBlue,
                ),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Enhanced Sandbox Shield",
                style = MaterialTheme.typography.titleSmall,
                color = ZenTextPrimary,
              )
              Text(
                text = "Enforce strict zero-transfer verification on every QR scan",
                style = MaterialTheme.typography.bodySmall,
                color = ZenTextSecondary,
              )
            }
            Switch(
              checked = biometricSimEnabled,
              onCheckedChange = { biometricSimEnabled = it },
              colors =
                SwitchDefaults.colors(
                  checkedThumbColor = ZenBlack,
                  checkedTrackColor = ZenRoyalPurple,
                ),
            )
          }

          Button(
            onClick = {
              onSaveAccountSettings(
                displayNameInput,
                upiIdInput,
                notificationsEnabled,
                biometricSimEnabled,
              )
            },
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_account_settings_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ZenRoyalPurple),
          ) {
            Icon(
              imageVector = Icons.Default.Save,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Save Account Settings",
              style = MaterialTheme.typography.labelLarge,
              color = Color.White,
            )
          }
        }
      }
    }

    // 7. Reset Sandbox Session Button
    item {
      Button(
        onClick = onLogoutClick,
        modifier =
          Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("logout_button"),
        shape = RoundedCornerShape(16.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = ZenElectricBlue.copy(alpha = 0.14f),
            contentColor = ZenElectricBlue,
          ),
        border = BorderStroke(1.dp, ZenElectricBlue.copy(alpha = 0.6f)),
      ) {
        Icon(
          imageVector = Icons.Default.AccountBalanceWallet,
          contentDescription = "Reset Sandbox Demo Balance",
          tint = ZenElectricBlue,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Reset Sandbox Demo Balance (₹25,000)",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = ZenElectricBlue,
        )
      }
    }
  }
}
