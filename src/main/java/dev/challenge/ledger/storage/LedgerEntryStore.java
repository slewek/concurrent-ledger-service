package dev.challenge.ledger.storage;

import dev.challenge.ledger.domain.LedgerEntry;

import java.util.List;
import java.util.UUID;

public interface LedgerEntryStore {

    void appendAll(List<LedgerEntry> entries);

    List<LedgerEntry> findByTransferId(UUID transferId);
}