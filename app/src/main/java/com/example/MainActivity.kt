package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.ZenrationRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOut
import com.example.ui.components.TransactionDetailsModal
import com.example.ui.screens.CustomBalanceScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.QrScannerScreen
import com.example.ui.screens.VoiceSettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ZenBlack
import com.example.ui.theme.ZenCardBorder
import com.example.ui.theme.ZenDeepPurple
import com.example.ui.theme.ZenElectricBlue
import com.example.ui.theme.ZenRoyalPurple
import com.example.ui.theme.ZenSurfaceDark
import com.example.ui.theme.ZenTextMuted
import com.example.ui.theme.ZenTextPrimary
import com.example.ui.viewmodel.PaymentFlowState
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.ZenBottomTab
import com.example.ui.viewmodel.ZenrationViewModel
import com.example.voice.VoiceAnnouncementManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        AppNavigation()
      }
    }
  }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
  val flowUser by
    auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val activeUserId = flowUser?.uid ?: ZenrationRepository.DEFAULT_LOCAL_UID
  ZenrationMainShell(currentUserId = activeUserId)
}

@Composable
fun ZenrationMainShell(currentUserId: String) {
  val viewModel: ZenrationViewModel =
    viewModel(
      key = currentUserId,
      factory =
        viewModelFactory {
          initializer {
            val app =
              checkNotNull(this[APPLICATION_KEY]) {
                "APPLICATION_KEY missing from CreationExtras"
              }
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            ZenrationViewModel(
              repository =
                ZenrationRepository(
                  db = db,
                  auth = Firebase.auth,
                  appContext = app.applicationContext,
                ),
              currentUserId = currentUserId,
              voiceManager = VoiceAnnouncementManager(app),
            )
          }
        },
    )

  val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
  val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
  val demoBalanceState by viewModel.demoBalanceState.collectAsStateWithLifecycle()
  val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
  val paymentFlowState by viewModel.paymentFlowState.collectAsStateWithLifecycle()
  val selectedTxDetail by viewModel.selectedTransactionDetail.collectAsStateWithLifecycle()
  val statusBannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
  val isCustomBalanceOpen by viewModel.isCustomBalanceOpen.collectAsStateWithLifecycle()

  val voicePreferences by viewModel.voicePreferences.collectAsStateWithLifecycle()
  val ttsEngineStatus by viewModel.ttsEngineStatus.collectAsStateWithLifecycle()
  val isVoiceSpeaking by viewModel.isVoiceSpeaking.collectAsStateWithLifecycle()
  val lastSpokenText by viewModel.lastSpokenText.collectAsStateWithLifecycle()

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(statusBannerMessage) {
    statusBannerMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearBannerMessage()
    }
  }

  // Handle system back navigation on secondary tabs and sub-states
  BackHandler(
    enabled =
      isCustomBalanceOpen || selectedTab != ZenBottomTab.HOME || selectedTxDetail != null
  ) {
    when {
      isCustomBalanceOpen -> viewModel.closeCustomBalanceScreen()
      selectedTxDetail != null -> viewModel.dismissTransactionDetails()
      selectedTab == ZenBottomTab.SCAN && paymentFlowState !is PaymentFlowState.IdleScanning -> {
        viewModel.resetScannerToIdle()
      }
      else -> viewModel.selectTab(ZenBottomTab.HOME)
    }
  }

  if (isCustomBalanceOpen) {
    val liveBalanceObj = (demoBalanceState as? UiState.Success)?.data
    val currentLiveBalance = liveBalanceObj?.demoBalance ?: liveBalanceObj?.balance
    CustomBalanceScreen(
      currentBalance = currentLiveBalance,
      onCommitCustomBalance = { customAmt, callback ->
        viewModel.applyCustomSimulatedBalance(customAmt, callback)
      },
      onClose = { viewModel.closeCustomBalanceScreen() },
    )
    return
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = ZenBlack,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    bottomBar = {
      ZenrationCenteredScannerBottomBar(
        selectedTab = selectedTab,
        onSelectTab = { tab ->
          if (tab == ZenBottomTab.SCAN) {
            viewModel.openQrScanner()
          } else {
            viewModel.selectTab(tab)
          }
        },
      )
    },
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      when (selectedTab) {
        ZenBottomTab.HOME -> {
          HomeScreen(
            userProfileState = userProfileState,
            demoBalanceState = demoBalanceState,
            transactionsState = transactionsState,
            voicePreferences = voicePreferences,
            onScanQrClick = { viewModel.openQrScanner() },
            onOpenCustomBalanceClick = { viewModel.openCustomBalanceScreen() },
            onOpenVoiceSettingsClick = { viewModel.selectTab(ZenBottomTab.VOICE) },
            onTestVoiceQuickClick = { amt -> viewModel.testVoiceAnnouncement(amt) },
            onViewAllHistoryClick = { viewModel.selectTab(ZenBottomTab.HISTORY) },
            onOpenAccountSettingsClick = { viewModel.selectTab(ZenBottomTab.PROFILE) },
            onTransactionClick = { tx -> viewModel.showTransactionDetails(tx) },
            onResetBalanceClick = { viewModel.resetOrTopUpDemoBalance(25000.0) },
            onTopUpBalanceClick = { newAmt -> viewModel.resetOrTopUpDemoBalance(newAmt) },
            onQuickSimulateMerchant = { preset -> viewModel.onSelectPresetMerchant(preset) },
            onShowToastMessage = { msg -> viewModel.notifyMessage(msg) },
          )
        }

        ZenBottomTab.SCAN -> {
          val balance = (demoBalanceState as? UiState.Success)?.data
          QrScannerScreen(
            paymentFlowState = paymentFlowState,
            demoBalance = balance,
            onQrDetected = { rawQr -> viewModel.onQrCodeDetected(rawQr) },
            onSelectPreset = { preset -> viewModel.onSelectPresetMerchant(preset) },
            onSimulatePayment = { payload, amt, note ->
              viewModel.simulatePayment(payload, amt, note)
            },
            onResetScanner = { viewModel.resetScannerToIdle() },
            onFakeReturnHome = { viewModel.onFakeButtonReturnHome() },
          )
        }

        ZenBottomTab.HISTORY -> {
          HistoryScreen(
            transactionsState = transactionsState,
            onTransactionClick = { tx -> viewModel.showTransactionDetails(tx) },
            onStartScanClick = { viewModel.openQrScanner() },
          )
        }

        ZenBottomTab.VOICE -> {
          VoiceSettingsScreen(
            voicePreferences = voicePreferences,
            engineStatus = ttsEngineStatus,
            isSpeaking = isVoiceSpeaking,
            lastSpokenText = lastSpokenText,
            onToggleEnabled = { enabled -> viewModel.setVoiceEnabled(enabled) },
            onSelectLanguage = { lang -> viewModel.setVoiceLanguage(lang) },
            onVolumeChange = { vol -> viewModel.setVoiceVolume(vol) },
            onTestVoice = { amt -> viewModel.testVoiceAnnouncement(amt) },
          )
        }

        ZenBottomTab.PROFILE -> {
          ProfileScreen(
            currentUserId = currentUserId,
            userProfileState = userProfileState,
            demoBalanceState = demoBalanceState,
            voicePreferences = voicePreferences,
            onOpenVoiceSettingsClick = { viewModel.selectTab(ZenBottomTab.VOICE) },
            onSaveAccountSettings = { name, upi, notif, bio ->
              viewModel.updateAccountSettings(name, upi, notif, bio)
            },
            onAdjustDemoBalance = { newAmt -> viewModel.resetOrTopUpDemoBalance(newAmt) },
            onLogoutClick = {
              viewModel.resetOrTopUpDemoBalance(25000.0)
              viewModel.selectTab(ZenBottomTab.HOME)
            },
          )
        }
      }

      selectedTxDetail?.let { transaction ->
        TransactionDetailsModal(
          transaction = transaction,
          onDismiss = { viewModel.dismissTransactionDetails() },
        )
      }
    }
  }
}

