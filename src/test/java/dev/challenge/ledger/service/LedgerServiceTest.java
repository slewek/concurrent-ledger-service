package dev.challenge.ledger.service;

import dev.challenge.ledger.concurrency.AccountLockManager;
import dev.challenge.ledger.domain.EntryType;
import dev.challenge.ledger.domain.TransferStatus;
import dev.challenge.ledger.idempotency.InMemoryIdempotencyStore;
import dev.challenge.ledger.storage.InMemoryAccountStore;
import dev.challenge.ledger.storage.InMemoryLedgerEntryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LedgerServiceTest {

    private InMemoryAccountStore accountStore;
    private InMemoryLedgerEntryStore ledgerEntryStore;
    private LedgerService ledgerService;

    @BeforeEach
    void setUp() {
        accountStore = new InMemoryAccountStore();
        ledgerEntryStore = new InMemoryLedgerEntryStore();

        ledgerService = new LedgerService(
                accountStore,
                ledgerEntryStore,
                new AccountLockManager(),
                new InMemoryIdempotencyStore()
        );
    }

    @Test
    void shouldTransferMoneyBetweenAccounts() {
        var source = accountStore.create(10_000);
        var destination = accountStore.create(2_000);

        var result = ledgerService.transfer(
                UUID.randomUUID().toString(),
                source.id(),
                destination.id(),
                3_000
        );

        assertEquals(
                TransferStatus.SUCCESS,
                result.status()
        );

        assertEquals(
                7_000,
                accountStore.findById(source.id())
                        .orElseThrow()
                        .balance()
        );

        assertEquals(
                5_000,
                accountStore.findById(destination.id())
                        .orElseThrow()
                        .balance()
        );

        var entries = ledgerEntryStore.findByTransferId(
                result.transferId()
        );

        assertEquals(2, entries.size());

        assertTrue(entries.stream().anyMatch(entry ->
                entry.accountId().equals(source.id())
                        && entry.type() == EntryType.DEBIT
                        && entry.amount() == 3_000
        ));

        assertTrue(entries.stream().anyMatch(entry ->
                entry.accountId().equals(destination.id())
                        && entry.type() == EntryType.CREDIT
                        && entry.amount() == 3_000
        ));
    }

    @Test
    void shouldNotChangeBalancesWhenFundsAreInsufficient() {
        var source = accountStore.create(1_000);
        var destination = accountStore.create(2_000);

        var result = ledgerService.transfer(
                UUID.randomUUID().toString(),
                source.id(),
                destination.id(),
                1_500
        );

        assertEquals(
                TransferStatus.INSUFFICIENT_FUNDS,
                result.status()
        );

        assertEquals(
                1_000,
                accountStore.findById(source.id())
                        .orElseThrow()
                        .balance()
        );

        assertEquals(
                2_000,
                accountStore.findById(destination.id())
                        .orElseThrow()
                        .balance()
        );

        assertTrue(
                ledgerEntryStore
                        .findByTransferId(result.transferId())
                        .isEmpty()
        );
    }

    @Test
    void shouldPreventDoubleSpendingUnderConcurrentLoad()
            throws Exception {

        var source = accountStore.create(100);
        var destination = accountStore.create(0);

        int numberOfTransfers = 1_000;

        var start = new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(32);

        try {
            List<Future<TransferStatus>> futures =
                    new ArrayList<>();

            for (int i = 0; i < numberOfTransfers; i++) {
                futures.add(executor.submit(() -> {
                    start.await();

                    return ledgerService.transfer(
                            UUID.randomUUID().toString(),
                            source.id(),
                            destination.id(),
                            1
                    ).status();
                }));
            }

            start.countDown();

            long successfulTransfers = 0;

            for (var future : futures) {
                if (future.get() == TransferStatus.SUCCESS) {
                    successfulTransfers++;
                }
            }

            assertEquals(
                    100,
                    successfulTransfers
            );

            assertEquals(
                    0,
                    accountStore.findById(source.id())
                            .orElseThrow()
                            .balance()
            );

            assertEquals(
                    100,
                    accountStore.findById(destination.id())
                            .orElseThrow()
                            .balance()
            );
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldNotDeadlockForOppositeDirectionTransfers() {
        var accountA = accountStore.create(10_000);
        var accountB = accountStore.create(10_000);

        assertTimeoutPreemptively(
                Duration.ofSeconds(5),
                () -> {
                    ExecutorService executor =
                            Executors.newFixedThreadPool(2);

                    try {
                        var start = new CountDownLatch(1);

                        var first = executor.submit(() -> {
                            start.await();

                            for (int i = 0; i < 1_000; i++) {
                                ledgerService.transfer(
                                        UUID.randomUUID().toString(),
                                        accountA.id(),
                                        accountB.id(),
                                        1
                                );
                            }

                            return null;
                        });

                        var second = executor.submit(() -> {
                            start.await();

                            for (int i = 0; i < 1_000; i++) {
                                ledgerService.transfer(
                                        UUID.randomUUID().toString(),
                                        accountB.id(),
                                        accountA.id(),
                                        1
                                );
                            }

                            return null;
                        });

                        start.countDown();

                        first.get();
                        second.get();
                    } finally {
                        executor.shutdownNow();
                    }
                }
        );

        assertEquals(
                10_000,
                accountStore.findById(accountA.id())
                        .orElseThrow()
                        .balance()
        );

        assertEquals(
                10_000,
                accountStore.findById(accountB.id())
                        .orElseThrow()
                        .balance()
        );
    }

    @Test
    void shouldRejectInvalidTransfer() {
        var accountA = accountStore.create(1_000);
        var accountB = accountStore.create(1_000);

        var accountAId = accountA.id();
        var accountBId = accountB.id();

        assertThrows(
                IllegalArgumentException.class,
                () -> ledgerService.transfer(
                        "invalid-amount",
                        accountAId,
                        accountBId,
                        0
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ledgerService.transfer(
                        "same-account",
                        accountAId,
                        accountAId,
                        100
                )
        );
    }

    @Test
    void shouldApplyTransferOnlyOnceForRepeatedIdempotencyKey() {
        var source = accountStore.create(1_000);
        var destination = accountStore.create(0);

        String key = "transfer-123";

        var first = ledgerService.transfer(
                key,
                source.id(),
                destination.id(),
                300
        );

        var retry = ledgerService.transfer(
                key,
                source.id(),
                destination.id(),
                300
        );

        assertEquals(first, retry);

        assertEquals(
                700,
                accountStore.findById(source.id())
                        .orElseThrow()
                        .balance()
        );

        assertEquals(
                300,
                accountStore.findById(destination.id())
                        .orElseThrow()
                        .balance()
        );

        var entries = ledgerEntryStore.findByTransferId(
                first.transferId()
        );

        assertEquals(
                2,
                entries.size()
        );
    }

    @Test
    void shouldRejectSameIdempotencyKeyForDifferentTransfer() {
        var source = accountStore.create(1_000);
        var destination = accountStore.create(0);

        var sourceId = source.id();
        var destinationId = destination.id();

        String key = "transfer-123";

        ledgerService.transfer(
                key,
                sourceId,
                destinationId,
                100
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ledgerService.transfer(
                        key,
                        sourceId,
                        destinationId,
                        200
                )
        );

        assertEquals(
                900,
                accountStore.findById(sourceId)
                        .orElseThrow()
                        .balance()
        );

        assertEquals(
                100,
                accountStore.findById(destinationId)
                        .orElseThrow()
                        .balance()
        );
    }
}