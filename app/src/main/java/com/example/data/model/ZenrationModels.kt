package com.example.data.model

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PaymentStyle(
  val id: String,
  val title: String,
  val subtitle: String,
  val bankTransferLabel: String,
) {
  PAYTM_INSPIRED(
    id = "PAYTM_INSPIRED",
    title = "PAYTM-INSPIRED",
    subtitle = "Blue & Light-Cyan Interface • ZENRATION PAY Original",
    bankTransferLabel = "Bank Transfer",
  ),
  PHONEPE_INSPIRED(
    id = "PHONEPE_INSPIRED",
    title = "PHONEPE-INSPIRED",
    subtitle = "Royal Purple Interface • ZENRATION PAY Original",
    bankTransferLabel = "Bank Account",
  );

  companion object {
    fun fromId(id: String?): PaymentStyle {
      return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: PAYTM_INSPIRED
    }
  }
}

data class DemoContact(
  val id: String,
  val name: String,
  val username: String,
  val mobileNumber: String,
  val upiId: String,
  val bankName: String,
  val roleTag: String = "Demo Contact",
) {
  fun toUpiPayload(defaultAmount: String = ""): UpiQrPayload {
    return UpiQrPayload(
      recipientUpiId = upiId,
      recipientName = name,
      recipientMobile = mobileNumber,
      amount = defaultAmount,
      note = "Simulated transfer to $name",
    )
  }
}

object DemoContactsCatalog {
  val defaultContacts: List<DemoContact> =
    listOf(
      DemoContact(
        id = "c1",
        name = "Aarav Sharma",
        username = "aarav_zen",
        mobileNumber = "+91 98765 43210",
        upiId = "aarav.sharma@zenpay.sim",
        bankName = "Zenration Sandbox Bank ••4021",
        roleTag = "Frequent",
      ),
      DemoContact(
        id = "c2",
        name = "Priya Verma",
        username = "priya_v",
        mobileNumber = "+91 98112 23344",
        upiId = "priya.verma@zenpay.sim",
        bankName = "Cyber Axis Sandbox ••8812",
        roleTag = "Recent",
      ),
      DemoContact(
        id = "c3",
        name = "Rohan Malhotra",
        username = "rohan_m",
        mobileNumber = "+91 97654 32109",
        upiId = "rohan.m@zenpay.sim",
        bankName = "Nova Demo Bank ••1190",
        roleTag = "Recent",
      ),
      DemoContact(
        id = "c4",
        name = "Ananya Gupta",
        username = "ananya_g",
        mobileNumber = "+91 99887 76655",
        upiId = "ananya.gupta@zenpay.sim",
        bankName = "Zenith Sandbox Bank ••5504",
        roleTag = "Saved",
      ),
      DemoContact(
        id = "c5",
        name = "Kabir Singhania",
        username = "kabir_x",
        mobileNumber = "+91 91234 56789",
        upiId = "kabir.s@zenpay.sim",
        bankName = "Metro Demo Ledger ••3309",
        roleTag = "Saved",
      ),
      DemoContact(
        id = "c6",
        name = "Neha Kapoor",
        username = "neha_k",
        mobileNumber = "+91 93456 78901",
        upiId = "neha.kapoor@zenpay.sim",
        bankName = "Apex Sandbox Bank ••7741",
        roleTag = "Saved",
      ),
      DemoContact(
        id = "c7",
        name = "Vikramaditya Rao",
        username = "vikram_rao",
        mobileNumber = "+91 94567 89012",
        upiId = "vikram.rao@zenpay.sim",
        bankName = "Zenration Sandbox Bank ••9012",
        roleTag = "Merchant",
      ),
      DemoContact(
        id = "c8",
        name = "Ishita Joshi",
        username = "ishita_j",
        mobileNumber = "+91 95678 90123",
        upiId = "ishita.joshi@zenpay.sim",
        bankName = "Pulse Demo Bank ••6128",
        roleTag = "Saved",
      ),
    )

