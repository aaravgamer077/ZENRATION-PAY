package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DemoBalance
import com.example.data.model.SimulatedTransaction
import com.example.data.model.UpiQrParser
import com.example.data.model.UpiQrPayload
import com.example.data.model.UserProfile
import com.example.data.repository.ZenrationRepository
import com.example.voice.TtsEngineStatus
import com.example.voice.VoiceAnnouncementManager
import com.example.voice.VoiceLanguage
import com.example.voice.VoicePreferences
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>

  data class Success<T>(val data: T) : UiState<T>

  data class Error(val message: String) : UiState<Nothing>
}

sealed interface PaymentFlowState {
  data object IdleScanning : PaymentFlowState

  data class EnterAmount(val payload: UpiQrPayload) : PaymentFlowState

  data class Processing(
    val payload: UpiQrPayload,
    val amount: Double,
    val note: String,
    val stageMessage: String,
  ) : PaymentFlowState

  data class SimulatedSuccess(
    val transaction: SimulatedTransaction,
    val announcedSentence: String? = null,
  ) : PaymentFlowState

  data class Error(val message: String, val previousPayload: UpiQrPayload? = null) :
    PaymentFlowState
}

enum class ZenBottomTab {
  HOME,
  HISTORY,
  SCAN,
  VOICE,
  PROFILE,
}

