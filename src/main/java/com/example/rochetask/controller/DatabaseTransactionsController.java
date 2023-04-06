package com.example.rochetask.controller;


import com.example.rochetask.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "transactions", description = "Endpoints to manage transactions")
@RestController
@RequestMapping("/api/v1/database/transactions")
@RequiredArgsConstructor
public class DatabaseTransactionsController {

    private final TransactionService transactionService;

    @Operation(summary = "Endpoint which begins the transaction. In case of executing again before committing all " +
            "transactions or before rollback the first and only one transaction it will start nested transaction.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "New transaction started (first outer or next nested).",
                    content = @Content)})
    @PostMapping("/begin")
    public ResponseEntity<Void> beginTransaction() {
        transactionService.begin();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Endpoint which commits all started transactions.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200",
            description = "Committed all started transactions. All data from all " +
                    "transactions is saved to database.", content = @Content)})
    @PostMapping("/commit")
    public ResponseEntity<Void> commitTransactions() {
        transactionService.commit();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Endpoint which rollbacks current transaction.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Current transaction has been rolled back.",
            content = @Content), @ApiResponse(responseCode = "404",
            description = "There is no started transaction, so rollback can't be " + "executed.", content = @Content)})
    @PostMapping("/rollback")
    public ResponseEntity<Void> rollbackTransaction() {
        transactionService.rollback();
        return ResponseEntity.ok().build();
    }
}
