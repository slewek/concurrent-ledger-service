package dev.challenge.ledger.domain;

import java.util.Objects;
import java.util.UUID;

public record Account(UUID id, long balance) {

    public Account {
        Objects.requireNonNull(id, "id must not be null");

        if (balance < 0) {
            throw new IllegalArgumentException("balance must not be negative");
        }
    }
}