class ZenrationViewModel(
  private val repository: ZenrationRepository,
  private val currentUserId: String,
  private val voiceManager: VoiceAnnouncementManager,
) : ViewModel() {

  private val _selectedTab = MutableStateFlow(ZenBottomTab.HOME)
  val selectedTab: StateFlow<ZenBottomTab> = _selectedTab.asStateFlow()

  private val _paymentFlowState = MutableStateFlow<PaymentFlowState>(PaymentFlowState.IdleScanning)
  val paymentFlowState: StateFlow<PaymentFlowState> = _paymentFlowState.asStateFlow()

  private val _selectedTransactionDetail = MutableStateFlow<SimulatedTransaction?>(null)
  val selectedTransactionDetail: StateFlow<SimulatedTransaction?> =
    _selectedTransactionDetail.asStateFlow()

  private val _statusBannerMessage = MutableStateFlow<String?>(null)
  val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

  private val _isCustomBalanceOpen = MutableStateFlow(false)
  val isCustomBalanceOpen: StateFlow<Boolean> = _isCustomBalanceOpen.asStateFlow()

  val voicePreferences: StateFlow<VoicePreferences> = voiceManager.preferences
  val ttsEngineStatus: StateFlow<TtsEngineStatus> = voiceManager.engineStatus
  val isVoiceSpeaking: StateFlow<Boolean> = voiceManager.isSpeaking
  val lastSpokenText: StateFlow<String?> = voiceManager.lastSpokenText

  val userProfileState: StateFlow<UiState<UserProfile?>> =
    repository
      .observeUserProfile(currentUserId)
      .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
      .catch { error ->
        Log.w(TAG, "Error observing user profile", error)
        emit(UiState.Error(error.localizedMessage ?: "Failed to load user profile"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val demoBalanceState: StateFlow<UiState<DemoBalance?>> =
    repository
      .observeDemoBalance(currentUserId)
      .map<DemoBalance?, UiState<DemoBalance?>> { UiState.Success(it) }
      .catch { error ->
        Log.w(TAG, "Error observing demo balance", error)
        emit(UiState.Error(error.localizedMessage ?: "Failed to sync demo balance"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val transactionsState: StateFlow<UiState<List<SimulatedTransaction>>> =
    repository
      .observeTransactions(currentUserId)
      .map<List<SimulatedTransaction>, UiState<List<SimulatedTransaction>>> { UiState.Success(it) }
      .catch { error ->
        Log.w(TAG, "Error observing simulated transactions", error)
        emit(UiState.Error(error.localizedMessage ?: "Failed to load transaction history"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  init {
    bootstrapUserAndBalance()
  }

  private fun bootstrapUserAndBalance() {
    viewModelScope.launch {
      val firebaseUser = Firebase.auth.currentUser
      val displayName =
        firebaseUser?.displayName?.takeIf { it.isNotBlank() }
          ?: firebaseUser?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
          ?: "Zenration Member"
      val email = firebaseUser?.email ?: "member@zenrationpay.sim"
      val photoUrl = firebaseUser?.photoUrl?.toString()

      repository.ensureOrUpdateUserProfile(
        displayName = displayName,
        email = email,
        photoUrl = photoUrl,
      )
      repository.ensureDemoBalanceInitialized()
    }
  }

  fun selectTab(tab: ZenBottomTab) {
    _selectedTab.value = tab
    if (tab != ZenBottomTab.SCAN && _paymentFlowState.value is PaymentFlowState.SimulatedSuccess) {
      _paymentFlowState.value = PaymentFlowState.IdleScanning
    }
  }

  fun openQrScanner() {
    _paymentFlowState.value = PaymentFlowState.IdleScanning
    _selectedTab.value = ZenBottomTab.SCAN
  }

  fun onQrCodeDetected(rawQrString: String) {
    if (_paymentFlowState.value !is PaymentFlowState.IdleScanning) return
    val parsed = UpiQrParser.parse(rawQrString)
    if (parsed != null) {
      _paymentFlowState.value = PaymentFlowState.EnterAmount(parsed)
    } else {
      _paymentFlowState.value =
        PaymentFlowState.Error(
          message =
            "Unrecognized UPI QR format. Expected standard UPI URI (upi://pay?pa=...) or valid UPI ID (name@bank)."
        )
    }
  }

  fun onSelectPresetMerchant(payload: UpiQrPayload) {
    _paymentFlowState.value = PaymentFlowState.EnterAmount(payload)
    _selectedTab.value = ZenBottomTab.SCAN
  }

  fun resetScannerToIdle() {
    _paymentFlowState.value = PaymentFlowState.IdleScanning
  }

  fun simulatePayment(
    payload: UpiQrPayload,
    amountInput: String,
    noteInput: String,
    deductFromBalance: Boolean = true,
  ) {
    val amount = amountInput.trim().toDoubleOrNull()
    if (amount == null || amount <= 0.0 || amount > 10000000.0) {
      _paymentFlowState.value =
        PaymentFlowState.Error(
          message = "Please enter a valid simulation amount between ₹1 and ₹1,00,00,000.",
          previousPayload = payload,
        )
      return
    }

    viewModelScope.launch {
      _paymentFlowState.value =
        PaymentFlowState.Processing(
          payload = payload,
          amount = amount,
          note = noteInput.trim(),
          stageMessage = "Verifying UPI QR Parameters...",
        )
      delay(500L)
      _paymentFlowState.value =
        PaymentFlowState.Processing(
          payload = payload,
          amount = amount,
          note = noteInput.trim(),
          stageMessage = "Simulating Encrypted Sandbox Ledger...",
        )
      delay(550L)
      _paymentFlowState.value =
        PaymentFlowState.Processing(
          payload = payload,
          amount = amount,
          note = noteInput.trim(),
          stageMessage = "Recording SIMULATED Entry in Cloud Firestore...",
        )

      val result =
        repository.createSimulatedTransaction(
          recipientName = payload.recipientName,
          recipientUpiId = payload.recipientUpiId,
          amount = amount,
          note = noteInput.trim().ifBlank { null },
          deductFromDemoBalance = deductFromBalance,
        )
      delay(300L)

      result
        .onSuccess { simulatedTx ->
          val uniqueKey = simulatedTx.id.ifBlank { simulatedTx.referenceId }
          // Automatically announce exact amount entered by user ONCE per transaction
          val spokenSentence = voiceManager.announceTransactionOnce(uniqueKey, simulatedTx.amount)
          _paymentFlowState.value =
            PaymentFlowState.SimulatedSuccess(
              transaction = simulatedTx,
              announcedSentence = spokenSentence,
            )
        }
        .onFailure { error ->
          _paymentFlowState.value =
            PaymentFlowState.Error(
              message = error.localizedMessage ?: "Failed to record simulated transaction.",
              previousPayload = payload,
            )
        }
    }
  }

  fun setVoiceEnabled(enabled: Boolean) {
    voiceManager.setAnnouncementEnabled(enabled)
    showBanner(
      if (enabled) "Voice announcements enabled." else "Voice announcements muted."
    )
  }

  fun setVoiceLanguage(language: VoiceLanguage) {
    voiceManager.setVoiceLanguage(language)
    showBanner("Voice set to ${language.displayName}.")
  }

  fun setVoiceVolume(volume: Float) {
    voiceManager.setVolume(volume)
  }

  fun testVoiceAnnouncement(sampleAmount: Double) {
    val spoken = voiceManager.playTestVoice(sampleAmount)
    showBanner("Speaking: \"$spoken\"")
  }

  fun onFakeButtonReturnHome() {
    _paymentFlowState.value = PaymentFlowState.IdleScanning
    _selectedTab.value = ZenBottomTab.HOME
  }

  fun showTransactionDetails(transaction: SimulatedTransaction) {
    _selectedTransactionDetail.value = transaction
  }

  fun dismissTransactionDetails() {
    _selectedTransactionDetail.value = null
  }

  fun openCustomBalanceScreen() {
    _isCustomBalanceOpen.value = true
  }

  fun closeCustomBalanceScreen() {
    _isCustomBalanceOpen.value = false
  }

  fun applyCustomSimulatedBalance(
    customAmount: Double,
    onResult: (Result<Double>) -> Unit,
  ) {
    viewModelScope.launch {
      val safeAmount = customAmount.coerceIn(0.0, 1_000_000_000.0)
      val result = repository.updateDemoBalance(safeAmount)
      if (result.isSuccess) {
        showBanner("SIMULATED BALANCE UPDATED SUCCESSFULLY")
        onResult(Result.success(safeAmount))
      } else {
        val fallback = repository.ensureDemoBalanceInitialized(safeAmount)
        if (fallback.isSuccess) {
          showBanner("SIMULATED BALANCE UPDATED SUCCESSFULLY")
          onResult(Result.success(safeAmount))
        } else {
          val error =
            result.exceptionOrNull()
              ?: fallback.exceptionOrNull()
              ?: IllegalStateException("Failed to update simulated balance")
          onResult(Result.failure(error))
        }
      }
    }
  }

  fun resetOrTopUpDemoBalance(newAmount: Double) {
    viewModelScope.launch {
      val result = repository.updateDemoBalance(newAmount)
      if (result.isSuccess) {
        showBanner("Balance synced to ₹${String.format("%,.2f", newAmount)} (SIMULATED)")
      } else {
        repository.ensureDemoBalanceInitialized(newAmount)
        showBanner("Balance initialized to ₹${String.format("%,.2f", newAmount)}")
      }
    }
  }

  fun updateAccountSettings(
    displayName: String,
    upiId: String,
    notificationsEnabled: Boolean,
    biometricSimEnabled: Boolean,
  ) {
    viewModelScope.launch {
      val result =
        repository.updateUserSettings(
          displayName = displayName,
          upiId = upiId,
          notificationsEnabled = notificationsEnabled,
          biometricSimEnabled = biometricSimEnabled,
        )
      if (result.isSuccess) {
        showBanner("Account settings saved to Firestore.")
      } else {
        showBanner("Could not update settings: ${result.exceptionOrNull()?.localizedMessage}")
      }
    }
  }

  fun clearBannerMessage() {
    _statusBannerMessage.value = null
  }

  fun notifyMessage(message: String) {
    showBanner(message)
  }

  private fun showBanner(message: String) {
    _statusBannerMessage.value = message
  }

  override fun onCleared() {
    super.onCleared()
    voiceManager.shutdown()
  }

  companion object {
    private const val TAG = "ZenrationVM"
    private const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
