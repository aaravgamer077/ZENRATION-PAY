package com.example.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.math.BigDecimal
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VoiceLanguage(
  val code: String,
  val displayName: String,
  val nativeBadge: String,
  val samplePreview: String,
) {
  HINDI(
    code = "hi",
    displayName = "Hindi Voice",
    nativeBadge = "हिंदी / Hinglish",
    samplePreview = "\"250 rupees ki payment simulation complete.\"",
  ),
  ENGLISH(
    code = "en",
    displayName = "English Voice",
    nativeBadge = "English (India)",
    samplePreview = "\"250 rupees payment simulation complete.\"",
  );

  companion object {
    fun fromCode(code: String?): VoiceLanguage {
      return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: HINDI
    }
  }
}

data class VoicePreferences(
  val isEnabled: Boolean = true,
  val language: VoiceLanguage = VoiceLanguage.HINDI,
  val volume: Float = 1.0f,
)

sealed interface TtsEngineStatus {
  data object Initializing : TtsEngineStatus

  data class Ready(val languageLabel: String) : TtsEngineStatus

  data class Unavailable(val reason: String) : TtsEngineStatus
}

class VoiceAnnouncementManager(context: Context) {

  private val appContext = context.applicationContext
  private val sharedPrefs =
    appContext.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

  private val _preferences = MutableStateFlow(loadSavedPreferences())
  val preferences: StateFlow<VoicePreferences> = _preferences.asStateFlow()

  private val _engineStatus = MutableStateFlow<TtsEngineStatus>(TtsEngineStatus.Initializing)
  val engineStatus: StateFlow<TtsEngineStatus> = _engineStatus.asStateFlow()

  private val _isSpeaking = MutableStateFlow(false)
  val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

  private val _lastSpokenText = MutableStateFlow<String?>(null)
  val lastSpokenText: StateFlow<String?> = _lastSpokenText.asStateFlow()

  // Tracks announced transaction IDs so each simulated transaction is announced ONLY ONCE
  private val announcedTransactionIds = mutableSetOf<String>()

  private var tts: TextToSpeech? = null
  private var isTtsReady = false
  private var pendingSpeechText: String? = null

  init {
    initializeTts()
  }

