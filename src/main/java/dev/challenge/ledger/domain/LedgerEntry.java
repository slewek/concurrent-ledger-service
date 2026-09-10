package dev.challenge.ledger.domain;

import java.util.Objects;
import java.util.UUID;

public record LedgerEntry(
        UUID transferId,
        UUID accountId,
        EntryType type,
        long amount
) {

    public LedgerEntry {
        Objects.requireNonNull(transferId);
        Objects.requireNonNull(accountId);
        Objects.requireNonNull(type);

        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}