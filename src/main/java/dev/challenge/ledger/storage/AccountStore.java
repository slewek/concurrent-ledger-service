package dev.challenge.ledger.storage;

import dev.challenge.ledger.domain.Account;

import java.util.Optional;
import java.util.UUID;

public interface AccountStore {

    Account create(long initialBalance);

    Optional<Account> findById(UUID accountId);

    void update(Account account);
}