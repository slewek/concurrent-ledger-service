package dev.challenge.ledger.idempotency;

import dev.challenge.ledger.domain.TransferResult;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

public class InMemoryIdempotencyStore implements IdempotencyStore {

    private final ConcurrentMap<String, Entry> entries =
            new ConcurrentHashMap<>();

    @Override
    public TransferResult execute(
            String key,
            String requestFingerprint,
            Supplier<TransferResult> operation
    ) {
        Objects.requireNonNull(key, "idempotency key must not be null");

        if (key.isBlank()) {
            throw new IllegalArgumentException(
                    "idempotency key must not be blank"
            );
        }

        Objects.requireNonNull(
                requestFingerprint,
                "request fingerprint must not be null"
        );

        var newEntry = new Entry(
                requestFingerprint,
                new CompletableFuture<>()
        );

        var existingEntry = entries.putIfAbsent(key, newEntry);

        if (existingEntry != null) {
            if (!existingEntry.requestFingerprint()
                    .equals(requestFingerprint)) {
                throw new IllegalArgumentException(
                        "idempotency key was already used for a different request"
                );
            }

            return existingEntry.result().join();
        }

        try {
            var result = operation.get();
            newEntry.result().complete(result);
            return result;
        } catch (RuntimeException exception) {
            newEntry.result().completeExceptionally(exception);

            entries.remove(key, newEntry);

            throw exception;
        }
    }

    private record Entry(
            String requestFingerprint,
            CompletableFuture<TransferResult> result
    ) {
    }
}