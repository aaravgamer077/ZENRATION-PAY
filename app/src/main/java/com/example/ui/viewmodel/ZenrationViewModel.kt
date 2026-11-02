package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DemoBalance
import com.example.data.model.PaymentStyle
import com.example.data.model.SimulatedTransaction
import com.example.data.model.UpiQrParser
import com.example.data.model.UpiQrPayload
import com.example.data.model.UserProfile
import com.example.data.repository.ZenrationRepository
import com.example.ui.screens.RecipientSearchMode
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

  data class EnterDemoPin(
    val payload: UpiQrPayload,
    val amount: Double,
    val note: String,
  ) : PaymentFlowState

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

  private val _paymentStyle = MutableStateFlow(repository.getSavedPaymentStyle())
  val paymentStyle: StateFlow<PaymentStyle> = _paymentStyle.asStateFlow()

  private val _isStyleSelectorOpen = MutableStateFlow(false)
  val isStyleSelectorOpen: StateFlow<Boolean> = _isStyleSelectorOpen.asStateFlow()

  private val _recipientSearchMode = MutableStateFlow<RecipientSearchMode?>(null)
  val recipientSearchMode: StateFlow<RecipientSearchMode?> = _recipientSearchMode.asStateFlow()

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
      val remembered = repository.getRememberedUsername()
      val firebaseUser = Firebase.auth.currentUser
      val displayName =
        remembered.takeIf { it.isNotBlank() }
          ?: firebaseUser?.displayName?.takeIf { it.isNotBlank() }
          ?: firebaseUser?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
          ?: "Zenration Player"
      val email = firebaseUser?.email ?: "player@zenrationpay.sim"
      val photoUrl = firebaseUser?.photoUrl?.toString()

      repository.ensureOrUpdateUserProfile(
        displayName = displayName,
        email = email,
        photoUrl = photoUrl,
      )
      repository.ensureDemoBalanceInitialized()
    }
  }

  fun getRememberedUsername(): String = repository.getRememberedUsername()

  fun completeOwnerLoginAndStyle(username: String, style: PaymentStyle) {
    repository.saveRememberedUsername(username)
    setPaymentStyle(style)
    _isStyleSelectorOpen.value = false
  }

  fun setPaymentStyle(style: PaymentStyle) {
    _paymentStyle.value = style
    repository.savePaymentStyle(style)
    showBanner("Switched to ${style.title} simulator interface.")
  }

  fun openStyleSelector() {
    _isStyleSelectorOpen.value = true
  }

  fun closeStyleSelector() {
    _isStyleSelectorOpen.value = false
  }

  fun openRecipientSearch(mode: RecipientSearchMode = RecipientSearchMode.ALL) {
    _recipientSearchMode.value = mode
  }

  fun closeRecipientSearch() {
    _recipientSearchMode.value = null
  }

  fun selectTab(tab: ZenBottomTab) {
    _selectedTab.value = tab
    if (tab != ZenBottomTab.SCAN && _paymentFlowState.value is PaymentFlowState.SimulatedSuccess) {
      _paymentFlowState.value = PaymentFlowState.IdleScanning
    }
  }

  fun openQrScanner() {
    _recipientSearchMode.value = null
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
            "Unrecognized UPI QR format. Expected standard UPI URI (upi://pay?pa=...), mobile number, or valid UPI ID (name@bank)."
        )
    }
  }

  fun onSelectPresetMerchant(payload: UpiQrPayload) {
    _recipientSearchMode.value = null
    _paymentFlowState.value = PaymentFlowState.EnterAmount(payload)
    _selectedTab.value = ZenBottomTab.SCAN
  }

  fun resetScannerToIdle() {
    _paymentFlowState.value = PaymentFlowState.IdleScanning
  }

  fun returnFromPinToAmountEntry() {
    val current = _paymentFlowState.value
    if (current is PaymentFlowState.EnterDemoPin) {
      _paymentFlowState.value =
        PaymentFlowState.EnterAmount(
          current.payload.copy(
            amount =
              if (current.amount % 1.0 == 0.0) {
                current.amount.toLong().toString()
              } else {
                current.amount.toString()
              },
            note = current.note,
          )
        )
    } else {
      _paymentFlowState.value = PaymentFlowState.IdleScanning
    }
  }

  /**
   * Validates that amount > 0 on the Amount Entry Screen and transitions to the 6-digit Demo PIN Screen.
   */
  fun proceedToDemoPin(
    payload: UpiQrPayload,
    amountInput: String,
    noteInput: String,
  ) {
    val sanitized = amountInput.replace(",", "").replace("₹", "").trim()
    val amount = sanitized.toDoubleOrNull()
    if (amount == null || amount <= 0.0 || amount > 10000000.0) {
      _paymentFlowState.value =
        PaymentFlowState.Error(
          message = "Please enter a valid simulation amount greater than ₹0 (up to ₹1,00,00,000).",
          previousPayload = payload,
        )
      return
    }
    _paymentFlowState.value =
      PaymentFlowState.EnterDemoPin(
        payload = payload,
        amount = amount,
        note = noteInput.trim(),
      )
  }

  fun confirmDemoPinAndSimulate() {
    val current = _paymentFlowState.value as? PaymentFlowState.EnterDemoPin ?: return
    executeSimulatedTransaction(
      payload = current.payload,
      amount = current.amount,
      note = current.note,
      deductFromBalance = true,
    )
  }

  fun simulatePayment(
    payload: UpiQrPayload,
    amountInput: String,
    noteInput: String,
    deductFromBalance: Boolean = true,
  ) {
    val sanitized = amountInput.replace(",", "").replace("₹", "").trim()
    val amount = sanitized.toDoubleOrNull()
    if (amount == null || amount <= 0.0 || amount > 10000000.0) {
      _paymentFlowState.value =
        PaymentFlowState.Error(
          message = "Please enter a valid simulation amount greater than ₹0 (up to ₹1,00,00,000).",
          previousPayload = payload,
        )
      return
    }
    executeSimulatedTransaction(
      payload = payload,
      amount = amount,
      note = noteInput.trim(),
      deductFromBalance = deductFromBalance,
    )
  }

  private fun executeSimulatedTransaction(
    payload: UpiQrPayload,
    amount: Double,
    note: String,
    deductFromBalance: Boolean,
  ) {
    viewModelScope.launch {
      _paymentFlowState.value =
        PaymentFlowState.Processing(
          payload = payload,
          amount = amount,
          note = note,
          stageMessage = "Verifying Dummy PIN & Recipient Parameters...",
        )
      delay(450L)
      _paymentFlowState.value =
        PaymentFlowState.Processing(
          payload = payload,
          amount = amount,
          note = note,
          stageMessage = "Simulating Encrypted Sandbox Ledger...",
        )
      delay(450L)
      _paymentFlowState.value =
        PaymentFlowState.Processing(
          payload = payload,
          amount = amount,
          note = note,
          stageMessage = "Saving SIMULATED – NOT PAID Entry...",
        )

      val result =
        repository.createSimulatedTransaction(
          recipientName = payload.recipientName,
          recipientUpiId = payload.recipientUpiId,
          amount = amount,
          note = note.ifBlank { null },
          deductFromDemoBalance = deductFromBalance,
          recipientMobile = payload.recipientMobile,
          paymentStyle = _paymentStyle.value,
        )
      delay(250L)

      result
        .onSuccess { simulatedTx ->
          val uniqueKey = simulatedTx.id.ifBlank { simulatedTx.referenceId }
          // Automatically announce exact amount entered by player ONCE per transaction
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
        showBanner("SIMULATED BALANCE UPDATED SUCCESSFULLY.")
        onResult(Result.success(safeAmount))
      } else {
        val fallback = repository.ensureDemoBalanceInitialized(safeAmount)
        if (fallback.isSuccess) {
          showBanner("SIMULATED BALANCE UPDATED SUCCESSFULLY.")
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
        showBanner("Account settings saved.")
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
