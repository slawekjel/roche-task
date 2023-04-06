package com.example.rochetask.model;

import jakarta.validation.constraints.Pattern;

public record Entry(
        @Pattern(regexp = "[0-9A-Za-z]{1,10}", message = "The 'key' must have between 1 and 10 characters and contain only alphanumeric characters.") String key,
        @Pattern(regexp = "[0-9A-Za-z]{1,10}", message = "The 'value' must have between 1 and 10 characters and contain only alphanumeric characters.") String value) {
}
