package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.SimulatedTransaction
import com.example.ui.components.SimulatedBadgePill
import com.example.ui.components.TransactionItemCard
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
import com.example.ui.viewmodel.UiState
import java.util.Locale

private enum class HistoryAmountFilter(val label: String) {
  ALL("All Simulated"),
  UNDER_500("Under ₹500"),
  OVER_500("₹500 & Above"),
}

@Composable
fun HistoryScreen(
  transactionsState: UiState<List<SimulatedTransaction>>,
  onTransactionClick: (SimulatedTransaction) -> Unit,
  onStartScanClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedFilter by remember { mutableStateOf(HistoryAmountFilter.ALL) }

  val allTransactions = (transactionsState as? UiState.Success)?.data.orEmpty()

  val filteredTransactions by
    remember(allTransactions, searchQuery, selectedFilter) {
      derivedStateOf {
        allTransactions.filter { tx ->
          val matchesQuery =
            searchQuery.isBlank() ||
              tx.recipientName.contains(searchQuery, ignoreCase = true) ||
              tx.recipientUpiId.contains(searchQuery, ignoreCase = true) ||
              tx.referenceId.contains(searchQuery, ignoreCase = true) ||
              (tx.note?.contains(searchQuery, ignoreCase = true) == true)

          val matchesAmount =
            when (selectedFilter) {
              HistoryAmountFilter.ALL -> true
              HistoryAmountFilter.UNDER_500 -> tx.amount < 500.0
              HistoryAmountFilter.OVER_500 -> tx.amount >= 500.0
            }

          matchesQuery && matchesAmount
        }
      }
    }

  val totalSimulatedVolume by
    remember(allTransactions) {
      derivedStateOf { allTransactions.sumOf { it.amount } }
    }

  LazyColumn(
    modifier = modifier.fillMaxSize().background(ZenBlack).testTag("history_screen_list"),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "Transaction History",
            style = MaterialTheme.typography.headlineMedium,
            color = ZenTextPrimary,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "Every entry is strictly SIMULATED • Cloud Synced",
            style = MaterialTheme.typography.bodySmall,
            color = ZenTextSecondary,
          )
        }
        SimulatedBadgePill(text = "SIMULATED", accentColor = ZenSimAmber)
      }
    }

    // Summary Analytics Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
        border = BorderStroke(1.dp, ZenCardBorder),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "Total Simulated Volume",
              style = MaterialTheme.typography.bodySmall,
              color = ZenTextSecondary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "₹${String.format(Locale.US, "%,.2f", totalSimulatedVolume)}",
              style = MaterialTheme.typography.headlineMedium,
              color = ZenElectricBlue,
              fontWeight = FontWeight.Bold,
            )
          }
          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "Simulated Count",
              style = MaterialTheme.typography.bodySmall,
              color = ZenTextSecondary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "${allTransactions.size} Entries",
              style = MaterialTheme.typography.titleLarge,
              color = ZenPurpleGlow,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }

    // Search & Filter Bar
    item {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = {
            Text(
              text = "Search recipient, UPI ID, or SIM reference...",
              color = ZenTextMuted,
            )
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Search simulated transactions",
              tint = ZenElectricBlue,
            )
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(
                  imageVector = Icons.Default.Clear,
                  contentDescription = "Clear search",
                  tint = ZenTextSecondary,
                )
              }
            }
          },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("history_search_input"),
          shape = RoundedCornerShape(16.dp),
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
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          HistoryAmountFilter.entries.forEach { filter ->
            val selected = selectedFilter == filter
            Surface(
              modifier =
                Modifier
                  .clip(RoundedCornerShape(50))
                  .clickable { selectedFilter = filter },
              shape = RoundedCornerShape(50),
              color =
                if (selected) ZenRoyalPurple.copy(alpha = 0.28f) else ZenSurfaceElevated,
              border =
                BorderStroke(
                  1.dp,
                  if (selected) ZenElectricBlue else ZenCardBorder,
                ),
            ) {
              Text(
                text = filter.label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) ZenElectricBlue else ZenTextSecondary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
              )
            }
          }
        }
      }
    }

    when (transactionsState) {
      is UiState.Loading -> {
        item {
          Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(color = ZenElectricBlue)
          }
        }
      }

      is UiState.Error -> {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
            border = BorderStroke(1.dp, ZenCardBorder),
          ) {
            Text(
              text = transactionsState.message,
              style = MaterialTheme.typography.bodyMedium,
              color = Color(0xFFFF8A80),
              modifier = Modifier.padding(20.dp),
            )
          }
        }
      }

      is UiState.Success -> {
        if (filteredTransactions.isEmpty()) {
          item {
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(22.dp),
              colors = CardDefaults.cardColors(containerColor = ZenSurfaceDark),
              border = BorderStroke(1.dp, ZenCardBorder),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                  contentDescription = null,
                  tint = ZenPurpleGlow,
                  modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text =
                    if (searchQuery.isNotBlank()) {
                      "No Matching Simulated Transactions"
                    } else {
                      "Your Simulated History is Empty"
                    },
                  style = MaterialTheme.typography.titleMedium,
                  color = ZenTextPrimary,
                  textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text =
                    "Every simulated UPI payment you test is recorded in Firestore with a SIMULATED badge.",
                  style = MaterialTheme.typography.bodySmall,
                  color = ZenTextSecondary,
                  textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                  onClick = onStartScanClick,
                  shape = RoundedCornerShape(14.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = ZenRoyalPurple),
                ) {
                  Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(text = "Simulate a QR Payment", color = Color.White)
                }
              }
            }
          }
        } else {
          items(
            items = filteredTransactions,
            key = { it.id.ifBlank { it.referenceId } },
          ) { tx ->
            TransactionItemCard(
              transaction = tx,
              onClick = { onTransactionClick(tx) },
            )
          }
        }
      }
    }
  }
}
