package dev.challenge.ledger.api;

import java.util.UUID;

public record AccountResponse(
        UUID id,
        long balance
) {
}