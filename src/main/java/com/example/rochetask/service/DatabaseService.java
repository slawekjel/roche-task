package com.example.rochetask.service;

import com.example.rochetask.exception.DataNotFoundException;
import com.example.rochetask.model.Counter;
import com.example.rochetask.model.Entry;

public interface DatabaseService {

    void put(Entry entry);

    Entry retrieve(String key) throws DataNotFoundException;

    void remove(String key) throws DataNotFoundException;

    Counter countEntries(String value);

    boolean isDuplicatedDatabaseKey(String key);

    void clearAll();
}
