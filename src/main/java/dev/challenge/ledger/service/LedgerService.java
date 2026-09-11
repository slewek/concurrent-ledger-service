package dev.challenge.ledger.service;

import dev.challenge.ledger.concurrency.AccountLockManager;
import dev.challenge.ledger.domain.Account;
import dev.challenge.ledger.domain.EntryType;
import dev.challenge.ledger.domain.LedgerEntry;
import dev.challenge.ledger.domain.TransferResult;
import dev.challenge.ledger.domain.TransferStatus;
import dev.challenge.ledger.idempotency.IdempotencyStore;
import dev.challenge.ledger.storage.AccountStore;
import dev.challenge.ledger.storage.LedgerEntryStore;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class LedgerService {

    private final AccountStore accountStore;
    private final LedgerEntryStore ledgerEntryStore;
    private final AccountLockManager lockManager;
    private final IdempotencyStore idempotencyStore;

    public LedgerService(
            AccountStore accountStore,
            LedgerEntryStore ledgerEntryStore,
            AccountLockManager lockManager,
            IdempotencyStore idempotencyStore
    ) {
        this.accountStore = accountStore;
        this.ledgerEntryStore = ledgerEntryStore;
        this.lockManager = lockManager;
        this.idempotencyStore = idempotencyStore;
    }

    public TransferResult transfer(
            String idempotencyKey,
            UUID fromAccountId,
            UUID toAccountId,
            long amount
    ) {
        validateTransfer(
                idempotencyKey,
                fromAccountId,
                toAccountId,
                amount
        );

        String requestFingerprint = createRequestFingerprint(
                fromAccountId,
                toAccountId,
                amount
        );

        return idempotencyStore.execute(
                idempotencyKey,
                requestFingerprint,
                () -> executeConcurrentTransfer(
                        fromAccountId,
                        toAccountId,
                        amount
                )
        );
    }

    private TransferResult executeConcurrentTransfer(
            UUID fromAccountId,
            UUID toAccountId,
            long amount
    ) {
        ReentrantLock firstLock;
        ReentrantLock secondLock;

        if (fromAccountId.compareTo(toAccountId) < 0) {
            firstLock = lockManager.lockFor(fromAccountId);
            secondLock = lockManager.lockFor(toAccountId);
        } else {
            firstLock = lockManager.lockFor(toAccountId);
            secondLock = lockManager.lockFor(fromAccountId);
        }

        firstLock.lock();

        try {
            secondLock.lock();

            try {
                return executeTransfer(
                        fromAccountId,
                        toAccountId,
                        amount
                );
            } finally {
                secondLock.unlock();
            }
        } finally {
            firstLock.unlock();
        }
    }

    private TransferResult executeTransfer(
            UUID fromAccountId,
            UUID toAccountId,
            long amount
    ) {
        Account fromAccount = accountStore.findById(fromAccountId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "source account not found"
                        )
                );

        Account toAccount = accountStore.findById(toAccountId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "destination account not found"
                        )
                );

        UUID transferId = UUID.randomUUID();

        if (fromAccount.balance() < amount) {
            return new TransferResult(
                    transferId,
                    TransferStatus.INSUFFICIENT_FUNDS,
                    fromAccountId,
                    toAccountId,
                    amount
            );
        }

        long newDestinationBalance = Math.addExact(
                toAccount.balance(),
                amount
        );

        Account updatedFrom = new Account(
                fromAccount.id(),
                fromAccount.balance() - amount
        );

        Account updatedTo = new Account(
                toAccount.id(),
                newDestinationBalance
        );

        LedgerEntry debit = new LedgerEntry(
                transferId,
                fromAccountId,
                EntryType.DEBIT,
                amount
        );

        LedgerEntry credit = new LedgerEntry(
                transferId,
                toAccountId,
                EntryType.CREDIT,
                amount
        );

        accountStore.update(updatedFrom);
        accountStore.update(updatedTo);

        ledgerEntryStore.appendAll(
                List.of(debit, credit)
        );

        return new TransferResult(
                transferId,
                TransferStatus.SUCCESS,
                fromAccountId,
                toAccountId,
                amount
        );
    }

    private String createRequestFingerprint(
            UUID fromAccountId,
            UUID toAccountId,
            long amount
    ) {
        return fromAccountId
                + ":"
                + toAccountId
                + ":"
                + amount;
    }

    private void validateTransfer(
            String idempotencyKey,
            UUID fromAccountId,
            UUID toAccountId,
            long amount
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "idempotency key must not be blank"
            );
        }

        if (fromAccountId == null || toAccountId == null) {
            throw new IllegalArgumentException(
                    "account id must not be null"
            );
        }

        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException(
                    "source and destination accounts must be different"
            );
        }

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "amount must be positive"
            );
        }
    }
}