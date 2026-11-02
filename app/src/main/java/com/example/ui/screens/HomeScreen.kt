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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
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
import com.example.data.model.DemoContact
import com.example.data.model.DemoContactsCatalog
import com.example.data.model.PaymentStyle
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

private val PaytmPrimaryBlue = Color(0xFF002E6E)
private val PaytmVibrantCyan = Color(0xFF00B9F1)
private val PaytmLightCyan = Color(0xFF00E5FF)

private val PhonePeDeepPurple = Color(0xFF3B0A75)
private val PhonePeRichViolet = Color(0xFF5F259F)
private val PhonePeLightPurple = Color(0xFFB388FF)

@Composable
fun HomeScreen(
  userProfileState: UiState<UserProfile?>,
  demoBalanceState: UiState<DemoBalance?>,
  transactionsState: UiState<List<SimulatedTransaction>>,
  voicePreferences: VoicePreferences,
  paymentStyle: PaymentStyle = PaymentStyle.PAYTM_INSPIRED,
  onSelectPaymentStyle: (PaymentStyle) -> Unit = {},
  onOpenStyleSelectorClick: () -> Unit = {},
  onOpenRecipientSearchClick: (RecipientSearchMode) -> Unit = {},
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

  val isPaytmStyle = paymentStyle == PaymentStyle.PAYTM_INSPIRED
  val stylePrimaryAccent = if (isPaytmStyle) PaytmLightCyan else PhonePeLightPurple
  val styleSecondaryAccent = if (isPaytmStyle) PaytmVibrantCyan else PhonePeRichViolet
  val styleBannerGradient =
    if (isPaytmStyle) {
      listOf(PaytmPrimaryBlue, Color(0xFF00529B), PaytmVibrantCyan)
    } else {
      listOf(PhonePeDeepPurple, PhonePeRichViolet, Color(0xFF7C4DFF))
    }

  val displayName =
    profile?.name?.takeIf { it.isNotBlank() }
      ?: profile?.displayName?.takeIf { it.isNotBlank() }
      ?: "Zenration Player"
  val userEmail = profile?.email.orEmpty()
  val userUpi = profile?.upiId ?: "player@zenpay.sim"
  val currentBalanceAmount = balance?.demoBalance ?: balance?.balance ?: 0.0

  LazyColumn(
    modifier = modifier.fillMaxSize().background(ZenBlack).testTag("home_screen_list"),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    // 1. Top Header: Three-Dot Menu (⋮) at Top-Left + ZENRATION PAY Logo + User Profile Avatar
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f).padding(end = 8.dp),
        ) {
          Box {
            IconButton(
              onClick = { isTopLeftMenuExpanded = true },
              modifier =
                Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .background(ZenSurfaceElevated)
                  .border(1.dp, stylePrimaryAccent.copy(alpha = 0.65f), CircleShape)
                  .testTag("home_three_dot_menu_button"),
            ) {
              Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More Options Menu",
                tint = stylePrimaryAccent,
              )
            }

            DropdownMenu(
              expanded = isTopLeftMenuExpanded,
              onDismissRequest = { isTopLeftMenuExpanded = false },
              modifier =
                Modifier
                  .background(ZenSurfaceElevated)
                  .border(1.dp, stylePrimaryAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            ) {
              DropdownMenuItem(
                text = {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.AccountBalanceWallet,
                      contentDescription = null,
                      tint = stylePrimaryAccent,
                      modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                      text = "CUSTOM YOUR BALANCE",
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

              DropdownMenuItem(
                text = {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.Palette,
                      contentDescription = null,
                      tint = stylePrimaryAccent,
                      modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                      text = "Choose Payment Style",
                      style = MaterialTheme.typography.titleSmall,
                      color = ZenTextPrimary,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                },
                onClick = {
                  isTopLeftMenuExpanded = false
                  onOpenStyleSelectorClick()
                },
                modifier = Modifier.testTag("menu_option_switch_style"),
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
              text =
                if (isPaytmStyle) {
                  "PAYTM-INSPIRED • SIMULATED"
                } else {
                  "PHONEPE-INSPIRED • SIMULATED"
                },
              style = MaterialTheme.typography.labelSmall,
              color = stylePrimaryAccent,
              fontWeight = FontWeight.Bold,
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
          border = BorderStroke(1.dp, stylePrimaryAccent.copy(alpha = 0.5f)),
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
                color = stylePrimaryAccent,
              )
            }
          }
        }
      }
    }

    // 2. Payment Style Switcher Strip (Paytm-Inspired Blue/Cyan vs PhonePe-Inspired Purple)
    item {
      Card(
        modifier = Modifier.fillMaxWidth().testTag("home_style_switcher_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
        border = BorderStroke(1.2.dp, stylePrimaryAccent.copy(alpha = 0.55f)),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(10.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            modifier =
              Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .clickable { onSelectPaymentStyle(PaymentStyle.PAYTM_INSPIRED) }
                .testTag("switch_to_paytm_style_button"),
            shape = RoundedCornerShape(14.dp),
            color =
              if (isPaytmStyle) PaytmVibrantCyan.copy(alpha = 0.25f) else Color.Transparent,
            border =
              BorderStroke(
                width = 1.dp,
                color = if (isPaytmStyle) PaytmLightCyan else ZenCardBorder,
              ),
          ) {
            Column(
              modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Text(
                text = "PAYTM-INSPIRED",
                style = MaterialTheme.typography.labelMedium,
                color = if (isPaytmStyle) PaytmLightCyan else ZenTextSecondary,
                fontWeight = FontWeight.ExtraBold,
              )
              Text(
                text = "Blue & Light-Cyan",
                style = MaterialTheme.typography.labelSmall,
                color = if (isPaytmStyle) Color.White else ZenTextMuted,
              )
            }
          }

          Surface(
            modifier =
              Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .clickable { onSelectPaymentStyle(PaymentStyle.PHONEPE_INSPIRED) }
                .testTag("switch_to_phonepe_style_button"),
            shape = RoundedCornerShape(14.dp),
            color =
              if (!isPaytmStyle) PhonePeRichViolet.copy(alpha = 0.35f) else Color.Transparent,
            border =
              BorderStroke(
                width = 1.dp,
                color = if (!isPaytmStyle) PhonePeLightPurple else ZenCardBorder,
              ),
          ) {
            Column(
              modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Text(
                text = "PHONEPE-INSPIRED",
                style = MaterialTheme.typography.labelMedium,
                color = if (!isPaytmStyle) PhonePeLightPurple else ZenTextSecondary,
                fontWeight = FontWeight.ExtraBold,
              )
              Text(
                text = "Royal Purple Theme",
                style = MaterialTheme.typography.labelSmall,
                color = if (!isPaytmStyle) Color.White else ZenTextMuted,
              )
            }
          }

          IconButton(
            onClick = onOpenStyleSelectorClick,
            modifier =
              Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ZenSurfaceElevated)
                .testTag("open_style_cards_button"),
          ) {
            Icon(
              imageVector = Icons.Default.Palette,
              contentDescription = "Open Style Selection Cards",
              tint = stylePrimaryAccent,
            )
          }
        }
      }
    }

    // 3. Recipient Search Bar (Search by Mobile Number, Username, or Saved Demo Contacts)
    item {
      Surface(
        modifier =
          Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onOpenRecipientSearchClick(RecipientSearchMode.ALL) }
            .testTag("home_recipient_search_bar"),
        shape = RoundedCornerShape(18.dp),
        color = ZenSurfaceDark,
        border = BorderStroke(1.2.dp, stylePrimaryAccent.copy(alpha = 0.6f)),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
          ) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Search recipients",
              tint = stylePrimaryAccent,
              modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Search Mobile Number, Username or Demo Contact",
                style = MaterialTheme.typography.bodyMedium,
                color = ZenTextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "Dummy contact directory • Instant simulated transfer",
                style = MaterialTheme.typography.labelSmall,
                color = ZenTextSecondary,
              )
            }
          }
          SimulatedBadgePill(text = "SEARCH", accentColor = stylePrimaryAccent)
        }
      }
    }

    // 4. TRANSFER MONEY SECTION (Paytm-Inspired Blue/Cyan OR PhonePe-Inspired Purple)
    item {
      Card(
        modifier = Modifier.fillMaxWidth().testTag("transfer_money_section_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.4.dp, stylePrimaryAccent.copy(alpha = 0.65f)),
      ) {
        Column(
          modifier =
            Modifier
              .fillMaxWidth()
              .background(
                brush =
                  Brush.verticalGradient(
                    colors =
                      if (isPaytmStyle) {
                        listOf(Color(0xFF002559), Color(0xFF06152D))
                      } else {
                        listOf(Color(0xFF2B0859), Color(0xFF130829))
                      }
                  )
              )
              .padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              Text(
                text = "Transfer Money (Simulated)",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
              )
              Text(
                text =
                  if (isPaytmStyle) {
                    "ZENRATION PAY • Paytm-Inspired Blue & Cyan"
                  } else {
                    "ZENRATION PAY • PhonePe-Inspired Purple"
                  },
                style = MaterialTheme.typography.labelSmall,
                color = stylePrimaryAccent,
              )
            }
            SimulatedBadgePill(
              text = "SIMULATED – NOT PAID",
              accentColor = ZenSimAmber,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            TransferOptionTile(
              icon = Icons.Default.PhoneAndroid,
              label = "Mobile Number",
              accentColor = stylePrimaryAccent,
              containerColor = styleSecondaryAccent.copy(alpha = 0.22f),
              onClick = { onOpenRecipientSearchClick(RecipientSearchMode.MOBILE_NUMBER) },
              testTag = "transfer_mobile_number_button",
              modifier = Modifier.weight(1f),
            )

            TransferOptionTile(
              icon = Icons.Default.AlternateEmail,
              label = "UPI ID",
              accentColor = stylePrimaryAccent,
              containerColor = styleSecondaryAccent.copy(alpha = 0.22f),
              onClick = { onOpenRecipientSearchClick(RecipientSearchMode.UPI_ID) },
              testTag = "transfer_upi_id_button",
              modifier = Modifier.weight(1f),
            )

            TransferOptionTile(
              icon = Icons.Default.QrCodeScanner,
              label = "Scan QR",
              accentColor = stylePrimaryAccent,
              containerColor = styleSecondaryAccent.copy(alpha = 0.22f),
              onClick = onScanQrClick,
              testTag = "transfer_scan_qr_button",
              modifier = Modifier.weight(1f),
            )

            TransferOptionTile(
              icon = Icons.Default.AccountBalance,
              label = paymentStyle.bankTransferLabel,
              accentColor = stylePrimaryAccent,
              containerColor = styleSecondaryAccent.copy(alpha = 0.22f),
              onClick = { onOpenRecipientSearchClick(RecipientSearchMode.BANK_TRANSFER) },
              testTag = "transfer_bank_button",
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }

    // 5. RECENT CONTACTS SECTION (Dummy Contact Data Only)
    item {
      Card(
        modifier = Modifier.fillMaxWidth().testTag("recent_contacts_section"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
        border = BorderStroke(1.dp, ZenCardBorder),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text = "Recent Contacts (Demo Only)",
                style = MaterialTheme.typography.titleSmall,
                color = ZenTextPrimary,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Tap any dummy contact to open Amount Entry",
                style = MaterialTheme.typography.labelSmall,
                color = ZenTextSecondary,
              )
            }
            TextButton(
              onClick = { onOpenRecipientSearchClick(RecipientSearchMode.ALL) },
              modifier = Modifier.testTag("recent_contacts_search_all_button"),
            ) {
              Text(
                text = "Search All",
                style = MaterialTheme.typography.labelMedium,
                color = stylePrimaryAccent,
                fontWeight = FontWeight.Bold,
              )
            }
          }

          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
          ) {
            items(
              items = DemoContactsCatalog.defaultContacts,
              key = { it.id },
            ) { contact ->
              RecentContactChip(
                contact = contact,
                accentColor = stylePrimaryAccent,
                onClick = { onQuickSimulateMerchant(contact.toUpiPayload()) },
              )
            }
          }
        }
      }
    }

    // 6. Demo Balance Hero Card (Clearly labeled SIMULATED BALANCE – NOT REAL MONEY)
    item {
      DemoBalanceHeroCard(
        demoBalance = balance,
        isLoading = demoBalanceState is UiState.Loading,
        onResetBalanceClick = onResetBalanceClick,
      )
    }

    // 7. Primary Scan QR CTA Button + Quick Navigation Actions
    item {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Button(
          onClick = onScanQrClick,
          modifier =
            Modifier
              .fillMaxWidth()
              .height(60.dp)
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
                  brush = Brush.horizontalGradient(colors = styleBannerGradient),
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
                tint = Color.White,
                modifier = Modifier.size(26.dp),
              )
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = "SCAN ANY UPI QR (SIMULATOR)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
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
            accent = stylePrimaryAccent,
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
            accent = stylePrimaryAccent,
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

    // 8. Dynamic Voice Soundbox Quick Control Strip on Home Screen
    item {
      Card(
        modifier = Modifier.fillMaxWidth().testTag("home_voice_soundbox_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
        border = BorderStroke(1.dp, stylePrimaryAccent.copy(alpha = 0.45f)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f).padding(end = 8.dp),
            ) {
              Box(
                modifier =
                  Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(styleSecondaryAccent.copy(alpha = 0.25f))
                    .border(1.dp, stylePrimaryAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Default.RecordVoiceOver,
                  contentDescription = null,
                  tint = stylePrimaryAccent,
                  modifier = Modifier.size(22.dp),
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Dynamic Voice Announcement",
                  style = MaterialTheme.typography.titleMedium,
                  color = ZenTextPrimary,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text =
                    "\"150 rupaye ki simulated payment hui.\" • ${(voicePreferences.volume * 100).toInt()}% Vol",
                  style = MaterialTheme.typography.bodySmall,
                  color = stylePrimaryAccent,
                )
              }
            }

            SimulatedBadgePill(
              text = if (voicePreferences.isEnabled) "ACTIVE" else "MUTED",
              accentColor = if (voicePreferences.isEnabled) stylePrimaryAccent else ZenSimAmber,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedButton(
              onClick = { onTestVoiceQuickClick(150.0) },
              modifier = Modifier.weight(1f).testTag("home_test_voice_button"),
              border = BorderStroke(1.dp, stylePrimaryAccent),
              shape = RoundedCornerShape(12.dp),
            ) {
              Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = stylePrimaryAccent,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(text = "Test Voice (₹150)", color = stylePrimaryAccent)
            }

            Button(
              onClick = onOpenVoiceSettingsClick,
              modifier = Modifier.weight(1f).testTag("home_voice_settings_button"),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = styleSecondaryAccent),
            ) {
              Text(text = "Voice Settings", color = Color.White)
            }
          }
        }
      }
    }

    // 9. Welcome & Player UPI Handle Card
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
              text = "Operator: $displayName",
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
                text = "Simulated UPI ID: $userUpi",
                style = MaterialTheme.typography.bodySmall,
                color = stylePrimaryAccent,
              )
              Spacer(modifier = Modifier.width(6.dp))
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy UPI ID",
                tint = stylePrimaryAccent,
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
          SimulatedBadgePill(text = "SANDBOX", accentColor = stylePrimaryAccent)
        }
      }
    }

    // 10. Transaction History Preview Section
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
            color = stylePrimaryAccent,
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
              tint = stylePrimaryAccent,
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
                "Select a Recent Contact, use Mobile Number / UPI ID / Scan QR above to run a simulated payment.",
              style = MaterialTheme.typography.bodySmall,
              color = ZenTextSecondary,
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    } else {
      items(
        items = transactions.take(5),
        key = { it.id.ifBlank { it.referenceId } },
      ) { tx ->
        TransactionItemCard(
          transaction = tx,
          onClick = { onTransactionClick(tx) },
        )
      }
    }

    // 11. Digital Sandbox Hub & Utility Buttons Section
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
            accent = stylePrimaryAccent,
            onClick = { onTopUpBalanceClick(currentBalanceAmount + 5000.0) },
            testTag = "sandbox_tool_topup",
            modifier = Modifier.weight(1f),
          )
          SandboxToolButtonCard(
            icon = Icons.Default.QrCode2,
            title = "Custom Balance",
            subtitle = "10s Matrix Protocol",
            accent = ZenPurpleGlow,
            onClick = onOpenCustomBalanceClick,
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
            title = "Test ₹500 Voice",
            subtitle = "Hear Hindi voice alert",
            accent = stylePrimaryAccent,
            onClick = { onTestVoiceQuickClick(500.0) },
            testTag = "sandbox_tool_test_2000_voice",
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    // 12. Sandbox Safety Footer
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
          text = "ZENRATION PAY Sandbox • SIMULATED – NOT PAID • Zero real money",
          style = MaterialTheme.typography.bodySmall,
          color = ZenTextMuted,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}

