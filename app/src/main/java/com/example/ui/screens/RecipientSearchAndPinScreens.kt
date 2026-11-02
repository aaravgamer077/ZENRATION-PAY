package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DemoContact
import com.example.data.model.DemoContactsCatalog
import com.example.data.model.PaymentStyle
import com.example.data.model.UpiQrPayload
import com.example.ui.components.SimulatedBadgePill
import com.example.ui.theme.ZenBlack
import com.example.ui.theme.ZenCardBorder
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

enum class RecipientSearchMode(
  val title: String,
  val subtitle: String,
  val searchPlaceholder: String,
) {
  ALL(
    title = "Search Demo Recipients",
    subtitle = "Search by Mobile Number, Username, or Saved Demo Contacts",
    searchPlaceholder = "Enter mobile number, username, or contact name...",
  ),
  MOBILE_NUMBER(
    title = "Transfer via Mobile Number",
    subtitle = "Enter any 10-digit demo mobile number or pick a contact",
    searchPlaceholder = "Enter mobile number (e.g. 9876543210) or name...",
  ),
  UPI_ID(
    title = "Transfer via UPI ID",
    subtitle = "Enter any simulated UPI ID (@zenpay.sim) or pick a contact",
    searchPlaceholder = "Enter UPI ID (e.g. aarav@zenpay.sim) or username...",
  ),
  BANK_TRANSFER(
    title = "Simulated Bank Transfer",
    subtitle = "Select a demo contact bank account or enter custom recipient",
    searchPlaceholder = "Search recipient name, bank, or mobile number...",
  ),
}

/**
 * 6. RECIPIENT SEARCH SCREEN
 * Allows players to search for a recipient using:
 * - Mobile number
 * - Username
 * - Saved demo contacts
 * Uses dummy contact data only. Selecting a recipient opens the Amount Entry Screen.
 */
