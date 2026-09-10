package dev.challenge.ledger.storage;

import dev.challenge.ledger.domain.Account;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryAccountStore implements AccountStore {

    private final ConcurrentMap<UUID, Account> accounts = new ConcurrentHashMap<>();

    @Override
    public Account create(long initialBalance) {
        Account account = new Account(UUID.randomUUID(), initialBalance);
        accounts.put(account.id(), account);
        return account;
    }

    @Override
    public Optional<Account> findById(UUID accountId) {
        return Optional.ofNullable(accounts.get(accountId));
    }

    @Override
    public void update(Account account) {
        accounts.put(account.id(), account);
    }
}