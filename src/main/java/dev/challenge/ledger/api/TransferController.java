package dev.challenge.ledger.api;

import dev.challenge.ledger.service.LedgerService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final LedgerService ledgerService;

    public TransferController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping
    public TransferResponse transfer(
            @RequestHeader("Idempotency-Key")
            String idempotencyKey,
            @RequestBody
            TransferRequest request
    ) {
        var result = ledgerService.transfer(
                idempotencyKey,
                request.fromAccountId(),
                request.toAccountId(),
                request.amount()
        );

        return new TransferResponse(
                result.transferId(),
                result.status(),
                result.fromAccountId(),
                result.toAccountId(),
                result.amount()
        );
    }
}