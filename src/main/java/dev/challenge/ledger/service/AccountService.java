package dev.challenge.ledger.service;

import dev.challenge.ledger.domain.Account;
import dev.challenge.ledger.storage.AccountStore;

import java.util.UUID;

public class AccountService {

    private final AccountStore accountStore;

    public AccountService(AccountStore accountStore) {
        this.accountStore = accountStore;
    }

    public Account createAccount(long initialBalance) {
        return accountStore.create(initialBalance);
    }

    public Account getAccount(UUID accountId) {
        return accountStore.findById(accountId)
                .orElseThrow(() ->
                        new IllegalArgumentException("account not found")
                );
    }
}