@Composable
fun RecipientSearchScreen(
  paymentStyle: PaymentStyle,
  initialMode: RecipientSearchMode = RecipientSearchMode.ALL,
  initialQuery: String = "",
  onRecipientSelected: (UpiQrPayload) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler { onClose() }

  val focusManager = LocalFocusManager.current
  var query by rememberSaveable { mutableStateOf(initialQuery) }
  val matchingContacts = remember(query) { DemoContactsCatalog.search(query) }

  val primaryAccent =
    if (paymentStyle == PaymentStyle.PAYTM_INSPIRED) {
      Color(0xFF00E5FF)
    } else {
      Color(0xFFB388FF)
    }
  val headerGradient =
    if (paymentStyle == PaymentStyle.PAYTM_INSPIRED) {
      listOf(Color(0xFF002E6E), Color(0xFF0078D7), Color(0xFF00B9F1))
    } else {
      listOf(Color(0xFF3B0A75), Color(0xFF5F259F), Color(0xFF7C4DFF))
    }

  fun selectCustomQueryRecipient() {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return
    focusManager.clearFocus()
    val digitsOnly = trimmed.replace(Regex("[^0-9]"), "")
    val payload =
      if (digitsOnly.length >= 10) {
        val ten = digitsOnly.takeLast(10)
        val formattedMobile = "+91 ${ten.substring(0, 5)} ${ten.substring(5)}"
        UpiQrPayload(
          recipientUpiId = "$ten@zenpay.sim",
          recipientName = "Demo Recipient ($ten)",
          recipientMobile = formattedMobile,
        )
      } else if (trimmed.contains("@")) {
        val namePart =
          trimmed.substringBefore("@").replace(Regex("[^a-zA-Z0-9 ]"), " ").trim().ifBlank {
            "Demo Recipient"
          }
        UpiQrPayload(
          recipientUpiId = trimmed,
          recipientName = namePart.replaceFirstChar { it.uppercase() },
          recipientMobile = "+91 98765 43210",
        )
      } else {
        val handle = trimmed.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "").ifBlank { "demo" }
        UpiQrPayload(
          recipientUpiId = "$handle@zenpay.sim",
          recipientName = trimmed,
          recipientMobile = "+91 98765 43210",
        )
      }
    onRecipientSelected(payload)
  }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(ZenBlack)
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .testTag("recipient_search_screen")
  ) {
    // Styled Top Header
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(brush = Brush.horizontalGradient(colors = headerGradient))
          .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = onClose,
              modifier =
                Modifier
                  .size(44.dp)
                  .clip(CircleShape)
                  .background(Color.Black.copy(alpha = 0.3f))
                  .testTag("recipient_search_back_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text =
                  if (initialMode == RecipientSearchMode.BANK_TRANSFER) {
                    paymentStyle.bankTransferLabel
                  } else {
                    initialMode.title
                  },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
              )
              Text(
                text = initialMode.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
              )
            }
          }
          SimulatedBadgePill(text = "DUMMY DATA", accentColor = ZenSimAmber)
        }

        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          placeholder = {
            Text(
              text = initialMode.searchPlaceholder,
              color = ZenTextMuted,
            )
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Search recipients",
              tint = primaryAccent,
            )
          },
          trailingIcon = {
            if (query.isNotEmpty()) {
              IconButton(onClick = { query = "" }) {
                Icon(
                  imageVector = Icons.Default.Clear,
                  contentDescription = "Clear search",
                  tint = ZenTextSecondary,
                )
              }
            }
          },
          singleLine = true,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(onSearch = { selectCustomQueryRecipient() }),
          modifier = Modifier.fillMaxWidth().testTag("recipient_search_input"),
          shape = RoundedCornerShape(16.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedContainerColor = ZenSurfaceDark,
              unfocusedContainerColor = ZenSurfaceDark,
              focusedBorderColor = primaryAccent,
              unfocusedBorderColor = ZenCardBorder,
              focusedTextColor = ZenTextPrimary,
              unfocusedTextColor = ZenTextPrimary,
            ),
        )
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Custom entered recipient quick action if query is non-blank
      if (query.trim().isNotEmpty()) {
        item {
          Card(
            modifier =
              Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable { selectCustomQueryRecipient() }
                .testTag("select_custom_recipient_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ZenSurfaceElevated),
            border = BorderStroke(1.2.dp, primaryAccent),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
              ) {
                Box(
                  modifier =
                    Modifier
                      .size(46.dp)
                      .clip(CircleShape)
                      .background(primaryAccent.copy(alpha = 0.18f)),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Default.PersonSearch,
                    contentDescription = null,
                    tint = primaryAccent,
                  )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(
                    text = "Proceed with \"${query.trim()}\"",
                    style = MaterialTheme.typography.titleSmall,
                    color = ZenTextPrimary,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "Tap to open Amount Entry for this custom dummy recipient",
                    style = MaterialTheme.typography.bodySmall,
                    color = primaryAccent,
                  )
                }
              }
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = primaryAccent,
              )
            }
          }
        }
      }

      item {
        Text(
          text = "SAVED DEMO CONTACTS (${matchingContacts.size})",
          style = MaterialTheme.typography.labelMedium,
          color = ZenTextSecondary,
          fontWeight = FontWeight.Bold,
        )
      }

      items(
        items = matchingContacts,
        key = { it.id },
      ) { contact ->
        DemoContactRowCard(
          contact = contact,
          accentColor = primaryAccent,
          showBankSubtitle = initialMode == RecipientSearchMode.BANK_TRANSFER,
          onClick = { onRecipientSelected(contact.toUpiPayload()) },
        )
      }
    }
  }
}

