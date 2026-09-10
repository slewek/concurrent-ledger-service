package dev.challenge.ledger.storage;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryAccountStoreTest {

    private final AccountStore accountStore = new InMemoryAccountStore();

    @Test
    void shouldCreateAccountWithInitialBalance() {
        var account = accountStore.create(10_000);

        assertNotNull(account.id());
        assertEquals(10_000, account.balance());
    }

    @Test
    void shouldFindCreatedAccount() {
        var created = accountStore.create(10_000);

        var found = accountStore.findById(created.id());

        assertTrue(found.isPresent());
        assertEquals(created, found.get());
    }

    @Test
    void shouldReturnEmptyForUnknownAccount() {
        var result = accountStore.findById(UUID.randomUUID());

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRejectNegativeInitialBalance() {
        assertThrows(
                IllegalArgumentException.class,
                () -> accountStore.create(-1)
        );
    }
}