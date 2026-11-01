package com.example.data.model

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

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
  val recipientUPI: String = "",
  val recipientUpiId: String = "",
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
      val createdAt =
        snapshot.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      return SimulatedTransaction(
        id = snapshot.id,
        uid = resolvedUid,
        userId = resolvedUid,
        recipientName = snapshot.getString("recipientName") ?: "Unknown Recipient",
        recipientUPI = resolvedRecipientUpi,
        recipientUpiId = resolvedRecipientUpi,
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
  val amount: String = "",
  val note: String = "",
  val currency: String = "INR",
  val rawQrString: String = "",
)

object UpiQrParser {
  /**
   * Parses standard UPI QR strings (e.g. upi://pay?pa=merchant@okaxis&pn=Merchant%20Name&am=250.00&tn=Coffee&cu=INR)
   * or plain UPI handles (e.g. merchant@ybl).
   */
  fun parse(rawInput: String): UpiQrPayload? {
    val trimmed = rawInput.trim()
    if (trimmed.isBlank()) return null

    if (trimmed.startsWith("upi://", ignoreCase = true) || trimmed.contains("pa=", ignoreCase = true)) {
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
        UpiQrPayload(
          recipientUpiId = pa.take(100),
          recipientName = pn.take(100),
          amount = am,
          note = tn.take(200),
          currency = cu.take(10),
          rawQrString = trimmed,
        )
      } catch (e: Exception) {
        null
      }
    }

    // Fallback: Plain UPI ID handle (e.g., store@okicici)
    if (trimmed.contains("@") && !trimmed.contains(" ") && trimmed.length in 3..100) {
      return UpiQrPayload(
        recipientUpiId = trimmed,
        recipientName = deriveDisplayNameFromUpi(trimmed),
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