  fun search(query: String): List<DemoContact> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return defaultContacts
    val digitsOnly = q.replace(Regex("[^0-9]"), "")
    return defaultContacts.filter { contact ->
      contact.name.lowercase().contains(q) ||
        contact.username.lowercase().contains(q) ||
        contact.upiId.lowercase().contains(q) ||
        contact.mobileNumber.lowercase().contains(q) ||
        (digitsOnly.isNotEmpty() &&
          contact.mobileNumber.replace(Regex("[^0-9]"), "").contains(digitsOnly))
    }
  }
}

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val displayName: String = "",
  val email: String = "",
  val photoURL: String? = null,
  val photoUrl: String? = null,
  val upiId: String? = null,
  val notificationsEnabled: Boolean = true,
  val biometricSimEnabled: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  companion object {
    fun fromSnapshot(snapshot: DocumentSnapshot, fallbackUserId: String): UserProfile {
      val resolvedName =
        snapshot.getString("name")?.takeIf { it.isNotBlank() }
          ?: snapshot.getString("displayName")?.takeIf { it.isNotBlank() }
          ?: "Zenration User"
      val resolvedPhoto =
        snapshot.getString("photoURL")?.takeIf { it.isNotBlank() }
          ?: snapshot.getString("photoUrl")?.takeIf { it.isNotBlank() }
      val resolvedEmail = snapshot.getString("email").orEmpty()
      val resolvedUid =
        snapshot.getString("userId")?.takeIf { it.isNotBlank() }
          ?: snapshot.id.ifBlank { fallbackUserId }
      val defaultUpi =
        "${resolvedEmail.substringBefore("@").lowercase().replace(Regex("[^a-z0-9]"), "").ifBlank { "user" }}@zenpay.sim"
      val createdAt =
        snapshot.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      val updatedAt =
        snapshot.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

      return UserProfile(
        id = snapshot.id.ifBlank { fallbackUserId },
        userId = resolvedUid,
        name = resolvedName,
        displayName = resolvedName,
        email = resolvedEmail,
        photoURL = resolvedPhoto,
        photoUrl = resolvedPhoto,
        upiId = snapshot.getString("upiId")?.takeIf { it.isNotBlank() } ?: defaultUpi,
        notificationsEnabled = snapshot.getBoolean("notificationsEnabled") ?: true,
        biometricSimEnabled = snapshot.getBoolean("biometricSimEnabled") ?: false,
        createdAt = createdAt,
        updatedAt = updatedAt,
      )
    }
  }
}

@IgnoreExtraProperties
data class DemoBalance(
  @DocumentId val id: String = "",
  val userId: String = "",
  val demoBalance: Double = 25000.0,
  val balance: Double = 25000.0,
  val currency: String = "INR",
  val isDemo: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  companion object {
    fun fromSnapshot(snapshot: DocumentSnapshot, fallbackUserId: String): DemoBalance {
      val resolvedBalance =
        (snapshot.get("demoBalance") as? Number)?.toDouble()
          ?: (snapshot.get("balance") as? Number)?.toDouble()
          ?: 25000.0
      val currency = snapshot.getString("currency") ?: "INR"
      val isDemo = snapshot.getBoolean("isDemo") ?: true
      val createdAt =
        snapshot.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      val updatedAt =
        snapshot.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      return DemoBalance(
        id = snapshot.id.ifBlank { fallbackUserId },
        userId = snapshot.getString("userId") ?: snapshot.id.ifBlank { fallbackUserId },
        demoBalance = resolvedBalance,
        balance = resolvedBalance,
        currency = currency,
        isDemo = isDemo,
        createdAt = createdAt,
        updatedAt = updatedAt,
      )
    }
  }
}

@IgnoreExtraProperties
data class SimulatedTransaction(
  @DocumentId val id: String = "",
  val uid: String = "",
  val userId: String = "",
  val recipientName: String = "",
  val recipientMobile: String = "+91 98765 43210",
  val recipientUPI: String = "",
  val recipientUpiId: String = "",
  val senderName: String = "Zenration Player",
  val senderUpiId: String = "player@zenpay.sim",
  val paymentStyle: String = PaymentStyle.PAYTM_INSPIRED.id,
  val amount: Double = 0.0,
  val currency: String = "INR",
  val status: String = "SIMULATED – NOT PAID",
  val isSimulated: Boolean = true,
  val note: String? = null,
  val referenceId: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
) {
  companion object {
    fun fromSnapshot(snapshot: DocumentSnapshot): SimulatedTransaction {
      val rawAmount = (snapshot.get("amount") as? Number)?.toDouble() ?: 0.0
      val resolvedUid =
        snapshot.getString("uid")?.takeIf { it.isNotBlank() }
          ?: snapshot.getString("userId").orEmpty()
      val resolvedRecipientUpi =
        snapshot.getString("recipientUPI")?.takeIf { it.isNotBlank() }
          ?: snapshot.getString("recipientUpiId")?.takeIf { it.isNotBlank() }
          ?: "unknown@upi"
      val resolvedMobile =
        snapshot.getString("recipientMobile")?.takeIf { it.isNotBlank() }
          ?: "+91 98765 43210"
      val resolvedSenderName =
        snapshot.getString("senderName")?.takeIf { it.isNotBlank() }
          ?: "Zenration Player"
      val resolvedSenderUpi =
        snapshot.getString("senderUpiId")?.takeIf { it.isNotBlank() }
          ?: "player@zenpay.sim"
      val resolvedStyle =
        snapshot.getString("paymentStyle")?.takeIf { it.isNotBlank() }
          ?: PaymentStyle.PAYTM_INSPIRED.id
      val createdAt =
        snapshot.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      return SimulatedTransaction(
        id = snapshot.id,
        uid = resolvedUid,
        userId = resolvedUid,
        recipientName = snapshot.getString("recipientName") ?: "Unknown Recipient",
        recipientMobile = resolvedMobile,
        recipientUPI = resolvedRecipientUpi,
        recipientUpiId = resolvedRecipientUpi,
        senderName = resolvedSenderName,
        senderUpiId = resolvedSenderUpi,
        paymentStyle = resolvedStyle,
        amount = rawAmount,
        currency = snapshot.getString("currency") ?: "INR",
        status = snapshot.getString("status") ?: "SIMULATED – NOT PAID",
        isSimulated = snapshot.getBoolean("isSimulated") ?: true,
        note = snapshot.getString("note"),
        referenceId = snapshot.getString("referenceId") ?: "",
        createdAt = createdAt,
      )
    }
  }
}

