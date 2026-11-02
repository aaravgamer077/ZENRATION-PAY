package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
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
import java.util.Date
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class ZenrationRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
  private val appContext: Context? = null,
) {
  // Convenience secondary constructor resolving the provisioned custom Firestore database ID
  constructor(
    context: Context
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth,
    context.applicationContext,
  )

  private val prefs: SharedPreferences? =
    appContext?.applicationContext?.getSharedPreferences(LOCAL_PREFS_NAME, Context.MODE_PRIVATE)

  private val localProfileFlow = MutableStateFlow<UserProfile?>(loadLocalProfile())
  private val localBalanceFlow = MutableStateFlow<DemoBalance?>(loadLocalBalance())
  private val localTransactionsFlow =
    MutableStateFlow<List<SimulatedTransaction>>(loadLocalTransactions())

  private fun isCloudSignedIn(): Boolean = auth.currentUser != null

  private fun activeLocalUid(fallback: String = DEFAULT_LOCAL_UID): String {
    return auth.currentUser?.uid ?: fallback.ifBlank { DEFAULT_LOCAL_UID }
  }

  private fun loadLocalProfile(): UserProfile {
    val uid = DEFAULT_LOCAL_UID
    val savedName = prefs?.getString(KEY_PROFILE_NAME, null)?.takeIf { it.isNotBlank() } ?: "Zenration User"
    val savedEmail =
      prefs?.getString(KEY_PROFILE_EMAIL, null)?.takeIf { it.isNotBlank() } ?: "user@zenrationpay.sim"
    val savedUpi =
      prefs?.getString(KEY_PROFILE_UPI, null)?.takeIf { it.isNotBlank() } ?: "zenration@zenpay.sim"
    val notif = prefs?.getBoolean(KEY_PROFILE_NOTIF, true) ?: true
    val bio = prefs?.getBoolean(KEY_PROFILE_BIO, false) ?: false
    val now = Timestamp.now()
    return UserProfile(
      id = uid,
      userId = uid,
      name = savedName,
      displayName = savedName,
      email = savedEmail,
      upiId = savedUpi,
      notificationsEnabled = notif,
      biometricSimEnabled = bio,
      createdAt = now,
      updatedAt = now,
    )
  }

  private fun saveLocalProfile(profile: UserProfile) {
    prefs
      ?.edit()
      ?.putString(KEY_PROFILE_NAME, profile.name.ifBlank { profile.displayName })
      ?.putString(KEY_PROFILE_EMAIL, profile.email)
      ?.putString(KEY_PROFILE_UPI, profile.upiId ?: "zenration@zenpay.sim")
      ?.putBoolean(KEY_PROFILE_NOTIF, profile.notificationsEnabled)
      ?.putBoolean(KEY_PROFILE_BIO, profile.biometricSimEnabled)
      ?.apply()
    localProfileFlow.value = profile
  }

  private fun loadLocalBalance(): DemoBalance {
    val uid = DEFAULT_LOCAL_UID
    val rawBits = prefs?.getLong(KEY_DEMO_BALANCE_BITS, java.lang.Double.doubleToRawLongBits(DEFAULT_DEMO_BALANCE))
    val amount =
      if (rawBits != null) {
        java.lang.Double.longBitsToDouble(rawBits).coerceIn(0.0, 1_000_000_000.0)
      } else {
        DEFAULT_DEMO_BALANCE
      }
    val now = Timestamp.now()
    return DemoBalance(
      id = uid,
      userId = uid,
      demoBalance = amount,
      balance = amount,
      currency = "INR",
      isDemo = true,
      createdAt = now,
      updatedAt = now,
    )
  }

  private fun saveLocalBalance(amount: Double): DemoBalance {
    val safeBalance = amount.coerceIn(0.0, 1_000_000_000.0)
    prefs
      ?.edit()
      ?.putLong(KEY_DEMO_BALANCE_BITS, java.lang.Double.doubleToRawLongBits(safeBalance))
      ?.apply()
    val updated =
      DemoBalance(
        id = activeLocalUid(),
        userId = activeLocalUid(),
        demoBalance = safeBalance,
        balance = safeBalance,
        currency = "INR",
        isDemo = true,
        createdAt = localBalanceFlow.value?.createdAt ?: Timestamp.now(),
        updatedAt = Timestamp.now(),
      )
    localBalanceFlow.value = updated
    return updated
  }

  private fun loadLocalTransactions(): List<SimulatedTransaction> {
    val rawJson = prefs?.getString(KEY_TRANSACTIONS_JSON, null) ?: return emptyList()
    return try {
      val array = JSONArray(rawJson)
      val list = mutableListOf<SimulatedTransaction>()
      for (i in 0 until array.length()) {
        val obj = array.optJSONObject(i) ?: continue
        val epochMs = obj.optLong("createdAtMs", System.currentTimeMillis())
        val recipientUpi = obj.optString("recipientUpiId", "merchant@upi")
        val uid = obj.optString("uid", DEFAULT_LOCAL_UID)
        list.add(
          SimulatedTransaction(
            id = obj.optString("id", "tx_$i"),
            uid = uid,
            userId = uid,
            recipientName = obj.optString("recipientName", "Verified Merchant"),
            recipientUPI = recipientUpi,
            recipientUpiId = recipientUpi,
            amount = obj.optDouble("amount", 0.0),
            currency = obj.optString("currency", "INR"),
            status = obj.optString("status", SHARED_SIMULATED_STATUS),
            isSimulated = true,
            note = obj.optString("note", "").takeIf { it.isNotBlank() },
            referenceId = obj.optString("referenceId", "SIM-ZEN-LOCAL"),
            createdAt = Timestamp(Date(epochMs)),
          )
        )
      }
      list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
    } catch (_: Exception) {
      emptyList()
    }
  }

  private fun saveLocalTransactions(list: List<SimulatedTransaction>) {
    val sorted = list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
    localTransactionsFlow.value = sorted
    try {
      val array = JSONArray()
      sorted.take(200).forEach { tx ->
        val obj =
          JSONObject().apply {
            put("id", tx.id)
            put("uid", tx.uid.ifBlank { tx.userId })
            put("recipientName", tx.recipientName)
            put("recipientUpiId", tx.recipientUpiId.ifBlank { tx.recipientUPI })
            put("amount", tx.amount)
            put("currency", tx.currency)
            put("status", tx.status)
            put("note", tx.note ?: "")
            put("referenceId", tx.referenceId)
            put("createdAtMs", (tx.createdAt?.toDate()?.time ?: System.currentTimeMillis()))
          }
        array.put(obj)
      }
      prefs?.edit()?.putString(KEY_TRANSACTIONS_JSON, array.toString())?.apply()
    } catch (_: Exception) {
      // Ignore JSON serialization errors in unit tests without Android JSON
    }
  }

  private fun requireUserId(): String {
    return auth.currentUser?.uid ?: DEFAULT_LOCAL_UID
  }

  // --- 1. USER PROFILE OPERATIONS (/users/{uid}) ---

  fun observeUserProfile(userId: String): Flow<UserProfile?> {
    if (!isCloudSignedIn()) {
      return localProfileFlow.asStateFlow()
    }
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
    val safeName = displayName.trim().ifBlank { "Zenration User" }.take(100)
    val safeEmail = email.trim().ifBlank { "user@zenrationpay.sim" }.take(150)
    val safePhoto = (photoUrl?.trim() ?: "").take(500)
    val defaultUpi =
      "${safeEmail.substringBefore("@").lowercase().replace(Regex("[^a-z0-9]"), "").ifBlank { "user" }}@zenpay.sim"
        .take(100)

    if (!isCloudSignedIn()) {
      val current = localProfileFlow.value ?: loadLocalProfile()
      val updated =
        current.copy(
          id = uid,
          userId = uid,
          name = current.name.ifBlank { safeName },
          displayName = current.displayName.ifBlank { safeName },
          email = current.email.ifBlank { safeEmail },
          upiId = current.upiId?.takeIf { it.isNotBlank() } ?: defaultUpi,
          updatedAt = Timestamp.now(),
        )
      saveLocalProfile(updated)
      return@runCatching updated
    }

    val docRef = db.collection("users").document(uid)
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
    if (!isCloudSignedIn()) {
      return@runCatching localProfileFlow.value ?: loadLocalProfile()
    }
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
    val safeName = displayName.trim().ifBlank { "Zenration User" }.take(100)
    val safeUpi = upiId.trim().ifBlank { "user@zenpay.sim" }.take(100)

    if (!isCloudSignedIn()) {
      val current = localProfileFlow.value ?: loadLocalProfile()
      val updated =
        current.copy(
          name = safeName,
          displayName = safeName,
          upiId = safeUpi,
          notificationsEnabled = notificationsEnabled,
          biometricSimEnabled = biometricSimEnabled,
          updatedAt = Timestamp.now(),
        )
      saveLocalProfile(updated)
      return@runCatching
    }

    val docRef = db.collection("users").document(uid)
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
    if (!isCloudSignedIn()) {
      return localBalanceFlow.asStateFlow()
    }
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
    val safeBalance = initialBalance.coerceIn(0.0, 1000000000.0)

    if (!isCloudSignedIn()) {
      val existing = localBalanceFlow.value
      return@runCatching if (existing != null && prefs?.contains(KEY_DEMO_BALANCE_BITS) == true) {
        existing
      } else {
        saveLocalBalance(safeBalance)
      }
    }

    val docRef = db.collection("balances").document(uid)
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
    if (!isCloudSignedIn()) {
      return@runCatching localBalanceFlow.value ?: loadLocalBalance()
    }
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
    val safeBalance = newBalance.coerceIn(0.0, 1000000000.0)

    if (!isCloudSignedIn()) {
      saveLocalBalance(safeBalance)
      return@runCatching
    }

    val docRef = db.collection("balances").document(uid)
    try {
      val existing = docRef.get().await()
      if (existing.exists()) {
        docRef
          .update(
            mapOf(
              "demoBalance" to safeBalance,
              "balance" to safeBalance,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        docRef
          .set(
            mapOf(
              "userId" to uid,
              "demoBalance" to safeBalance,
              "balance" to safeBalance,
              "currency" to "INR",
              "isDemo" to true,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  // --- 3. SIMULATED TRANSACTIONS OPERATIONS (/transactions/{transactionId}) ---

  fun observeTransactions(userId: String): Flow<List<SimulatedTransaction>> {
    if (!isCloudSignedIn()) {
      return localTransactionsFlow.asStateFlow()
    }
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
    if (!isCloudSignedIn()) {
      return@runCatching localTransactionsFlow.value
    }
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
      if (!isCloudSignedIn()) {
        return@runCatching localTransactionsFlow.value.firstOrNull { it.id == transactionId }
          ?: throw NoSuchElementException("Simulated transaction not found: $transactionId")
      }
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
    val trimmedNote = note?.trim()?.take(200)?.takeIf { it.isNotBlank() }

    if (!isCloudSignedIn()) {
      val newTx =
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
      if (deductFromDemoBalance) {
        val current =
          localBalanceFlow.value?.demoBalance
            ?: localBalanceFlow.value?.balance
            ?: DEFAULT_DEMO_BALANCE
        val updated = (current - amount).coerceAtLeast(0.0)
        saveLocalBalance(updated)
      }
      val updatedList = listOf(newTx) + localTransactionsFlow.value
      saveLocalTransactions(updatedList)
      return@runCatching newTx
    }

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
    if (!trimmedNote.isNullOrBlank()) {
      payload["note"] = trimmedNote
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
    const val DEFAULT_LOCAL_UID = "zenration_local_user"
    private const val LOCAL_PREFS_NAME = "zenration_local_sandbox_prefs"
    private const val KEY_PROFILE_NAME = "profile_name"
    private const val KEY_PROFILE_EMAIL = "profile_email"
    private const val KEY_PROFILE_UPI = "profile_upi"
    private const val KEY_PROFILE_NOTIF = "profile_notif"
    private const val KEY_PROFILE_BIO = "profile_bio"
    private const val KEY_DEMO_BALANCE_BITS = "demo_balance_bits"
    private const val KEY_TRANSACTIONS_JSON = "transactions_json"
  }
}
