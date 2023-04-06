package com.example.rochetask.controller;


import com.example.rochetask.model.Counter;
import com.example.rochetask.model.Entry;
import com.example.rochetask.model.ErrorMessage;
import com.example.rochetask.service.DatabaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "entries", description = "Endpoints to perform operations on database entries")
@RestController
@RequestMapping("/api/v1/database/entries")
@RequiredArgsConstructor
public class DatabaseEntriesController {

    private final DatabaseService databaseService;

    @Operation(summary = "Endpoint to create new entry or replace old one.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Old entry has been replaces with new " + "value",
                    content = @Content),
            @ApiResponse(responseCode = "201", description = "New entry has been created", content = @Content),
            @ApiResponse(responseCode = "400",
                    description = "Both key and value must have between 1 and 10 characters and contain only alphanumeric characters.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorMessage.class)))})
    @PutMapping
    public ResponseEntity<Void> setEntry(@Parameter(description =
            "Entry with key/value as strings which both must have between 1 and 10 characters and contain " +
                    "only alphanumeric characters.") @RequestBody @Valid Entry entry) {
        if (databaseService.isDuplicatedDatabaseKey(entry.key())) {
            databaseService.put(entry);
            return ResponseEntity.ok().build();
        } else {
            databaseService.put(entry);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
    }

    @Operation(summary = "Endpoint to retrieve entry using key.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Found entry with provided key",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Entry.class))),
            @ApiResponse(responseCode = "404", description = "Couldn't find an entry with provided key",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorMessage.class)))})
    @GetMapping("/{key}")
    public ResponseEntity<Entry> getEntry(
            @Parameter(description = "Key of entry to retrieve") @PathVariable String key) {
        final var entry = databaseService.retrieve(key);
        return ResponseEntity.ok(entry);
    }

    @Operation(summary = "Endpoint to remove entry using key.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Entry has been removed with success", content = @Content),
            @ApiResponse(responseCode = "404", description = "Couldn't find an entry to remove with provided key",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorMessage.class)))})
    @DeleteMapping("/{key}")
    public ResponseEntity<Void> deleteEntry(
            @Parameter(description = "Key of entry to remove") @PathVariable String key) {
        databaseService.remove(key);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Endpoint to count occurrences of provided value in database.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Value with number of occurrences for " + "provided value",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Counter.class)))})
    @GetMapping("/counters/{value}")
    public ResponseEntity<Counter> countEntries(
            @Parameter(description = "Value to check occurrences in database") @PathVariable String value) {
        final var counter = databaseService.countEntries(value);
        return ResponseEntity.ok(counter);
    }
}
