package com.example.rochetask.service;

import com.example.rochetask.exception.TooManyTransactionsOpenException;
import com.example.rochetask.exception.TransactionNotFoundException;

public interface TransactionService {

    void begin() throws TooManyTransactionsOpenException;

    void rollback() throws TransactionNotFoundException;

    void commit();
}