@Composable
private fun TransferOptionTile(
  icon: ImageVector,
  label: String,
  accentColor: Color,
  containerColor: Color,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .clip(RoundedCornerShape(16.dp))
        .clickable(onClick = onClick)
        .padding(vertical = 8.dp, horizontal = 4.dp)
        .testTag(testTag),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Box(
      modifier =
        Modifier
          .size(48.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(containerColor)
          .border(1.dp, accentColor.copy(alpha = 0.65f), RoundedCornerShape(14.dp)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = Color.White,
        modifier = Modifier.size(24.dp),
      )
    }
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = Color.White,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun RecentContactChip(
  contact: DemoContact,
  accentColor: Color,
  onClick: () -> Unit,
) {
  Column(
    modifier =
      Modifier
        .width(82.dp)
        .clip(RoundedCornerShape(16.dp))
        .clickable(onClick = onClick)
        .padding(vertical = 6.dp, horizontal = 4.dp)
        .testTag("recent_contact_chip_${contact.id}"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Box(
      modifier =
        Modifier
          .size(50.dp)
          .clip(CircleShape)
          .background(accentColor.copy(alpha = 0.16f))
          .border(1.2.dp, accentColor.copy(alpha = 0.7f), CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text =
          contact.name
            .split(" ")
            .take(2)
            .joinToString("") { it.firstOrNull()?.uppercase() ?: "" },
        style = MaterialTheme.typography.titleSmall,
        color = accentColor,
        fontWeight = FontWeight.ExtraBold,
      )
    }
    Text(
      text = contact.name.substringBefore(" "),
      style = MaterialTheme.typography.labelMedium,
      color = ZenTextPrimary,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = contact.mobileNumber.takeLast(5),
      style = MaterialTheme.typography.labelSmall,
      color = ZenTextMuted,
      maxLines = 1,
    )
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
