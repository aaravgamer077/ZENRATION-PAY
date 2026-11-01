package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZenrationRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun test1To5_ensureOrUpdateUserProfile_andDemoBalance_matchesSharedWebsiteSchema() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = ZenrationRepository(firestore, auth)

    val profileResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.ensureOrUpdateUserProfile(
          displayName = "Alice Sharma",
          email = ALICE_EMAIL,
          photoUrl = "https://example.com/alice.png",
        )
      }
    assertTrue(
      "Expected profile creation to succeed: ${profileResult.exceptionOrNull()}",
      profileResult.isSuccess,
    )
    val profile = profileResult.getOrThrow()
    assertEquals(uid, profile.userId)
    assertEquals("Alice Sharma", profile.name)
    assertEquals("Alice Sharma", profile.displayName)
    assertEquals(ALICE_EMAIL, profile.email)
    assertEquals("https://example.com/alice.png", profile.photoURL)

    withTimeout(DEFAULT_TIMEOUT_MS) { repository.ensureDemoBalanceInitialized(25000.0).getOrThrow() }
    withTimeout(DEFAULT_TIMEOUT_MS) { repository.updateDemoBalance(25000.0).getOrThrow() }

    val balanceResult =
      withTimeout(DEFAULT_TIMEOUT_MS) { repository.getDemoBalanceById(uid) }
    assertTrue(
      "Expected demo balance fetch to succeed: ${balanceResult.exceptionOrNull()}",
      balanceResult.isSuccess,
    )
    val balance = checkNotNull(balanceResult.getOrThrow())
    assertEquals(uid, balance.userId)
    assertEquals(true, balance.isDemo)
    assertEquals(25000.0, balance.demoBalance, 0.01)
    assertEquals(25000.0, balance.balance, 0.01)
  }

  @Test
  fun test6To7_realTimeBalanceSync_whenWebsiteUpdatesDemoBalanceOnly_androidObserverReflectsImmediately() =
    runBlocking {
      val uid = signInTestUser(ALICE_EMAIL)
      val repository = ZenrationRepository(firestore, auth)

      withTimeout(DEFAULT_TIMEOUT_MS) { repository.ensureDemoBalanceInitialized(25000.0).getOrThrow() }
      withTimeout(DEFAULT_TIMEOUT_MS) { repository.updateDemoBalance(25000.0).getOrThrow() }

      // Simulate the ZENRATION PAY Website updating ONLY `demoBalance` and `updatedAt` in Firestore
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore
          .collection("balances")
          .document(uid)
          .update(
            mapOf(
              "demoBalance" to 82500.0,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }

      // Verify Android real-time Flow listener receives the updated demoBalance without app restart
      val observedBalance =
        withTimeout(FLOW_TIMEOUT_MS) {
          repository.observeDemoBalance(uid).first { it != null && it.demoBalance == 82500.0 }
        }
      assertNotNull(observedBalance)
      assertEquals(82500.0, observedBalance!!.demoBalance, 0.01)
      assertEquals(82500.0, observedBalance.balance, 0.01)

      // Verify Custom Your Balance Protocol update from Android saves to Firestore and syncs
      withTimeout(DEFAULT_TIMEOUT_MS) { repository.updateDemoBalance(125000.0).getOrThrow() }
      val customSyncedBalance =
        withTimeout(FLOW_TIMEOUT_MS) {
          repository.observeDemoBalance(uid).first { it != null && it.demoBalance == 125000.0 }
        }
      assertNotNull(customSyncedBalance)
      assertEquals(125000.0, customSyncedBalance!!.demoBalance, 0.01)
    }

  @Test
  fun test8To9_createSimulatedTransaction_writesSharedWebsiteFieldsAndUpdatesDemoBalance() =
    runBlocking {
      val uid = signInTestUser(ALICE_EMAIL)
      val repository = ZenrationRepository(firestore, auth)

      withTimeout(DEFAULT_TIMEOUT_MS) { repository.ensureDemoBalanceInitialized(25000.0).getOrThrow() }
      withTimeout(DEFAULT_TIMEOUT_MS) { repository.updateDemoBalance(25000.0).getOrThrow() }

      val txResult =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          repository.createSimulatedTransaction(
            recipientName = "Zen Coffee Lab",
            recipientUpiId = "zencoffee@okaxis",
            amount = 450.0,
            note = "Cold brew simulation",
            deductFromDemoBalance = true,
          )
        }
      assertTrue(
        "Expected simulated transaction to succeed: ${txResult.exceptionOrNull()}",
        txResult.isSuccess,
      )
      val tx = txResult.getOrThrow()
      assertEquals("SIMULATED – NOT PAID", tx.status)
      assertEquals(true, tx.isSimulated)
      assertEquals(uid, tx.uid)
      assertEquals(uid, tx.userId)
      assertEquals("zencoffee@okaxis", tx.recipientUPI)
      assertEquals("zencoffee@okaxis", tx.recipientUpiId)

      val emittedList =
        withTimeout(FLOW_TIMEOUT_MS) {
          repository.observeTransactions(uid).first { list -> list.any { it.id == tx.id } }
        }
      assertTrue(
        emittedList.any {
          it.id == tx.id &&
            it.uid == uid &&
            it.recipientUPI == "zencoffee@okaxis" &&
            it.status == "SIMULATED – NOT PAID"
        }
      )

      val updatedBalance =
        withTimeout(DEFAULT_TIMEOUT_MS) { repository.getDemoBalanceById(uid).getOrThrow() }
      assertNotNull(updatedBalance)
      assertEquals(24550.0, updatedBalance!!.demoBalance, 0.01)
      assertEquals(24550.0, updatedBalance.balance, 0.01)
    }

  @Test
  fun test10_crossUserAccess_differentGoogleAccountCannotReadAnotherUsersProfileBalanceOrTransaction() =
    runBlocking {
      val aliceUid = signInTestUser(ALICE_EMAIL)
      val aliceRepo = ZenrationRepository(firestore, auth)
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo.ensureOrUpdateUserProfile("Alice Sharma", ALICE_EMAIL).getOrThrow()
        aliceRepo.ensureDemoBalanceInitialized(25000.0).getOrThrow()
      }
      val aliceTx =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          aliceRepo
            .createSimulatedTransaction(
              recipientName = "Metro Mart",
              recipientUpiId = "metromart@ybl",
              amount = 200.0,
              note = "Groceries",
            )
            .getOrThrow()
        }

      // Switch to Bob (Different Google account -> different UID)
      signInTestUser(BOB_EMAIL)
      val bobRepo = ZenrationRepository(firestore, auth)

      val profileResult =
        withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getUserProfileById(aliceUid) }
      assertTrue(profileResult.isFailure)
      val profileEx = profileResult.exceptionOrNull()?.unwrapFirestoreException()
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, profileEx?.code)

      val balanceResult =
        withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getDemoBalanceById(aliceUid) }
      assertTrue(balanceResult.isFailure)
      val balanceEx = balanceResult.exceptionOrNull()?.unwrapFirestoreException()
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, balanceEx?.code)

      val txResult =
        withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getTransactionById(aliceTx.id) }
      assertTrue(txResult.isFailure)
      val txEx = txResult.exceptionOrNull()?.unwrapFirestoreException()
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, txEx?.code)
    }

  @Test
  fun unauthenticatedUser_observingTransactions_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    auth.signOut()
    val repository = ZenrationRepository(firestore, auth)

    var caught: Throwable? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { repository.observeTransactions(aliceUid).first() }
    } catch (e: Throwable) {
      caught = e
    }
    assertNotNull("Expected exception when unauthenticated", caught)
    val firestoreException =
      checkNotNull(caught?.unwrapFirestoreException()) {
        "Expected nested FirebaseFirestoreException, got $caught"
      }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
  }

  private companion object {
    const val ALICE_EMAIL = "alice@zenrationpay.test"
    const val BOB_EMAIL = "bob@zenrationpay.test"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