@Composable
private fun DemoContactRowCard(
  contact: DemoContact,
  accentColor: Color,
  showBankSubtitle: Boolean,
  onClick: () -> Unit,
) {
  Card(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .clickable(onClick = onClick)
        .testTag("demo_contact_card_${contact.id}"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
    border = BorderStroke(1.dp, ZenCardBorder),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(15.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.16f))
            .border(1.dp, accentColor.copy(alpha = 0.6f), CircleShape),
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

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = contact.name,
            style = MaterialTheme.typography.titleMedium,
            color = ZenTextPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "@${contact.username}",
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
          )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector =
              if (showBankSubtitle) Icons.Default.AccountBalance else Icons.Default.PhoneAndroid,
            contentDescription = null,
            tint = ZenTextSecondary,
            modifier = Modifier.size(13.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text =
              if (showBankSubtitle) {
                "${contact.bankName} • ${contact.mobileNumber}"
              } else {
                "${contact.mobileNumber} • ${contact.upiId}"
              },
            style = MaterialTheme.typography.bodySmall,
            color = ZenTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = "Select ${contact.name}",
        tint = accentColor,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

/**
 * 8. DEMO PIN SCREEN
 * Custom 6-digit PIN screen with:
 * - Six PIN input circles
 * - Numeric keypad (0–9)
 * - Delete button
 * - Confirm button
 * - Prominent warning: "DEMO ONLY: Enter any dummy PIN. Never enter your real UPI PIN."
 * Allows any dummy PIN for the simulation and never stores or transmits any PIN.
 */
@Composable
fun DemoPinScreen(
  payload: UpiQrPayload,
  amount: Double,
  paymentStyle: PaymentStyle,
  onConfirmDummyPin: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler { onBack() }

  var enteredDigits by rememberSaveable { mutableStateOf("") }
  var pinHintError by rememberSaveable { mutableStateOf<String?>(null) }

  val accentColor =
    if (paymentStyle == PaymentStyle.PAYTM_INSPIRED) {
      Color(0xFF00E5FF)
    } else {
      Color(0xFFB388FF)
    }

  fun onDigitPress(digit: Char) {
    if (enteredDigits.length < 6) {
      enteredDigits += digit
      pinHintError = null
    }
  }

  fun onDeletePress() {
    if (enteredDigits.isNotEmpty()) {
      enteredDigits = enteredDigits.dropLast(1)
      pinHintError = null
    }
  }

  fun onConfirmPress() {
    if (enteredDigits.isEmpty()) {
      // Allow any dummy PIN — if user taps confirm immediately without typing, auto-fill dummy 6 digits or prompt
      enteredDigits = "000000"
    }
    // Clear dummy digits immediately; never store or transmit any PIN
    enteredDigits = ""
    onConfirmDummyPin()
  }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(ZenBlack)
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 14.dp)
        .testTag("demo_pin_screen"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    // Top Header + Recipient & Amount Summary
    Column(
      modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = onBack,
          modifier =
            Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(ZenSurfaceElevated)
              .testTag("demo_pin_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back to Amount Entry",
            tint = ZenTextPrimary,
          )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "ZENRATION PAY SANDBOX PIN",
            style = MaterialTheme.typography.titleSmall,
            color = ZenTextPrimary,
            fontWeight = FontWeight.ExtraBold,
          )
          Text(
            text = "SIMULATED – NOT PAID",
            style = MaterialTheme.typography.labelSmall,
            color = ZenSimAmber,
            fontWeight = FontWeight.Bold,
          )
        }

        Box(
          modifier =
            Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(accentColor.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(20.dp),
          )
        }
      }

      // Mandatory Warning Banner
      Surface(
        modifier = Modifier.fillMaxWidth().testTag("demo_pin_warning_banner"),
        shape = RoundedCornerShape(16.dp),
        color = ZenSimAmber.copy(alpha = 0.16f),
        border = BorderStroke(1.5.dp, ZenSimAmber),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Default.WarningAmber,
            contentDescription = "Demo PIN Warning",
            tint = ZenSimAmber,
            modifier = Modifier.size(24.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "DEMO ONLY: Enter any dummy PIN. Never enter your real UPI PIN.",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.ExtraBold,
            color = ZenSimAmber,
          )
        }
      }

      // Recipient & Simulated Amount Summary Bar
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
        border = BorderStroke(1.dp, ZenCardBorder),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Simulating to: ${payload.recipientName}",
              style = MaterialTheme.typography.bodyMedium,
              color = ZenTextPrimary,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = payload.recipientMobile,
              style = MaterialTheme.typography.labelSmall,
              color = ZenTextSecondary,
            )
          }
          Text(
            text = "₹${String.format(Locale.US, "%,.2f", amount)}",
            style = MaterialTheme.typography.titleLarge,
            color = accentColor,
            fontWeight = FontWeight.ExtraBold,
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "ENTER 6-DIGIT DUMMY PIN",
        style =
          MaterialTheme.typography.labelLarge.copy(
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.6.sp,
          ),
        color = ZenTextSecondary,
        fontWeight = FontWeight.Bold,
      )

      // Six PIN Input Circles
      Row(
        modifier = Modifier.fillMaxWidth().testTag("demo_pin_circles_row"),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        for (idx in 0 until 6) {
          val isFilled = idx < enteredDigits.length
          Box(
            modifier =
              Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isFilled) accentColor else Color.Transparent)
                .border(
                  width = 2.dp,
                  color = if (isFilled) accentColor else ZenTextSecondary,
                  shape = CircleShape,
                )
                .testTag("pin_circle_$idx")
          )
        }
      }

      if (pinHintError != null) {
        Text(
          text = pinHintError.orEmpty(),
          style = MaterialTheme.typography.bodySmall,
          color = ZenSimAmber,
          textAlign = TextAlign.Center,
        )
      } else {
        Text(
          text = "${enteredDigits.length} / 6 dummy digits entered (any dummy combination works)",
          style = MaterialTheme.typography.labelSmall,
          color = ZenTextMuted,
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Numeric Keypad (0–9), Delete button, and Confirm button
    Column(
      modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      val rows =
        listOf(
          listOf('1', '2', '3'),
          listOf('4', '5', '6'),
          listOf('7', '8', '9'),
        )

      rows.forEach { rowDigits ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          rowDigits.forEach { digit ->
            PinKeypadButton(
              label = digit.toString(),
              onClick = { onDigitPress(digit) },
              modifier = Modifier.weight(1f).testTag("pin_key_$digit"),
            )
          }
        }
      }

      // Bottom Row: Delete, 0, Confirm
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Delete Button
        Surface(
          modifier =
            Modifier
              .weight(1f)
              .height(60.dp)
              .clip(RoundedCornerShape(16.dp))
              .clickable { onDeletePress() }
              .testTag("pin_key_delete"),
          shape = RoundedCornerShape(16.dp),
          color = ZenSurfaceElevated,
          border = BorderStroke(1.dp, ZenCardBorder),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Backspace,
              contentDescription = "Delete PIN digit",
              tint = ZenTextPrimary,
              modifier = Modifier.size(24.dp),
            )
          }
        }

        // Digit 0
        PinKeypadButton(
          label = "0",
          onClick = { onDigitPress('0') },
          modifier = Modifier.weight(1f).testTag("pin_key_0"),
        )

        // Confirm Button
        Surface(
          modifier =
            Modifier
              .weight(1f)
              .height(60.dp)
              .clip(RoundedCornerShape(16.dp))
              .clickable { onConfirmPress() }
              .testTag("demo_pin_confirm_button"),
          shape = RoundedCornerShape(16.dp),
          color = accentColor,
        ) {
          Box(contentAlignment = Alignment.Center) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
            ) {
              Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Confirm Dummy PIN",
                tint = ZenBlack,
                modifier = Modifier.size(22.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "CONFIRM",
                style = MaterialTheme.typography.labelLarge,
                color = ZenBlack,
                fontWeight = FontWeight.ExtraBold,
              )
            }
          }
        }
      }

      Button(
        onClick = { onConfirmPress() },
        modifier =
          Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("demo_pin_submit_full_button"),
        shape = RoundedCornerShape(16.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor =
              if (paymentStyle == PaymentStyle.PAYTM_INSPIRED) {
                Color(0xFF00B9F1)
              } else {
                ZenRoyalPurple
              }
          ),
      ) {
        Text(
          text = "CONFIRM DUMMY PIN & COMPLETE SIMULATION",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.ExtraBold,
          color = Color.White,
        )
      }
    }
  }
}

@Composable
private fun PinKeypadButton(
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier =
      modifier
        .height(60.dp)
        .clip(RoundedCornerShape(16.dp))
        .clickable(onClick = onClick),
    shape = RoundedCornerShape(16.dp),
    color = ZenSurfaceDark,
    border = BorderStroke(1.dp, ZenCardBorder),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = label,
        style = MaterialTheme.typography.headlineSmall,
        color = ZenTextPrimary,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}
