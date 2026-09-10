package dev.challenge.ledger.concurrency;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

public class AccountLockManager {

    private final ConcurrentMap<UUID, ReentrantLock> locks =
            new ConcurrentHashMap<>();

    public ReentrantLock lockFor(UUID accountId) {
        return locks.computeIfAbsent(
                accountId,
                ignored -> new ReentrantLock()
        );
    }
}