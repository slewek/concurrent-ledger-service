# Concurrent Ledger Service

Small in-memory double-entry ledger service implemented in Java 21 and Spring Boot.

## Run

```bash
./mvnw spring-boot:run
```

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Run tests:

```bash
./mvnw clean verify
```

## API

### Create account

```http
POST /accounts
Content-Type: application/json
```

```json
{
  "initialBalance": 10000
}
```

### Read balance

```http
GET /accounts/{accountId}/balance
```

### Transfer money

```http
POST /transfers
Idempotency-Key: transfer-123
Content-Type: application/json
```

```json
{
  "fromAccountId": "source-account-id",
  "toAccountId": "destination-account-id",
  "amount": 3000
}
```

Amounts are represented as `long` values in minor currency units.

## Guarantees

* Transfers are atomic with respect to concurrent transfers touching the same accounts.
* A balance never becomes negative.
* Successful transfers create one `DEBIT` and one matching `CREDIT` ledger entry.
* Locks are maintained per account, so transfers touching unrelated accounts can execute concurrently.
* Account locks are acquired in deterministic ID order to prevent deadlocks.
* Every transfer requires an idempotency key.
* Repeated requests with the same key and payload return the original result and apply the transfer at most once.
* A retry arriving while the original request is still in flight waits for the original result instead of executing the transfer again.
* Reusing an idempotency key for different transfer parameters is rejected.

## Design

The ledger core is plain Java and does not depend on Spring.

Spring Boot is used only for HTTP and dependency wiring.

Storage is accessed through interfaces:

```text
LedgerService
 ├── AccountStore
 ├── LedgerEntryStore
 ├── AccountLockManager
 └── IdempotencyStore
```

The current implementations keep all state in memory.

## Trade-offs

This implementation is designed for a single JVM instance.

Account locks and idempotency state are process-local, and all state is lost when the application stops.

For a production system I would use durable transactional storage and persist accounts, ledger entries, transfers and idempotency records in the same database transaction.

The account creation API accepts an initial balance because the challenge does not define a deposit operation. In a real ledger, funding would normally also be represented as a ledger transaction.

## Tests

The test suite covers:

* successful and rejected transfers,
* insufficient funds,
* concurrent transfers and double-spend prevention,
* opposite-direction transfers and deadlock prevention,
* idempotent retries,
* retries arriving while the original request is still in flight,
* basic HTTP API flow.
