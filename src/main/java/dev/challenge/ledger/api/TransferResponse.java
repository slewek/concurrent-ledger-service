package dev.challenge.ledger.api;

import dev.challenge.ledger.domain.TransferStatus;

import java.util.UUID;

public record TransferResponse(
        UUID transferId,
        TransferStatus status,
        UUID fromAccountId,
        UUID toAccountId,
        long amount
) {
}