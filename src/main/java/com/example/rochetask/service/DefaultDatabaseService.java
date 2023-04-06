package com.example.rochetask.service;

import com.example.rochetask.exception.DataNotFoundException;
import com.example.rochetask.exception.TooManyTransactionsOpenException;
import com.example.rochetask.exception.TransactionNotFoundException;
import com.example.rochetask.model.Counter;
import com.example.rochetask.model.Entry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

@Service
@Slf4j
public class DefaultDatabaseService implements DatabaseService, TransactionService {

    private static final long ZERO_OCCURRENCES = 0L;
    private static final long ONE_OCCURRENCE = 1L;
    private static final int MAX_OPENED_TRANSACTIONS = 20;

    /**
     * Represents simple in-memory key-value database
     */
    private Map<String, String> database = new HashMap<>();
    /**
     * Additional map for counting values in database to fulfill assignment requirement.
     * Thanks to that we are able to retrieve number of occurrences for each value in O(1)
     * Of course it has a one disadvantage which is taking care of updating this map everytime
     * when entry is removed, saved or updated. But overall it speeds up the process.
     */
    private Map<String, Long> occurrencesByValue = new HashMap<>();

    /**
     * Below we have a representation of transactions mechanism for both maps used above.
     * It's done using Stack (LIFO queue) where each new transactions is added at the
     * top and bases on the previous one to keep track on all changes.
     */
    private final Stack<Map<String, String>> databaseTransactions = new Stack<>();
    private final Stack<Map<String, Long>> occurrencesTransactions = new Stack<>();


    /**
     * Method to put elements into database. In case of started transaction
     * it puts it to the transaction at the top of the stack and does the same
     * with occurrences. In case of executing without started transactions it just
     * simply add or updates entry in database and manages counter in additional map.
     *
     * @param entry - it's already validated in Controller Entry object to put to database
     */
    public void put(Entry entry) {
        if (!databaseTransactions.empty()) {
            log.info("Transaction: Putting entry: {}", entry);
            final var currentDatabaseTransaction = databaseTransactions.peek();
            final var currentOccurrencesTransaction = occurrencesTransactions.peek();
            put(entry, currentDatabaseTransaction, currentOccurrencesTransaction);
        } else {
            log.info("Without transaction: Putting entry: {}", entry);
            put(entry, database, occurrencesByValue);
        }
    }

    /**
     * Method to retrieve entry from database. If transaction is started then it takes
     * value from the last element (transaction) on the Stack which holds all actual values.
     * In other case simply retrieves element from database map. In case of null throws exception.
     *
     * @param key - key to find key-value entry in database
     * @return Entry - retrieved Entry object with key and value fields
     * @throws DataNotFoundException - in case of not found entry in database it throws
     *                               such exception instead of returning null, it's handled in GlobalExceptionHandler
     *                               to return HTTP response to the end user
     */
    public Entry retrieve(String key) throws DataNotFoundException {
        String value;
        if (!databaseTransactions.empty()) {
            log.info("Transaction: Retrieving entry with key: {}", key);
            final var currentDatabaseTransaction = databaseTransactions.peek();
            value = currentDatabaseTransaction.get(key);
        } else {
            log.info("Without transaction: Retrieving entry with key: {}", key);
            value = database.get(key);
        }

        if (value != null) {
            return new Entry(key, value);
        } else {
            throw new DataNotFoundException("Couldn't find entry in database for provided key.");
        }
    }

    /**
     * Metho to remove elements from database. In case of started transaction it removes element
     * from the last element (transaction) of the stack which holds actual values. In case of no
     * started transaction it does it directly on the database. Beside that manages counter of
     * occurrences in both situations.
     *
     * @param key - key to find key-value entry in database
     * @throws DataNotFoundException - in case of not found entry in database it throws
     *                               such exception instead of returning null, it's handled in GlobalExceptionHandler
     *                               to return HTTP response to the end user
     */
    public void remove(String key) throws DataNotFoundException {
        if (!databaseTransactions.empty()) {
            log.info("Transaction: Removing entry with key: {}", key);
            final var currentDatabaseTransaction = databaseTransactions.peek();
            final var currentOccurrencesTransaction = occurrencesTransactions.peek();
            remove(key, currentDatabaseTransaction, currentOccurrencesTransaction);
        } else if (database.containsKey(key)) {
            log.info("Without transaction: Removing entry with key: {}", key);
            remove(key, database, occurrencesByValue);
        } else {
            throw new DataNotFoundException("Couldn't find entry in database for provided key.");
        }
    }

    /**
     * Method using additional Map structure to retrieve occurrences of searched value.
     * Thanks to that it's able to retrieve it in O(1).
     *
     * @param value - value to find all occurrences of it in database
     * @return Counter - object with occurrences of value
     */
    public Counter countEntries(String value) {
        long occurrences;
        if (!occurrencesTransactions.empty()) {
            final var currentOccurrenceTransaction = occurrencesTransactions.peek();
            occurrences = currentOccurrenceTransaction.get(value) != null ? currentOccurrenceTransaction.get(value) :
                    ZERO_OCCURRENCES;
        } else {
            occurrences = occurrencesByValue.get(value) != null ? occurrencesByValue.get(value) : ZERO_OCCURRENCES;
        }

        return new Counter(occurrences);
    }

    public boolean isDuplicatedDatabaseKey(String key) {
        return database.containsKey(key);
    }

