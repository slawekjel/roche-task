package com.example.rochetask.controller;


import com.example.rochetask.service.DatabaseService;
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

@Tag(name = "database", description = "Endpoints to managing database")
@RestController
@RequestMapping("/api/v1/database/")
@RequiredArgsConstructor
public class DatabaseController {

    private final DatabaseService databaseService;

    @Operation(summary = "Helper endpoint to clear database in case of easier local testing")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All collections (database and transactions) are cleared" +
                    ". Application is ready for new testing.",
                    content = @Content)})
    @PostMapping("/clear")
    public ResponseEntity<Void> clearDatabase() {
        databaseService.clearAll();
        return ResponseEntity.ok().build();
    }
}
