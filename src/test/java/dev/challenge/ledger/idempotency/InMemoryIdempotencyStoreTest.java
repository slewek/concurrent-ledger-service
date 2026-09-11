package dev.challenge.ledger.idempotency;

import dev.challenge.ledger.domain.TransferResult;
import dev.challenge.ledger.domain.TransferStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryIdempotencyStoreTest {

    @Test
    void shouldExecuteOperationOnlyOnceWhenRetryArrivesWhileOriginalIsInFlight()
            throws Exception {

        var store = new InMemoryIdempotencyStore();

        var fromAccountId = UUID.randomUUID();
        var toAccountId = UUID.randomUUID();

        var expectedResult = new TransferResult(
                UUID.randomUUID(),
                TransferStatus.SUCCESS,
                fromAccountId,
                toAccountId,
                100
        );

        var operationStarted = new CountDownLatch(1);
        var allowOperationToFinish = new CountDownLatch(1);
        var secondRequestStarted = new CountDownLatch(1);

        var operationCalls = new AtomicInteger();

        Supplier<TransferResult> operation = () -> {
            operationCalls.incrementAndGet();
            operationStarted.countDown();

            try {
                allowOperationToFinish.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }

            return expectedResult;
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            var firstRequest = executor.submit(() ->
                    store.execute(
                            "transfer-123",
                            "request-123",
                            operation
                    )
            );

            assertTrue(
                    operationStarted.await(2, TimeUnit.SECONDS)
            );

            var retryRequest = executor.submit(() -> {
                secondRequestStarted.countDown();

                return store.execute(
                        "transfer-123",
                        "request-123",
                        operation
                );
            });

            assertTrue(
                    secondRequestStarted.await(2, TimeUnit.SECONDS)
            );

            assertFalse(retryRequest.isDone());

            allowOperationToFinish.countDown();

            var firstResult = firstRequest.get(
                    2,
                    TimeUnit.SECONDS
            );

            var retryResult = retryRequest.get(
                    2,
                    TimeUnit.SECONDS
            );

            assertEquals(expectedResult, firstResult);
            assertEquals(expectedResult, retryResult);

            assertEquals(
                    1,
                    operationCalls.get()
            );
        } finally {
            executor.shutdownNow();
        }
    }
}