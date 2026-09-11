package dev.challenge.ledger.api;

import java.util.UUID;

public record TransferRequest(
        UUID fromAccountId,
        UUID toAccountId,
        long amount
) {
}