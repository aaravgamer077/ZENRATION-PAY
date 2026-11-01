package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.UpiQrParser
import com.example.voice.VoiceAnnouncementManager
import com.example.voice.VoiceLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ZENRATION PAY", appName)
  }

  @Test
  fun `UpiQrParser parses standard UPI URI and plain UPI handle`() {
    val parsedUri =
      UpiQrParser.parse(
        "upi://pay?pa=starlightcoffee@okaxis&pn=Starlight%20Coffee%20Roasters&am=320.50&tn=Espresso&cu=INR"
      )
    assertNotNull(parsedUri)
    assertEquals("starlightcoffee@okaxis", parsedUri?.recipientUpiId)
    assertEquals("Starlight Coffee Roasters", parsedUri?.recipientName)
    assertEquals("320.50", parsedUri?.amount)
    assertEquals("Espresso", parsedUri?.note)

    val parsedHandle = UpiQrParser.parse("nova.merchant@ybl")
    assertNotNull(parsedHandle)
    assertEquals("nova.merchant@ybl", parsedHandle?.recipientUpiId)
    assertEquals("Nova Merchant", parsedHandle?.recipientName)
  }

  @Test
  fun `VoiceAnnouncementManager formats dynamic amounts and never speaks Demo`() {
    assertEquals(
      "25 rupees ki payment simulation complete.",
      VoiceAnnouncementManager.buildAnnouncementSentence(25.0, VoiceLanguage.HINDI),
    )
    assertEquals(
      "150 rupees ki payment simulation complete.",
      VoiceAnnouncementManager.buildAnnouncementSentence(150.0, VoiceLanguage.HINDI),
    )
    assertEquals(
      "750 rupees ki payment simulation complete.",
      VoiceAnnouncementManager.buildAnnouncementSentence(750.0, VoiceLanguage.HINDI),
    )
    assertEquals(
      "2000 rupees ki payment simulation complete.",
      VoiceAnnouncementManager.buildAnnouncementSentence(2000.0, VoiceLanguage.HINDI),
    )

    val englishSentence =
      VoiceAnnouncementManager.buildAnnouncementSentence(750.0, VoiceLanguage.ENGLISH)
    assertEquals("750 rupees payment simulation complete.", englishSentence)
    assertFalse(englishSentence.contains("demo", ignoreCase = true))
    assertTrue(englishSentence.contains("payment simulation complete"))
  }

  @Test
  fun `VoiceAnnouncementManager persists preferences and announces once per transaction`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = VoiceAnnouncementManager(context)
    try {
      manager.setVoiceLanguage(VoiceLanguage.ENGLISH)
      manager.setVolume(0.65f)
      manager.setAnnouncementEnabled(false)

      // Recreate manager to verify SharedPreferences persistence
      val restoredManager = VoiceAnnouncementManager(context)
      try {
        assertEquals(VoiceLanguage.ENGLISH, restoredManager.preferences.value.language)
        assertEquals(0.65f, restoredManager.preferences.value.volume, 0.01f)
        assertEquals(false, restoredManager.preferences.value.isEnabled)

        // Announce transaction once
        val firstPlay = restoredManager.announceTransactionOnce("tx_unique_101", 150.0)
        assertEquals("150 rupees payment simulation complete.", firstPlay)
        val duplicatePlay = restoredManager.announceTransactionOnce("tx_unique_101", 150.0)
        assertNull(duplicatePlay)
      } finally {
        restoredManager.shutdown()
      }
    } finally {
      manager.shutdown()
    }
  }
}
