package dev.challenge.ledger.idempotency;

import dev.challenge.ledger.domain.TransferResult;

import java.util.function.Supplier;

public interface IdempotencyStore {

    TransferResult execute(
            String key,
            String requestFingerprint,
            Supplier<TransferResult> operation
    );
}