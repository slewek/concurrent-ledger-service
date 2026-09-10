package dev.challenge.ledger.storage;

import dev.challenge.ledger.domain.LedgerEntry;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class InMemoryLedgerEntryStore implements LedgerEntryStore {

    private final ConcurrentLinkedQueue<LedgerEntry> entries =
            new ConcurrentLinkedQueue<>();

    @Override
    public void appendAll(List<LedgerEntry> entries) {
        this.entries.addAll(entries);
    }

    @Override
    public List<LedgerEntry> findByTransferId(UUID transferId) {
        return entries.stream()
                .filter(entry -> entry.transferId().equals(transferId))
                .toList();
    }
}