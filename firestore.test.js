const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- TEST 1 to 5: SAME GOOGLE ACCOUNT UID & SHARED PROFILE SYNC ---

test("TEST 1-5: User logs into Android & Website with same UID and shares users/{uid} profile", async () => {
  const androidDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const webDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  // Android creates user profile at users/{uid}
  await assertSucceeds(
    androidDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice Sharma",
      displayName: "Alice Sharma",
      email: "alice@gmail.com",
      photoURL: "https://example.com/alice.png",
      photoUrl: "https://example.com/alice.png",
      upiId: "alice@zenpay.sim",
      notificationsEnabled: true,
      biometricSimEnabled: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Website reads the exact same profile using the same Firebase UID
  const webProfileSnap = await assertSucceeds(
    webDb.collection("users").doc(ALICE_UID).get()
  );
  if (webProfileSnap.data().name !== "Alice Sharma") {
    throw new Error("Profile name mismatch between Android and Website");
  }
});

// --- TEST 6 & 7: REAL-TIME DEMO BALANCE SYNCHRONIZATION (WEBSITE ADMIN -> ANDROID) ---

test("TEST 6-7: Authorised Administrator updates demoBalance on Website and Android reads it", async () => {
  const now = new Date();
  // Seed admin record in /admins/{adminUid} and Alice's initial balance
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("admins").doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      email: "admin@zenrationpay.com",
      createdAt: now,
    });
    await context.firestore().collection("balances").doc(ALICE_UID).set({
      userId: ALICE_UID,
      demoBalance: 25000.0,
      balance: 25000.0,
      currency: "INR",
      isDemo: true,
      createdAt: now,
      updatedAt: now,
    });
  });

  // Website Admin updates Alice's demoBalance
  const adminWebDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  await assertSucceeds(
    adminWebDb.collection("balances").doc(ALICE_UID).update({
      demoBalance: 75000.0,
      balance: 75000.0,
      updatedAt: new Date(),
    })
  );

  // Android app (logged in as Alice) reads the updated demoBalance
  const aliceAndroidDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const balanceSnap = await assertSucceeds(
    aliceAndroidDb.collection("balances").doc(ALICE_UID).get()
  );
  if (balanceSnap.data().demoBalance !== 75000.0) {
    throw new Error("Updated demoBalance not reflected for Android user");
  }
});

// --- TEST 8 & 9: SIMULATED TRANSACTION SYNCHRONIZATION (ANDROID -> WEBSITE) ---

test("TEST 8-9: Simulated transaction created in Android app is readable on Website with SIMULATED – NOT PAID status", async () => {
  const aliceAndroidDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceWebDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceAndroidDb.collection("transactions").doc("tx_shared_001").set({
      uid: ALICE_UID,
      userId: ALICE_UID,
      amount: 750.0,
      recipientName: "Apex Coffee Roasters",
      recipientUPI: "apexcoffee@okaxis",
      recipientUpiId: "apexcoffee@okaxis",
      currency: "INR",
      status: "SIMULATED – NOT PAID",
      isSimulated: true,
      note: "Morning espresso",
      referenceId: "SIM-ZEN-10029384",
      createdAt: now,
    })
  );

  // Website queries transactions by canonical 'uid' field
  const webQuerySnap = await assertSucceeds(
    aliceWebDb.collection("transactions").where("uid", "==", ALICE_UID).get()
  );
  if (webQuerySnap.empty) {
    throw new Error("Expected simulated transaction to appear on Website");
  }
});

// --- TEST 10: ACCOUNT ISOLATION & SECURITY BOUNDARIES ---

test("TEST 10: Different Google account (Bob) is isolated and cannot read or modify Alice's profile, balance, or transactions", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice Sharma",
      email: "alice@gmail.com",
      createdAt: now,
    });
    await context.firestore().collection("balances").doc(ALICE_UID).set({
      userId: ALICE_UID,
      demoBalance: 25000.0,
      updatedAt: now,
    });
    await context.firestore().collection("transactions").doc("tx_alice_1").set({
      uid: ALICE_UID,
      amount: 500.0,
      recipientName: "Store",
      recipientUPI: "store@upi",
      status: "SIMULATED – NOT PAID",
      createdAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("balances").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("balances").doc(ALICE_UID).update({
      demoBalance: 999999.0,
      updatedAt: new Date(),
    })
  );
  await assertFails(bobDb.collection("transactions").doc("tx_alice_1").get());
  await assertFails(
    bobDb.collection("transactions").where("uid", "==", ALICE_UID).get()
  );
});

test("Security: Normal user cannot grant themselves admin permissions or spoof unverified admin email", async () => {
  // Spoof attempt: Bob claims admin email with email_verified = false
  const spoofDb = testEnv
    .authenticatedContext(BOB_UID, {
      email: "nankidevi6746@gmail.com",
      email_verified: false,
    })
    .firestore();

  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("balances").doc(ALICE_UID).set({
      userId: ALICE_UID,
      demoBalance: 25000.0,
      updatedAt: now,
    });
  });

  // Must fail because email_verified is false
  await assertFails(spoofDb.collection("balances").doc(ALICE_UID).get());
  // Normal user cannot write to /admins
  await assertFails(
    spoofDb.collection("admins").doc(BOB_UID).set({
      uid: BOB_UID,
      email: "bob@gmail.com",
    })
  );
});
