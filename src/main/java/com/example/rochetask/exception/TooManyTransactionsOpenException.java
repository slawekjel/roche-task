package com.example.rochetask.exception;

public class TooManyTransactionsOpenException extends RuntimeException {

    public TooManyTransactionsOpenException(String message) {
        super(message);
    }
}
