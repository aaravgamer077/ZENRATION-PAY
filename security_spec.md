# ZENRATION PAY - Firestore Security Specification

## 1. Data Invariants
1. **Authentication Mandate**: Every read (`get`, `list`) and write (`create`, `update`) across `/users/{userId}`, `/balances/{userId}`, and `/transactions/{transactionId}` requires `request.auth != null`.
2. **Strict Multi-User Isolation**:
   - A user may only `get`, `list`, `create`, or `update` their own profile at `/users/{userId}` where `userId == request.auth.uid` and `data.userId == request.auth.uid`.
   - A user may only `get`, `list`, `create`, or `update` their own demo balance at `/balances/{userId}` where `userId == request.auth.uid` and `data.userId == request.auth.uid`.
   - A user may only `get`, `list`, or `create` simulated transactions at `/transactions/{transactionId}` where `data.userId == request.auth.uid`.
3. **Simulation Integrity Invariant**:
   - Every `/balances/{userId}` document must have `isDemo == true` and a non-negative numeric `balance` (`<= 1000000000`).
   - Every `/transactions/{transactionId}` document must have `status == 'SIMULATED'` and `isSimulated == true`, and is append-only (`allow update, delete: if false`).
4. **Temporal & Immutable Field Integrity**:
   - `createdAt` and `updatedAt` must be valid Firestore `timestamp` values `<= request.time`.
   - On `update` of `/users/{userId}` and `/balances/{userId}`, `userId` and `createdAt` are strictly immutable (`incoming().userId == existing().userId && incoming().createdAt == existing().createdAt`).

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Profile Read**: `unauthDb.collection("users").doc("alice").get()` -> REJECTED.
2. **Cross-User Profile Read (PII Leak)**: `aliceDb.collection("users").doc("bob").get()` -> REJECTED.
3. **Identity Spoofing on Profile Create**: `aliceDb.collection("users").doc("alice").set({ userId: "bob", ... })` -> REJECTED.
4. **Shadow Update / Ghost Field on Profile**: `aliceDb.collection("users").doc("alice").update({ isAdmin: true })` -> REJECTED by `hasOnly()`.
5. **Cross-User Balance Tampering**: `aliceDb.collection("balances").doc("bob").update({ balance: 999999 })` -> REJECTED.
6. **Negative or Non-Demo Balance Injection**: `aliceDb.collection("balances").doc("alice").set({ balance: -500, isDemo: false, ... })` -> REJECTED.
7. **Real Payment Status Spoofing**: `aliceDb.collection("transactions").doc("tx_1").set({ status: "PAID", isSimulated: false, ... })` -> REJECTED (must be `status == 'SIMULATED'` and `isSimulated == true`).
8. **Zero or Negative Transaction Amount**: `aliceDb.collection("transactions").doc("tx_1").set({ amount: 0, ... })` -> REJECTED.
9. **Transaction Ledger Mutation (Update/Delete)**: `aliceDb.collection("transactions").doc("tx_1").update({ amount: 10 })` -> REJECTED (`allow update, delete: if false`).
10. **Unfiltered Transaction List Query**: `aliceDb.collection("transactions").get()` without `.where("userId", "==", "alice")` -> REJECTED.
11. **Oversized String / Resource Poisoning**: `recipientName` with 5,000 characters -> REJECTED by `.size() <= 100`.
12. **Future Timestamp Spoofing**: `createdAt` set to a timestamp in the future (`> request.time`) -> REJECTED.
