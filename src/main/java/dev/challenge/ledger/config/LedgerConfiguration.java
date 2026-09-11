package dev.challenge.ledger.config;

import dev.challenge.ledger.concurrency.AccountLockManager;
import dev.challenge.ledger.idempotency.IdempotencyStore;
import dev.challenge.ledger.idempotency.InMemoryIdempotencyStore;
import dev.challenge.ledger.service.AccountService;
import dev.challenge.ledger.service.LedgerService;
import dev.challenge.ledger.storage.AccountStore;
import dev.challenge.ledger.storage.InMemoryAccountStore;
import dev.challenge.ledger.storage.InMemoryLedgerEntryStore;
import dev.challenge.ledger.storage.LedgerEntryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LedgerConfiguration {

    @Bean
    AccountStore accountStore() {
        return new InMemoryAccountStore();
    }

    @Bean
    LedgerEntryStore ledgerEntryStore() {
        return new InMemoryLedgerEntryStore();
    }

    @Bean
    AccountLockManager accountLockManager() {
        return new AccountLockManager();
    }

    @Bean
    IdempotencyStore idempotencyStore() {
        return new InMemoryIdempotencyStore();
    }

    @Bean
    AccountService accountService(AccountStore accountStore) {
        return new AccountService(accountStore);
    }

    @Bean
    LedgerService ledgerService(
            AccountStore accountStore,
            LedgerEntryStore ledgerEntryStore,
            AccountLockManager accountLockManager,
            IdempotencyStore idempotencyStore
    ) {
        return new LedgerService(
                accountStore,
                ledgerEntryStore,
                accountLockManager,
                idempotencyStore
        );
    }
}