  private fun initializeTts() {
    try {
      _engineStatus.value = TtsEngineStatus.Initializing
      tts =
        TextToSpeech(appContext) { status ->
          if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            configureTtsListeners()
            applyLocaleToTts(_preferences.value.language)
            pendingSpeechText?.let { queued ->
              pendingSpeechText = null
              speakRaw(queued, _preferences.value)
            }
          } else {
            isTtsReady = false
            _engineStatus.value =
              TtsEngineStatus.Unavailable(
                "TextToSpeech engine unavailable on this device (code $status)."
              )
          }
        }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to initialize TextToSpeech", e)
      isTtsReady = false
      _engineStatus.value =
        TtsEngineStatus.Unavailable(
          e.localizedMessage ?: "No TextToSpeech service found on device."
        )
    }
  }

  private fun configureTtsListeners() {
    tts?.setOnUtteranceProgressListener(
      object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
          _isSpeaking.value = true
        }

        override fun onDone(utteranceId: String?) {
          _isSpeaking.value = false
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
          _isSpeaking.value = false
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
          _isSpeaking.value = false
        }
      }
    )
  }

  private fun applyLocaleToTts(language: VoiceLanguage) {
    val engine = tts ?: return
    if (!isTtsReady) return

    try {
      val preferredLocale =
        when (language) {
          VoiceLanguage.HINDI -> Locale("hi", "IN")
          VoiceLanguage.ENGLISH -> Locale("en", "IN")
        }
      val result = engine.setLanguage(preferredLocale)
      if (
        result == TextToSpeech.LANG_MISSING_DATA ||
          result == TextToSpeech.LANG_NOT_SUPPORTED
      ) {
        // Graceful fallback to English (India) or US English if Hindi voice data is not installed
        val fallbackResult = engine.setLanguage(Locale("en", "IN"))
        if (
          fallbackResult == TextToSpeech.LANG_MISSING_DATA ||
            fallbackResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
          engine.setLanguage(Locale.US)
        }
        _engineStatus.value =
          TtsEngineStatus.Ready("${language.displayName} (System Voice Fallback)")
      } else {
        _engineStatus.value = TtsEngineStatus.Ready(language.displayName)
      }
      engine.setSpeechRate(0.96f)
      engine.setPitch(1.0f)
    } catch (e: Exception) {
      Log.w(TAG, "Failed to set TTS locale", e)
      _engineStatus.value = TtsEngineStatus.Ready(language.displayName)
    }
  }

  private fun loadSavedPreferences(): VoicePreferences {
    val enabled = sharedPrefs.getBoolean(KEY_ENABLED, true)
    val langCode = sharedPrefs.getString(KEY_LANGUAGE, VoiceLanguage.HINDI.code)
    val volume = sharedPrefs.getFloat(KEY_VOLUME, 1.0f).coerceIn(0f, 1f)
    return VoicePreferences(
      isEnabled = enabled,
      language = VoiceLanguage.fromCode(langCode),
      volume = volume,
    )
  }

  fun setAnnouncementEnabled(enabled: Boolean) {
    val updated = _preferences.value.copy(isEnabled = enabled)
    _preferences.value = updated
    sharedPrefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    if (!enabled) {
      stopSpeaking()
    }
  }

  fun setVoiceLanguage(language: VoiceLanguage) {
    val updated = _preferences.value.copy(language = language)
    _preferences.value = updated
    sharedPrefs.edit().putString(KEY_LANGUAGE, language.code).apply()
    applyLocaleToTts(language)
  }

  fun setVolume(volume: Float) {
    val clamped = volume.coerceIn(0f, 1f)
    val updated = _preferences.value.copy(volume = clamped)
    _preferences.value = updated
    sharedPrefs.edit().putFloat(KEY_VOLUME, clamped).apply()
  }

  /**
   * Automatically announces a completed payment simulation ONCE per unique transaction ID.
   * Never repeats if recomposed or revisited.
   */
  fun announceTransactionOnce(transactionKey: String, amount: Double): String? {
    val safeKey = transactionKey.trim().ifBlank { return null }
    if (!announcedTransactionIds.add(safeKey)) {
      return null
    }
    val currentPrefs = _preferences.value
    val announcement = buildAnnouncementSentence(amount, currentPrefs.language)
    _lastSpokenText.value = announcement

    if (!currentPrefs.isEnabled || currentPrefs.volume <= 0.01f) {
      return announcement
    }

    speakRaw(announcement, currentPrefs)
    return announcement
  }

  /**
   * Plays a test voice announcement immediately, flushing any previous utterance to prevent overlap.
   */
  fun playTestVoice(testAmount: Double): String {
    val currentPrefs = _preferences.value
    val announcement = buildAnnouncementSentence(testAmount, currentPrefs.language)
    _lastSpokenText.value = announcement
    val effectiveVolume = if (currentPrefs.volume <= 0.05f) 0.8f else currentPrefs.volume
    speakRaw(announcement, currentPrefs.copy(isEnabled = true, volume = effectiveVolume))
    return announcement
  }

  private fun speakRaw(text: String, prefs: VoicePreferences) {
    val engine = tts
    if (engine == null || !isTtsReady) {
      pendingSpeechText = text
      return
    }
    try {
      applyLocaleToTts(prefs.language)
      // Stop any ongoing speech and use QUEUE_FLUSH to prevent overlapping announcements
      engine.stop()
      val params =
        Bundle().apply {
          putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, prefs.volume.coerceIn(0f, 1f))
        }
      val utteranceId = "zenpay_sim_${UUID.randomUUID()}"
      engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    } catch (e: Exception) {
      Log.w(TAG, "Error speaking TTS announcement", e)
      _isSpeaking.value = false
    }
  }

  fun stopSpeaking() {
    try {
      tts?.stop()
    } catch (_: Exception) {}
    _isSpeaking.value = false
  }

  fun shutdown() {
    try {
      pendingSpeechText = null
      tts?.stop()
      tts?.shutdown()
    } catch (e: Exception) {
      Log.w(TAG, "Error shutting down TTS", e)
    } finally {
      tts = null
      isTtsReady = false
      _isSpeaking.value = false
    }
  }

  companion object {
    private const val TAG = "ZenVoiceManager"
    private const val PREFS_FILE_NAME = "zenration_voice_prefs"
    private const val KEY_ENABLED = "voice_announcement_enabled"
    private const val KEY_LANGUAGE = "voice_language_code"
    private const val KEY_VOLUME = "voice_volume_level"

    /**
     * Formats dynamic numeric amounts without commas or trailing .0 so TTS reads natural numbers:
     * 25.0 -> "25"
     * 150.0 -> "150"
     * 750.0 -> "750"
     * 2000.0 -> "2000"
     * 150.50 -> "150.5"
     */
    fun formatAmountForSpeech(amount: Double): String {
      if (amount.isNaN() || amount.isInfinite() || amount <= 0.0) return "0"
      val bd = BigDecimal.valueOf(amount).stripTrailingZeros()
      return bd.toPlainString()
    }

    /**
     * Builds the dynamic announcement string.
     * - Never hardcodes any amount.
     * - Never speaks the word "Demo".
     * - Always uses the exact phrase "payment simulation complete".
     *
     * Examples (Hindi / Hinglish):
     * - ₹25 -> "25 rupees ki payment simulation complete."
     * - ₹150 -> "150 rupees ki payment simulation complete."
     * - ₹750 -> "750 rupees ki payment simulation complete."
     * - ₹2,000 -> "2000 rupees ki payment simulation complete."
     */
    fun buildAnnouncementSentence(amount: Double, language: VoiceLanguage): String {
      val amountStr = formatAmountForSpeech(amount)
      return when (language) {
        VoiceLanguage.HINDI -> "$amountStr rupees ki payment simulation complete."
        VoiceLanguage.ENGLISH -> "$amountStr rupees payment simulation complete."
      }
    }
  }
}
