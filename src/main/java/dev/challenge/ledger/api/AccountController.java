package dev.challenge.ledger.api;

import dev.challenge.ledger.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(
            @RequestBody CreateAccountRequest request
    ) {
        var account = accountService.createAccount(
                request.initialBalance()
        );

        return new AccountResponse(
                account.id(),
                account.balance()
        );
    }

    @GetMapping("/{accountId}/balance")
    public AccountResponse getBalance(
            @PathVariable UUID accountId
    ) {
        var account = accountService.getAccount(accountId);

        return new AccountResponse(
                account.id(),
                account.balance()
        );
    }
}