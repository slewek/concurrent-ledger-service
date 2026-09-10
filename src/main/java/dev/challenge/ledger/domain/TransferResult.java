package dev.challenge.ledger.domain;

import java.util.UUID;

public record TransferResult(
        UUID transferId,
        TransferStatus status,
        UUID fromAccountId,
        UUID toAccountId,
        long amount
) {
}