/**
 * Custom Fintech Bottom Navigation Bar with the glowing QR Scanner Button centered in the middle
 * ("bich me scanner button"), flanked by Home & History on the left and Voice & Profile on the right.
 */
@Composable
private fun ZenrationCenteredScannerBottomBar(
  selectedTab: ZenBottomTab,
  onSelectTab: (ZenBottomTab) -> Unit,
) {
  Surface(
    color = ZenSurfaceDark,
    tonalElevation = 10.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .windowInsetsPadding(WindowInsets.navigationBars)
          .padding(horizontal = 8.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceAround,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // 1. Home
      StandardBottomNavTab(
        label = "Home",
        selected = selectedTab == ZenBottomTab.HOME,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        onClick = { onSelectTab(ZenBottomTab.HOME) },
        testTag = "bottom_nav_home",
        modifier = Modifier.weight(1f),
      )

      // 2. History
      StandardBottomNavTab(
        label = "History",
        selected = selectedTab == ZenBottomTab.HISTORY,
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        onClick = { onSelectTab(ZenBottomTab.HISTORY) },
        testTag = "bottom_nav_history",
        modifier = Modifier.weight(1f),
      )

      // 3. CENTERED SCANNER BUTTON (Bich me Scanner Button)
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
          Modifier
            .weight(1.25f)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onSelectTab(ZenBottomTab.SCAN) }
            .padding(vertical = 2.dp)
            .testTag("bottom_nav_scan"),
      ) {
        Box(
          modifier =
            Modifier
              .size(54.dp)
              .clip(CircleShape)
              .background(
                brush =
                  Brush.linearGradient(
                    colors =
                      if (selectedTab == ZenBottomTab.SCAN) {
                        listOf(ZenElectricBlue, ZenRoyalPurple)
                      } else {
                        listOf(ZenDeepPurple, ZenRoyalPurple, ZenElectricBlue)
                      }
                  )
              )
              .border(
                width = 2.dp,
                color =
                  if (selectedTab == ZenBottomTab.SCAN) Color.White else ZenElectricBlue,
                shape = CircleShape,
              ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.QrCodeScanner,
            contentDescription = "Scan QR",
            tint = ZenTextPrimary,
            modifier = Modifier.size(26.dp),
          )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = "Scan QR",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color =
            if (selectedTab == ZenBottomTab.SCAN) ZenElectricBlue else ZenTextPrimary,
        )
      }

      // 4. Voice (Dynamic Soundbox)
      StandardBottomNavTab(
        label = "Voice",
        selected = selectedTab == ZenBottomTab.VOICE,
        selectedIcon = Icons.AutoMirrored.Filled.VolumeUp,
        unselectedIcon = Icons.AutoMirrored.Outlined.VolumeUp,
        onClick = { onSelectTab(ZenBottomTab.VOICE) },
        testTag = "bottom_nav_voice",
        modifier = Modifier.weight(1f),
      )

      // 5. Profile
      StandardBottomNavTab(
        label = "Profile",
        selected = selectedTab == ZenBottomTab.PROFILE,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        onClick = { onSelectTab(ZenBottomTab.PROFILE) },
        testTag = "bottom_nav_profile",
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun StandardBottomNavTab(
  label: String,
  selected: Boolean,
  selectedIcon: ImageVector,
  unselectedIcon: ImageVector,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier =
      modifier
        .clip(RoundedCornerShape(14.dp))
        .clickable(onClick = onClick)
        .padding(vertical = 6.dp)
        .testTag(testTag),
  ) {
    Box(
      modifier =
        Modifier
          .clip(RoundedCornerShape(50))
          .background(
            if (selected) ZenDeepPurple.copy(alpha = 0.38f) else Color.Transparent
          )
          .padding(horizontal = 14.dp, vertical = 4.dp),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = if (selected) selectedIcon else unselectedIcon,
        contentDescription = label,
        tint = if (selected) ZenElectricBlue else ZenTextMuted,
        modifier = Modifier.size(22.dp),
      )
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      color = if (selected) ZenElectricBlue else ZenTextMuted,
    )
  }
}