data class UpiQrPayload(
  val recipientUpiId: String,
  val recipientName: String,
  val recipientMobile: String = "+91 98765 43210",
  val amount: String = "",
  val note: String = "",
  val currency: String = "INR",
  val rawQrString: String = "",
)

object UpiQrParser {
  /**
   * Parses standard UPI QR strings (e.g. upi://pay?pa=merchant@okaxis&pn=Merchant%20Name&am=250.00&tn=Coffee&cu=INR),
   * 10-digit demo mobile numbers, or plain UPI handles (e.g. merchant@ybl).
   */
  fun parse(rawInput: String): UpiQrPayload? {
    val trimmed = rawInput.trim()
    if (trimmed.isBlank()) return null

    // Check if it matches a saved DemoContact first
    val matchingContact =
      DemoContactsCatalog.defaultContacts.firstOrNull {
        it.upiId.equals(trimmed, ignoreCase = true) ||
          it.username.equals(trimmed, ignoreCase = true) ||
          it.mobileNumber.replace(Regex("[^0-9]"), "") ==
            trimmed.replace(Regex("[^0-9]"), "").takeIf { d -> d.length >= 10 }
      }
    if (matchingContact != null) {
      return matchingContact.toUpiPayload()
    }

    if (
      trimmed.startsWith("upi://", ignoreCase = true) ||
        trimmed.contains("pa=", ignoreCase = true)
    ) {
      return try {
        val normalized =
          if (trimmed.startsWith("upi://", ignoreCase = true)) {
            trimmed
          } else {
            "upi://pay?${trimmed.substringAfter("?")}"
          }
        val uri = Uri.parse(normalized)
        val pa = uri.getQueryParameter("pa")?.trim().orEmpty()
        if (pa.isBlank() || !pa.contains("@")) return null
        val pn =
          uri.getQueryParameter("pn")?.trim()?.takeIf { it.isNotBlank() }
            ?: deriveDisplayNameFromUpi(pa)
        val am =
          uri.getQueryParameter("am")?.trim()?.takeIf {
            it.toDoubleOrNull() != null && it.toDouble() > 0
          } ?: ""
        val tn = uri.getQueryParameter("tn")?.trim().orEmpty()
        val cu = uri.getQueryParameter("cu")?.trim()?.takeIf { it.isNotBlank() } ?: "INR"
        val digits = pa.substringBefore("@").replace(Regex("[^0-9]"), "")
        val derivedMobile =
          if (digits.length == 10) {
            "+91 ${digits.substring(0, 5)} ${digits.substring(5)}"
          } else {
            "+91 98765 43210"
          }
        UpiQrPayload(
          recipientUpiId = pa.take(100),
          recipientName = pn.take(100),
          recipientMobile = derivedMobile,
          amount = am,
          note = tn.take(200),
          currency = cu.take(10),
          rawQrString = trimmed,
        )
      } catch (e: Exception) {
        null
      }
    }

    // Fallback: 10-digit mobile number
    val digitsOnly = trimmed.replace(Regex("[^0-9]"), "")
    if (digitsOnly.length in 10..12) {
      val tenDigits = digitsOnly.takeLast(10)
      val formattedMobile = "+91 ${tenDigits.substring(0, 5)} ${tenDigits.substring(5)}"
      return UpiQrPayload(
        recipientUpiId = "$tenDigits@zenpay.sim",
        recipientName = "Demo Contact ($tenDigits)",
        recipientMobile = formattedMobile,
        rawQrString = trimmed,
      )
    }

    // Fallback: Plain UPI ID handle (e.g., store@okicici)
    if (trimmed.contains("@") && !trimmed.contains(" ") && trimmed.length in 3..100) {
      return UpiQrPayload(
        recipientUpiId = trimmed,
        recipientName = deriveDisplayNameFromUpi(trimmed),
        recipientMobile = "+91 98765 43210",
        rawQrString = trimmed,
      )
    }

    return null
  }

  fun deriveDisplayNameFromUpi(upiId: String): String {
    val handle = upiId.substringBefore("@").replace(Regex("[._\\-]+"), " ").trim()
    if (handle.isBlank()) return "Verified UPI Merchant"
    return handle
      .split(" ")
      .filter { it.isNotBlank() }
      .joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercase() }
      }
      .take(100)
  }
}