    /**
     * Method used to begin a transactions (database and occurrences).
     * In case of first transaction (outer) we make a copy of existing database content as starting point.
     * In case of nested transaction we make a copy of previous transaction.
     * As databaseTransaction is strictly related with occurrencesTransactions we only check emptiness of the first one.
     *
     * @throws TooManyTransactionsOpenException - to prevent issues related to the memory or prevent DDoS number of max
     *                                          open transactions is defined as 20. In case of exceeding it exception will be thrown.
     *                                          It's handled in GlobalExceptionHandler to return HTTP response to end user.
     */
    public void begin() throws TooManyTransactionsOpenException {
        log.info("Begin: Begins new transaction for database and occurrences");
        if (databaseTransactions.size() == MAX_OPENED_TRANSACTIONS) {
            String warnMessage = String.format("Maximum number of open transactions in the same time is: %s.", MAX_OPENED_TRANSACTIONS);
            log.warn(warnMessage);
            throw new TooManyTransactionsOpenException("Please commit your current transactions or rollback some of them. " + warnMessage);
        }

        if (databaseTransactions.empty()) {
            log.info("First (main outer) transaction using current database: {} and occurrences: {}", database,
                    occurrencesByValue);
            databaseTransactions.push(new HashMap<>(database));
            occurrencesTransactions.push(new HashMap<>(occurrencesByValue));
        }

        final var lastDatabaseTransaction = databaseTransactions.peek();
        final var lastOccurrencesTransaction = occurrencesTransactions.peek();
        log.info("New nested transaction using last still open transaction data: {} and occurrences: {}",
                lastDatabaseTransaction, lastOccurrencesTransaction);
        databaseTransactions.push(new HashMap<>(lastDatabaseTransaction));
        occurrencesTransactions.push(new HashMap<>(lastOccurrencesTransaction));
    }

    /**
     * Method used to rollback transaction. It just removes last element (transaction) from the stack of transactions.
     *
     * @throws TransactionNotFoundException - in case of executing this method in situation when transaction has not
     *                                      been started. It's handled in GlobalExceptionHandler to return HTTP response to end user.
     */
    public void rollback() throws TransactionNotFoundException {
        log.info("Rollback: Rollbacks current transaction");
        if (databaseTransactions.empty()) {
            throw new TransactionNotFoundException(
                    "Rollback operation cannot be executed as there are no transactions started.");
        }

        final var removedDatabaseTransaction = databaseTransactions.pop();
        final var removedOccurrenceTransaction = occurrencesTransactions.pop();
        log.info("Rollback: Rollbacks transaction data: {} and occurrences: {}", removedDatabaseTransaction,
                removedOccurrenceTransaction);
    }

    /**
     * Method used to commit all started transaction (outer and all nested).
     * It takes last element of the transaction and assigns it to the main maps: database
     * and other one related to occurrences: occurrencesByValue. Last element holds all
     * data from all transactions thanks to that this operation is sufficient. After that
     * clears both Stacks to prepare them for next transactions.
     */
    public void commit() {
        log.info("Commit: Commits all presently open transactions");
        if (databaseTransactions.empty()) {
            log.warn("Commit: There is no open transactions to commit");
            return;
        }

        database = databaseTransactions.lastElement();
        occurrencesByValue = occurrencesTransactions.lastElement();
        databaseTransactions.clear();
        occurrencesTransactions.clear();
    }

    /**
     * It's just helper function, not 'production' or related to the assignment code.
     * As we use collections to imitate local in memory database it's used to clear all of them
     * in case of testing and running application instead of making a re-run.
     */
    public void clearAll() {
        log.info("Clearing database (all local collections). Ready for new testing.");
        database.clear();
        occurrencesByValue.clear();
        databaseTransactions.clear();
        occurrencesTransactions.clear();
    }

    private void put(Entry entry, Map<String, String> selectedDatabase, Map<String, Long> selectedOccurrencesDatabase) {
        final var newValue = entry.value();
        final var previousValue = selectedDatabase.put(entry.key(), newValue);
        if (previousValue != null) {
            log.info("It's update, so previous value [{}] has to be decremented", previousValue);
            decrementOccurrences(selectedOccurrencesDatabase, previousValue);
        }
        log.info("New value [{}] counter will be incremented", newValue);
        incrementOccurrences(selectedOccurrencesDatabase, newValue);
    }

    private void remove(String key, Map<String, String> selectedDatabase,
                        Map<String, Long> selectedOccurrencesDatabase) {
        final var removedValue = selectedDatabase.remove(key);
        log.info("It's remove, so value [{}] has to be decremented", removedValue);
        decrementOccurrences(selectedOccurrencesDatabase, removedValue);
    }

    private void incrementOccurrences(Map<String, Long> selectedOccurrencesDatabase, String value) {
        var occurrences = selectedOccurrencesDatabase.get(value);
        if (occurrences == null) {
            occurrences = ZERO_OCCURRENCES;
        }
        selectedOccurrencesDatabase.put(value, occurrences + ONE_OCCURRENCE);
    }

    private void decrementOccurrences(Map<String, Long> selectedOccurrencesDatabase, String value) {
        final var occurrences = selectedOccurrencesDatabase.get(value);
        if (occurrences != null && occurrences > ZERO_OCCURRENCES) {
            selectedOccurrencesDatabase.put(value, occurrences - ONE_OCCURRENCE);
        }
    }
}
