package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.DemoBalance
import com.example.data.model.SimulatedTransaction
import com.example.data.model.UpiQrPayload
import com.example.data.model.UserProfile
import com.example.ui.components.DemoBalanceHeroCard
import com.example.ui.components.SimulatedBadgePill
import com.example.ui.components.TransactionItemCard
import com.example.ui.components.ZenrationLogoEmblem
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
import com.example.ui.viewmodel.UiState
import com.example.voice.VoicePreferences

@Composable
fun HomeScreen(
  userProfileState: UiState<UserProfile?>,
  demoBalanceState: UiState<DemoBalance?>,
  transactionsState: UiState<List<SimulatedTransaction>>,
  voicePreferences: VoicePreferences,
  onScanQrClick: () -> Unit,
  onOpenCustomBalanceClick: () -> Unit,
  onOpenVoiceSettingsClick: () -> Unit,
  onTestVoiceQuickClick: (Double) -> Unit,
  onViewAllHistoryClick: () -> Unit,
  onOpenAccountSettingsClick: () -> Unit,
  onTransactionClick: (SimulatedTransaction) -> Unit,
  onResetBalanceClick: () -> Unit,
  onTopUpBalanceClick: (Double) -> Unit,
  onQuickSimulateMerchant: (UpiQrPayload) -> Unit,
  onShowToastMessage: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val profile = (userProfileState as? UiState.Success)?.data
  val balance = (demoBalanceState as? UiState.Success)?.data
  val transactions = (transactionsState as? UiState.Success)?.data.orEmpty()
  var isTopLeftMenuExpanded by remember { mutableStateOf(false) }

  val displayName =
    profile?.name?.takeIf { it.isNotBlank() }
      ?: profile?.displayName?.takeIf { it.isNotBlank() }
      ?: "Digital Pioneer"
  val userEmail = profile?.email.orEmpty()
  val userUpi = profile?.upiId ?: "pioneer@zenpay.sim"
  val currentBalanceAmount = balance?.demoBalance ?: balance?.balance ?: 0.0

  LazyColumn(
    modifier = modifier.fillMaxSize().background(ZenBlack).testTag("home_screen_list"),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    // 1. Top Header: Three-Dot Menu (⋮) at Top-Left + ZENRATION PAY Logo + User Profile Avatar
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box {
            IconButton(
              onClick = { isTopLeftMenuExpanded = true },
              modifier =
                Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .background(ZenSurfaceElevated)
                  .border(1.dp, ZenCardBorder, CircleShape)
                  .testTag("home_three_dot_menu_button"),
            ) {
              Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More Options Menu",
                tint = ZenElectricBlue,
              )
            }

            DropdownMenu(
              expanded = isTopLeftMenuExpanded,
              onDismissRequest = { isTopLeftMenuExpanded = false },
              modifier =
                Modifier
                  .background(ZenSurfaceElevated)
                  .border(1.dp, ZenElectricBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            ) {
              DropdownMenuItem(
                text = {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.AccountBalanceWallet,
                      contentDescription = null,
                      tint = ZenElectricBlue,
                      modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                      text = "Custom Your Balance",
                      style = MaterialTheme.typography.titleSmall,
                      color = ZenTextPrimary,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                },
                onClick = {
                  isTopLeftMenuExpanded = false
                  onOpenCustomBalanceClick()
                },
                modifier = Modifier.testTag("menu_option_custom_balance"),
              )
            }
          }

          Spacer(modifier = Modifier.width(10.dp))
          ZenrationLogoEmblem(size = 44.dp)
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = stringResource(R.string.app_name),
              style = MaterialTheme.typography.titleMedium,
              color = ZenTextPrimary,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = stringResource(R.string.app_tagline),
              style = MaterialTheme.typography.labelSmall,
              color = ZenElectricBlue,
            )
          }
        }

        // User Profile Chip -> Opens Profile / Account Settings
        Surface(
          modifier =
            Modifier
              .clip(RoundedCornerShape(50))
              .clickable(onClick = onOpenAccountSettingsClick)
              .testTag("home_user_profile_chip"),
          shape = RoundedCornerShape(50),
          color = ZenSurfaceElevated,
          border = BorderStroke(1.dp, ZenCardBorder),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            UserAvatarBadge(
              photoUrl = profile?.photoURL ?: profile?.photoUrl,
              displayName = displayName,
              size = 32,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.padding(end = 4.dp)) {
              Text(
                text = displayName.substringBefore(" "),
                style = MaterialTheme.typography.labelMedium,
                color = ZenTextPrimary,
                maxLines = 1,
              )
              Text(
                text = "Profile",
                style = MaterialTheme.typography.labelSmall,
                color = ZenPurpleGlow,
              )
            }
          }
        }
      }
    }

    // 2. Welcome Message Banner with Copyable UPI Handle
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
        border = BorderStroke(1.dp, ZenCardBorder),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Welcome back, $displayName",
              style = MaterialTheme.typography.titleMedium,
              color = ZenTextPrimary,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier =
                Modifier.clickable {
                  val clipboard =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                  clipboard?.setPrimaryClip(ClipData.newPlainText("Sandbox UPI ID", userUpi))
                  onShowToastMessage("Copied Sandbox UPI ID: $userUpi")
                },
            ) {
              Text(
                text = "UPI ID: $userUpi",
                style = MaterialTheme.typography.bodySmall,
                color = ZenElectricBlue,
              )
              Spacer(modifier = Modifier.width(6.dp))
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy UPI ID",
                tint = ZenElectricBlue,
                modifier = Modifier.size(13.dp),
              )
            }
            if (userEmail.isNotBlank()) {
              Text(
                text = userEmail,
                style = MaterialTheme.typography.bodySmall,
                color = ZenTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
          SimulatedBadgePill(text = "SANDBOX", accentColor = ZenPurpleGlow)
        }
      }
    }

    // 3. Demo Balance Hero Card (Synced with Firestore /balances/{uid})
    item {
      DemoBalanceHeroCard(
        demoBalance = balance,
        isLoading = demoBalanceState is UiState.Loading,
        onResetBalanceClick = onResetBalanceClick,
      )
    }

    // 4. Primary Scan QR CTA Button + Quick Navigation Actions
    item {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Button(
          onClick = onScanQrClick,
          modifier =
            Modifier
              .fillMaxWidth()
              .height(62.dp)
              .testTag("home_scan_qr_button"),
          shape = RoundedCornerShape(20.dp),
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
                  shape = RoundedCornerShape(20.dp),
                ),
            contentAlignment = Alignment.Center,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
            ) {
              Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "Scan UPI QR code",
                tint = ZenTextPrimary,
                modifier = Modifier.size(26.dp),
              )
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = "SCAN ANY UPI QR (SIMULATOR)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ZenTextPrimary,
              )
            }
          }
        }

        // Quick Action Row: Scan QR, Voice Soundbox, Transaction History, Account Settings
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          QuickActionTile(
            icon = Icons.Default.QrCodeScanner,
            label = "Scan QR",
            accent = ZenElectricBlue,
            onClick = onScanQrClick,
            testTag = "quick_action_scan",
            modifier = Modifier.weight(1f),
          )
          QuickActionTile(
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            label = "Voice Box",
            accent = ZenPurpleGlow,
            onClick = onOpenVoiceSettingsClick,
            testTag = "quick_action_voice",
            modifier = Modifier.weight(1f),
          )
          QuickActionTile(
            icon = Icons.Default.History,
            label = "History",
            accent = ZenElectricBlue,
            onClick = onViewAllHistoryClick,
            testTag = "quick_action_history",
            modifier = Modifier.weight(1f),
          )
          QuickActionTile(
            icon = Icons.Default.Settings,
            label = "Settings",
            accent = ZenPurpleGlow,
            onClick = onOpenAccountSettingsClick,
            testTag = "quick_action_settings",
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    // 5. Dynamic Voice Soundbox Quick Control Strip on Home Screen
    item {
      Card(
        modifier = Modifier.fillMaxWidth().testTag("home_voice_soundbox_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
        border = BorderStroke(1.dp, ZenElectricBlue.copy(alpha = 0.4f)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier =
                  Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ZenDeepPurple.copy(alpha = 0.35f))
                    .border(1.dp, ZenElectricBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Default.RecordVoiceOver,
                  contentDescription = null,
                  tint = ZenElectricBlue,
                  modifier = Modifier.size(22.dp),
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Dynamic Voice Soundbox",
                  style = MaterialTheme.typography.titleMedium,
                  color = ZenTextPrimary,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text =
                    "${voicePreferences.language.displayName} • Volume ${(voicePreferences.volume * 100).toInt()}%",
                  style = MaterialTheme.typography.bodySmall,
                  color = ZenPurpleGlow,
                )
              }
            }

            SimulatedBadgePill(
              text = if (voicePreferences.isEnabled) "ACTIVE" else "MUTED",
              accentColor = if (voicePreferences.isEnabled) ZenElectricBlue else ZenSimAmber,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedButton(
              onClick = { onTestVoiceQuickClick(150.0) },
              modifier = Modifier.weight(1f).testTag("home_test_voice_button"),
              border = BorderStroke(1.dp, ZenElectricBlue),
              shape = RoundedCornerShape(12.dp),
            ) {
              Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = ZenElectricBlue,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(text = "Test Voice (₹150)", color = ZenElectricBlue)
            }

            Button(
              onClick = onOpenVoiceSettingsClick,
              modifier = Modifier.weight(1f).testTag("home_voice_settings_button"),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = ZenRoyalPurple),
            ) {
              Text(text = "Voice Settings", color = Color.White)
            }
          }
        }
      }
    }

    // 6. Instant Sandbox UPI Merchants (1-tap test simulation)
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Quick Sandbox UPI Merchants",
          style = MaterialTheme.typography.titleSmall,
          color = ZenTextSecondary,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          val presets =
            listOf(
              UpiQrPayload(
                recipientUpiId = "novacafe@okaxis",
                recipientName = "Nova Cyber Cafe",
                amount = "25",
                note = "Espresso Shot",
              ),
              UpiQrPayload(
                recipientUpiId = "zenithmart@ybl",
                recipientName = "Zenith Tech Store",
                amount = "750",
                note = "Fast Charger",
              ),
              UpiQrPayload(
                recipientUpiId = "metrofresh@icici",
                recipientName = "Metro HyperMart",
                amount = "2000",
                note = "Monthly Supplies",
              ),
            )
          presets.forEach { preset ->
            Card(
              modifier =
                Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(16.dp))
                  .clickable { onQuickSimulateMerchant(preset) },
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
              border = BorderStroke(1.dp, ZenCardBorder),
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.Start,
              ) {
                Text(
                  text = preset.recipientName,
                  style = MaterialTheme.typography.labelMedium,
                  color = ZenTextPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = preset.recipientUpiId,
                  style = MaterialTheme.typography.bodySmall,
                  color = ZenPurpleGlow,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Sim ₹${preset.amount}",
                  style = MaterialTheme.typography.labelSmall,
                  color = ZenElectricBlue,
                )
              }
            }
          }
        }
      }
    }

    // 7. Transaction History Preview Section
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(R.string.transaction_history),
          style = MaterialTheme.typography.titleLarge,
          color = ZenTextPrimary,
        )
        TextButton(
          onClick = onViewAllHistoryClick,
          modifier = Modifier.testTag("home_view_all_history_button"),
        ) {
          Text(
            text = "See All (${transactions.size})",
            style = MaterialTheme.typography.labelLarge,
            color = ZenElectricBlue,
          )
        }
      }
    }

    if (transactions.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
          border = BorderStroke(1.dp, ZenCardBorder),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
              contentDescription = null,
              tint = ZenPurpleGlow,
              modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "No Simulated Transactions Yet",
              style = MaterialTheme.typography.titleMedium,
              color = ZenTextPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text =
                "Tap 'Scan UPI QR' or choose a Quick Sandbox Merchant above to test your first simulated payment with voice announcement.",
              style = MaterialTheme.typography.bodySmall,
              color = ZenTextSecondary,
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    } else {
      items(
        items = transactions.take(4),
        key = { it.id.ifBlank { it.referenceId } },
      ) { tx ->
        TransactionItemCard(
          transaction = tx,
          onClick = { onTransactionClick(tx) },
        )
      }
    }

    // 8. Digital Sandbox Hub & Utility Buttons Section (Bottom Interactive Sections)
    item {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = "Digital Sandbox Controls & Tools",
          style = MaterialTheme.typography.titleMedium,
          color = ZenTextPrimary,
          fontWeight = FontWeight.Bold,
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          SandboxToolButtonCard(
            icon = Icons.Default.AddCard,
            title = "+₹5,000 Credits",
            subtitle = "Top up sandbox balance",
            accent = ZenElectricBlue,
            onClick = { onTopUpBalanceClick(currentBalanceAmount + 5000.0) },
            testTag = "sandbox_tool_topup",
            modifier = Modifier.weight(1f),
          )
          SandboxToolButtonCard(
            icon = Icons.Default.QrCode2,
            title = "Custom Amount",
            subtitle = "Enter any ₹ to simulate",
            accent = ZenPurpleGlow,
            onClick = {
              onQuickSimulateMerchant(
                UpiQrPayload(
                  recipientUpiId = "custommerchant@zenpay.sim",
                  recipientName = "Zenration Custom Sandbox",
                  amount = "",
                  note = "Custom Amount Simulation",
                )
              )
            },
            testTag = "sandbox_tool_custom_pay",
            modifier = Modifier.weight(1f),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          SandboxToolButtonCard(
            icon = Icons.Default.AccountBalanceWallet,
            title = "Reset ₹25,000",
            subtitle = "Restore default balance",
            accent = ZenPurpleGlow,
            onClick = onResetBalanceClick,
            testTag = "sandbox_tool_reset_balance",
            modifier = Modifier.weight(1f),
          )
          SandboxToolButtonCard(
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            title = "Test ₹2,000 Voice",
            subtitle = "Hear Hindi/English alert",
            accent = ZenElectricBlue,
            onClick = { onTestVoiceQuickClick(2000.0) },
            testTag = "sandbox_tool_test_2000_voice",
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    // 9. Account Settings Overview Card on Home Screen
    item {
      Card(
        modifier =
          Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onOpenAccountSettingsClick)
            .testTag("home_account_settings_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
        border = BorderStroke(1.dp, ZenCardBorder),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ZenDeepPurple.copy(alpha = 0.28f))
                .border(1.dp, ZenRoyalPurple.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = null,
              tint = ZenElectricBlue,
            )
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(R.string.account_settings),
              style = MaterialTheme.typography.titleMedium,
              color = ZenTextPrimary,
            )
            Text(
              text = "Manage Google profile, sandbox UPI ID, voice alerts & logout",
              style = MaterialTheme.typography.bodySmall,
              color = ZenTextSecondary,
            )
          }
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Open Account Settings",
            tint = ZenElectricBlue,
          )
        }
      }
    }

    // 10. Sandbox Safety Footer
    item {
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Default.Shield,
          contentDescription = null,
          tint = ZenTextMuted,
          modifier = Modifier.size(15.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "ZENRATION PAY Sandbox • All balances and payments are 100% simulated",
          style = MaterialTheme.typography.bodySmall,
          color = ZenTextMuted,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}

@Composable
private fun SandboxToolButtonCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  accent: Color,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier =
      modifier
        .clip(RoundedCornerShape(18.dp))
        .clickable(onClick = onClick)
        .testTag(testTag),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
    border = BorderStroke(1.dp, ZenCardBorder),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = accent,
          modifier = Modifier.size(20.dp),
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge,
          color = ZenTextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = ZenTextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
fun UserAvatarBadge(
  photoUrl: String?,
  displayName: String,
  size: Int = 40,
) {
  val initials =
    displayName
      .trim()
      .split(" ")
      .filter { it.isNotBlank() }
      .take(2)
      .joinToString("") { it.first().uppercase() }
      .ifBlank { "ZP" }

  Box(
    modifier =
      Modifier
        .size(size.dp)
        .clip(CircleShape)
        .background(
          brush = Brush.linearGradient(colors = listOf(ZenDeepPurple, ZenElectricBlue))
        ),
    contentAlignment = Alignment.Center,
  ) {
    if (!photoUrl.isNullOrBlank()) {
      AsyncImage(
        model = photoUrl,
        contentDescription = "Profile photo for $displayName",
        modifier = Modifier.fillMaxSize().clip(CircleShape),
        contentScale = ContentScale.Crop,
      )
    } else {
      Text(
        text = initials,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun QuickActionTile(
  icon: ImageVector,
  label: String,
  accent: Color,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier =
      modifier
        .clip(RoundedCornerShape(18.dp))
        .clickable(onClick = onClick)
        .testTag(testTag),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
    border = BorderStroke(1.dp, ZenCardBorder),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        modifier =
          Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = accent,
          modifier = Modifier.size(20.dp),
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = ZenTextPrimary,
        maxLines = 1,
      )
    }
  }
}
