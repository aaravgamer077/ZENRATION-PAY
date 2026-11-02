package com.example

import com.example.data.model.DemoContactsCatalog
import com.example.data.model.PaymentStyle
import com.example.data.repository.ZenrationRepository
import com.example.voice.VoiceAnnouncementManager
import com.example.voice.VoiceLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun dynamicVoiceAnnouncement_formatsExactHindiSentences() {
    assertEquals(
      "50 rupaye ki simulated payment hui.",
      VoiceAnnouncementManager.buildAnnouncementSentence(50.0, VoiceLanguage.HINDI),
    )
    assertEquals(
      "150 rupaye ki simulated payment hui.",
      VoiceAnnouncementManager.buildAnnouncementSentence(150.0, VoiceLanguage.HINDI),
    )
    assertEquals(
      "500 rupaye ki simulated payment hui.",
      VoiceAnnouncementManager.buildAnnouncementSentence(500.0, VoiceLanguage.HINDI),
    )
  }

  @Test
  fun ownerPasswordVerification_validatesHashWithoutPlaintextExposure() {
    val constructed =
      charArrayOf('Z', 'E', 'N', 'R', 'A', 'T', 'I', 'O', 'N', '-', 'X').concatToString()
    assertTrue(ZenrationRepository.verifyOwnerPassword(constructed))
    assertFalse(ZenrationRepository.verifyOwnerPassword("WRONG-PASS"))
    assertFalse(ZenrationRepository.verifyOwnerPassword(""))
  }

  @Test
  fun demoContactsCatalog_searchesByMobileUsernameAndName() {
    val byMobile = DemoContactsCatalog.search("9876543210")
    assertTrue(byMobile.any { it.name == "Aarav Sharma" })

    val byUsername = DemoContactsCatalog.search("priya_v")
    assertTrue(byUsername.any { it.name == "Priya Verma" })

    assertEquals("Bank Transfer", PaymentStyle.PAYTM_INSPIRED.bankTransferLabel)
    assertEquals("Bank Account", PaymentStyle.PHONEPE_INSPIRED.bankTransferLabel)
  }
}
