package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.DemoBalance
import com.example.data.model.SimulatedTransaction
import com.example.data.model.UserProfile
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ZenrationRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  // Convenience secondary constructor resolving the provisioned custom Firestore database ID
  constructor(
    context: Context
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth,
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException(
        "User must be signed in with Google before accessing Firestore."
      )
  }

  // --- 1. USER PROFILE OPERATIONS (/users/{uid}) ---

  fun observeUserProfile(userId: String): Flow<UserProfile?> {
    val path = "users/$userId"
    return db
      .collection("users")
      .document(userId)
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          UserProfile.fromSnapshot(snapshot, userId)
        } else {
          null
        }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
        throw error
      }
  }

  suspend fun ensureOrUpdateUserProfile(
    displayName: String,
    email: String,
    photoUrl: String? = null,
  ): Result<UserProfile> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    val safeName = displayName.trim().ifBlank { "Zenration User" }.take(100)
    val safeEmail = email.trim().ifBlank { "user@zenrationpay.sim" }.take(150)
    val safePhoto = (photoUrl?.trim() ?: "").take(500)
    val defaultUpi =
      "${safeEmail.substringBefore("@").lowercase().replace(Regex("[^a-z0-9]"), "").ifBlank { "user" }}@zenpay.sim"
        .take(100)

    try {
      val existingSnap = docRef.get().await()
      if (!existingSnap.exists()) {
        val createPayload =
          mutableMapOf<String, Any>(
            "userId" to uid,
            "name" to safeName,
            "displayName" to safeName,
            "email" to safeEmail,
            "photoURL" to safePhoto,
            "upiId" to defaultUpi,
            "notificationsEnabled" to true,
            "biometricSimEnabled" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        if (safePhoto.isNotBlank()) {
          createPayload["photoUrl"] = safePhoto
        }
        docRef.set(createPayload).await()
      } else {
        val updatePayload =
          mutableMapOf<String, Any>(
            "name" to safeName,
            "displayName" to safeName,
            "email" to safeEmail,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        if (safePhoto.isNotBlank()) {
          updatePayload["photoURL"] = safePhoto
          updatePayload["photoUrl"] = safePhoto
        }
        docRef.update(updatePayload).await()
      }
      val updatedSnap = docRef.get().await()
      if (updatedSnap.exists()) {
        UserProfile.fromSnapshot(updatedSnap, uid)
      } else {
        UserProfile(
          id = uid,
          userId = uid,
          name = safeName,
          displayName = safeName,
          email = safeEmail,
          photoURL = safePhoto.ifBlank { null },
          photoUrl = safePhoto.ifBlank { null },
          upiId = defaultUpi,
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  suspend fun getUserProfileById(userId: String): Result<UserProfile?> = runCatching {
    val docRef = db.collection("users").document(userId)
    try {
      val snap = docRef.get().await()
      if (snap.exists()) {
        UserProfile.fromSnapshot(snap, userId)
      } else {
        null
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      throw e
    }
  }

  suspend fun updateUserSettings(
    displayName: String,
    upiId: String,
    notificationsEnabled: Boolean,
    biometricSimEnabled: Boolean,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    val safeName = displayName.trim().ifBlank { "Zenration User" }.take(100)
    val safeUpi = upiId.trim().ifBlank { "user@zenpay.sim" }.take(100)
    try {
      val updatePayload =
        mapOf(
          "name" to safeName,
          "displayName" to safeName,
          "upiId" to safeUpi,
          "notificationsEnabled" to notificationsEnabled,
          "biometricSimEnabled" to biometricSimEnabled,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      docRef.update(updatePayload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  // --- 2. DEMO BALANCE OPERATIONS (/balances/{uid}) ---

  fun observeDemoBalance(userId: String): Flow<DemoBalance?> {
    val path = "balances/$userId"
    return db
      .collection("balances")
      .document(userId)
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          DemoBalance.fromSnapshot(snapshot, userId)
        } else {
          null
        }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
        throw error
      }
  }

  suspend fun ensureDemoBalanceInitialized(
    initialBalance: Double = DEFAULT_DEMO_BALANCE
  ): Result<DemoBalance> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("balances").document(uid)
    val safeBalance = initialBalance.coerceIn(0.0, 1000000000.0)
    try {
      val snap = docRef.get().await()
      if (!snap.exists()) {
        val payload =
          mapOf(
            "userId" to uid,
            "demoBalance" to safeBalance,
            "balance" to safeBalance,
            "currency" to "INR",
            "isDemo" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(payload).await()
        val created = docRef.get().await()
        DemoBalance.fromSnapshot(created, uid)
      } else {
        DemoBalance.fromSnapshot(snap, uid)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  suspend fun getDemoBalanceById(userId: String): Result<DemoBalance?> = runCatching {
    val docRef = db.collection("balances").document(userId)
    try {
      val snap = docRef.get().await()
      if (snap.exists()) {
        DemoBalance.fromSnapshot(snap, userId)
      } else {
        null
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      throw e
    }
  }

  suspend fun updateDemoBalance(newBalance: Double): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("balances").document(uid)
    val safeBalance = newBalance.coerceIn(0.0, 1000000000.0)
    try {
      docRef
        .update(
          mapOf(
            "demoBalance" to safeBalance,
            "balance" to safeBalance,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  // --- 3. SIMULATED TRANSACTIONS OPERATIONS (/transactions/{transactionId}) ---

  fun observeTransactions(userId: String): Flow<List<SimulatedTransaction>> {
    val path = "transactions"
    return db
      .collection("transactions")
      .whereEqualTo("uid", userId)
      .snapshots()
      .map { querySnapshot ->
        querySnapshot.documents
          .map { doc -> SimulatedTransaction.fromSnapshot(doc) }
          .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  suspend fun getUserTransactions(): Result<List<SimulatedTransaction>> = runCatching {
    val uid = requireUserId()
    val path = "transactions"
    try {
      val querySnapshot =
        db.collection("transactions").whereEqualTo("uid", uid).get().await()
      querySnapshot.documents
        .map { doc -> SimulatedTransaction.fromSnapshot(doc) }
        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, path)
      throw e
    }
  }

  suspend fun getTransactionById(transactionId: String): Result<SimulatedTransaction> =
    runCatching {
      val docRef = db.collection("transactions").document(transactionId)
      try {
        val snap = docRef.get().await()
        if (!snap.exists()) {
          throw NoSuchElementException("Simulated transaction not found: $transactionId")
        }
        SimulatedTransaction.fromSnapshot(snap)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.GET, docRef.path)
        throw e
      }
    }

  suspend fun createSimulatedTransaction(
    recipientName: String,
    recipientUpiId: String,
    amount: Double,
    note: String? = null,
    deductFromDemoBalance: Boolean = true,
  ): Result<SimulatedTransaction> = runCatching {
    val uid = requireUserId()
    require(amount > 0.0 && amount <= 10000000.0) {
      "Amount must be between 0.01 and 10,000,000"
    }
    val safeRecipientName = recipientName.trim().ifBlank { "Verified Merchant" }.take(100)
    val safeRecipientUpi = recipientUpiId.trim().ifBlank { "merchant@upi" }.take(100)
    val txId = "tx_${UUID.randomUUID().toString().replace("-", "")}"
    val referenceId =
      "SIM-ZEN-${UUID.randomUUID().toString().replace("-", "").uppercase().take(10)}"

    val docRef = db.collection("transactions").document(txId)
    val payload =
      mutableMapOf<String, Any>(
        "uid" to uid,
        "userId" to uid,
        "recipientName" to safeRecipientName,
        "recipientUPI" to safeRecipientUpi,
        "recipientUpiId" to safeRecipientUpi,
        "amount" to amount,
        "currency" to "INR",
        "status" to SHARED_SIMULATED_STATUS,
        "isSimulated" to true,
        "referenceId" to referenceId,
        "createdAt" to FieldValue.serverTimestamp(),
      )
    val trimmedNote = note?.trim()
    if (!trimmedNote.isNullOrBlank()) {
      payload["note"] = trimmedNote.take(200)
    }

    try {
      docRef.set(payload).await()
      if (deductFromDemoBalance) {
        val balanceRef = db.collection("balances").document(uid)
        val balanceSnap = balanceRef.get().await()
        if (balanceSnap.exists()) {
          val current =
            (balanceSnap.get("demoBalance") as? Number)?.toDouble()
              ?: (balanceSnap.get("balance") as? Number)?.toDouble()
              ?: DEFAULT_DEMO_BALANCE
          val updated = (current - amount).coerceAtLeast(0.0)
          balanceRef
            .update(
              mapOf(
                "demoBalance" to updated,
                "balance" to updated,
                "updatedAt" to FieldValue.serverTimestamp(),
              )
            )
            .await()
        }
      }
      val savedSnap = docRef.get().await()
      if (savedSnap.exists()) {
        SimulatedTransaction.fromSnapshot(savedSnap)
      } else {
        SimulatedTransaction(
          id = txId,
          uid = uid,
          userId = uid,
          recipientName = safeRecipientName,
          recipientUPI = safeRecipientUpi,
          recipientUpiId = safeRecipientUpi,
          amount = amount,
          currency = "INR",
          status = SHARED_SIMULATED_STATUS,
          isSimulated = true,
          note = trimmedNote,
          referenceId = referenceId,
          createdAt = Timestamp.now(),
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  companion object {
    const val DEFAULT_DEMO_BALANCE = 25000.0
    const val SHARED_SIMULATED_STATUS = "SIMULATED – NOT PAID"
  }
}
