package com.example.rochetask.service;

import com.example.rochetask.exception.DataNotFoundException;
import com.example.rochetask.exception.TooManyTransactionsOpenException;
import com.example.rochetask.exception.TransactionNotFoundException;
import com.example.rochetask.model.Entry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class DefaultDatabaseServiceTest {

    private final DefaultDatabaseService databaseService = new DefaultDatabaseService();

    @AfterEach
    void tearDown() {
        databaseService.clearAll();
    }

    @Test
    void shouldPutNewEntryToDatabase() {
        // given
        String key = "key1";
        Entry entry = new Entry(key, "value1");

        // when
        databaseService.put(entry);

        // then
        assertThat(databaseService.retrieve(key)).isEqualTo(entry);
    }

    @Test
    void shouldReplaceExistingEntryInDatabase() {
        String key = "key1";
        Entry firstEntry = new Entry(key, "value1");
        Entry newEntry = new Entry(key, "value2");

        databaseService.put(firstEntry);
        databaseService.put(newEntry);

        assertThat(databaseService.retrieve(key)).isEqualTo(newEntry);
    }

    @Test
    void shouldThrowException_whenValueToRetrieveIsNull() {
        assertThatThrownBy(() -> databaseService.retrieve("key")).isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("Couldn't find entry in database for provided key.");
    }

    @Test
    void shouldRemoveEntry() {
        String key = "key1";
        Entry entry = new Entry(key, "value1");
        databaseService.put(entry);

        databaseService.remove(key);

        assertThatThrownBy(() -> databaseService.retrieve(key)).isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("Couldn't find entry in database for provided key.");
    }

    @Test
    void shouldThrowException_whenThereIsNoKeyToRemoveEntry() {
        assertThatThrownBy(() -> databaseService.remove("key")).isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("Couldn't find entry in database for provided key.");
    }

    @Test
    void shouldReturnEmptyCounter_whenDatabaseIsEmpty() {
        assertThat(databaseService.countEntries("value").occurrences()).isEqualTo(0);
    }

    @Test
    void shouldReturnCounter_with1Occurrence() {
        String key = "key1";
        Entry entry = new Entry(key, "value1");
        databaseService.put(entry);

        long occurrences = databaseService.countEntries("value1").occurrences();

        assertThat(occurrences).isEqualTo(1);
    }

    @Test
    void shouldReturnCounter_with1Occurrence_whenUpdateWithSameValue() {
        String key = "key1";
        Entry entry = new Entry(key, "value1");
        // Create
        databaseService.put(entry);
        // Update
        databaseService.put(entry);

        long occurrences = databaseService.countEntries("value1").occurrences();

        assertThat(occurrences).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("provideKeyToCheckDuplicates")
    void shouldReturnTrueIfDuplicatedKey_andFalseIfNot(String key, boolean expected) {
        databaseService.put(new Entry("key1", "value"));
        assertThat(databaseService.isDuplicatedDatabaseKey(key)).isEqualTo(expected);
    }

    @Test
    void shouldThrowException_whenTooManyTransactionsOpen() {
        // Given: begin 20 transactions
        for (int i = 0; i < 19; i++) {
            databaseService.begin();
        }

        // Then try to begin 21st transaction
        assertThatThrownBy(databaseService::begin).isInstanceOf(TooManyTransactionsOpenException.class)
                .hasMessageContaining("Please commit your current transactions or rollback some of them. " +
                        "Maximum number of open transactions in the same time is: 20.");
    }

    @Test
    @DisplayName("Test if transactions work as expected by making a several operations using them")
    void shouldPass_whenFlowWithTransactionsWorksProperly() {
        // Put key1:value1 and key2:value1 directly to database and check
        databaseService.put(new Entry("key1", "value1"));
        databaseService.put(new Entry("key2", "value1"));
        assertThat(databaseService.retrieve("key1").value()).isEqualTo("value1");
        assertThat(databaseService.retrieve("key2").value()).isEqualTo("value1");

        // Start outer transaction
        databaseService.begin();
        // Check occurrences
        assertThat(databaseService.countEntries("value1").occurrences()).isEqualTo(2);
        // Update first entry
        databaseService.put(new Entry("key1", "value2"));
        // Add other entry with the same value as on the beginning
        databaseService.put(new Entry("key3", "value1"));
        assertThat(databaseService.retrieve("key3").value()).isEqualTo("value1");
        // Check occurrences (still should be 2)
        assertThat(databaseService.countEntries("value1").occurrences()).isEqualTo(2);

        // Start nested transaction
        databaseService.begin();
        // Remove key1 and key2 entries
        databaseService.remove("key1");
        databaseService.remove("key2");
        assertThatThrownBy(() -> databaseService.retrieve("key1")).isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("Couldn't find entry in database for provided key.");
        assertThatThrownBy(() -> databaseService.retrieve("key2")).isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("Couldn't find entry in database for provided key.");
        // Check occurrences (should be 1)
        assertThat(databaseService.countEntries("value1").occurrences()).isEqualTo(1);

        // Start another nested transaction
        databaseService.begin();
        // Add again key2:value1 and new key4:value4
        databaseService.put(new Entry("key2", "value1"));
        databaseService.put(new Entry("key4", "value4"));
        assertThat(databaseService.retrieve("key2").value()).isEqualTo("value1");
        assertThat(databaseService.retrieve("key4").value()).isEqualTo("value4");
        // Check occurrences (should be 2)
        assertThat(databaseService.countEntries("value1").occurrences()).isEqualTo(2);

        // Rollback last transaction (key2 and key4 shouldn't be available)
        databaseService.rollback();
        assertThatThrownBy(() -> databaseService.retrieve("key2")).isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("Couldn't find entry in database for provided key.");
        assertThatThrownBy(() -> databaseService.retrieve("key4")).isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("Couldn't find entry in database for provided key.");
        assertThat(databaseService.countEntries("value1").occurrences()).isEqualTo(1);

        // Commit all transactions with final result as below assertions
        databaseService.commit();
        assertThatThrownBy(() -> databaseService.retrieve("key1")).isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("Couldn't find entry in database for provided key.");
        assertThatThrownBy(() -> databaseService.retrieve("key2")).isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("Couldn't find entry in database for provided key.");
        assertThat(databaseService.retrieve("key3").value()).isEqualTo("value1");
        assertThatThrownBy(() -> databaseService.retrieve("key4")).isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("Couldn't find entry in database for provided key.");
        assertThat(databaseService.countEntries("value1").occurrences()).isEqualTo(1);
    }

    private static Stream<Arguments> provideKeyToCheckDuplicates() {
        return Stream.of(Arguments.of("key1", true), Arguments.of("key2", false), Arguments.of("", false),
                Arguments.of(null, false));
    }

    /**
     * Below are tests cases from Programming Assignment PDF
     * Names are corresponding the number of the test from table
     * Thus there is no meaningful names for those tests
     */
    @Test
    void testCase1() {
        databaseService.begin();
        databaseService.put(new Entry("a", "1"));
        assertThat(databaseService.retrieve("a").value()).isEqualTo("1");

        databaseService.begin();
        databaseService.put(new Entry("a", "2"));
        assertThat(databaseService.retrieve("a").value()).isEqualTo("2");

        databaseService.rollback();
        assertThat(databaseService.retrieve("a").value()).isEqualTo("1");

        databaseService.rollback();
        assertThatThrownBy(() -> databaseService.retrieve("a")).isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("Couldn't find entry in database for provided key.");
    }

    @Test
    void testCase2() {
        databaseService.put(new Entry("a", "1"));
        databaseService.put(new Entry("b", "1"));
        assertThat(databaseService.countEntries("1").occurrences()).isEqualTo(2);

        assertThat(databaseService.countEntries("2").occurrences()).isEqualTo(0);

        databaseService.remove("a");
        assertThat(databaseService.countEntries("1").occurrences()).isEqualTo(1);

        databaseService.put(new Entry("b", "3"));
        assertThat(databaseService.countEntries("1").occurrences()).isEqualTo(0);
    }

    @Test
    void testCase3() {
        databaseService.begin();
        databaseService.put(new Entry("a", "1"));
        assertThat(databaseService.retrieve("a").value()).isEqualTo("1");

        databaseService.begin();
        databaseService.put(new Entry("a", "2"));
        assertThat(databaseService.retrieve("a").value()).isEqualTo("2");

        databaseService.rollback();
        assertThat(databaseService.retrieve("a").value()).isEqualTo("1");

        databaseService.rollback();
        assertThatThrownBy(() -> databaseService.retrieve("a")).isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("Couldn't find entry in database for provided key.");
    }

    @Test
    void testCase4() {
        databaseService.begin();
        databaseService.put(new Entry("a", "1"));

        databaseService.begin();
        databaseService.put(new Entry("a", "2"));

        databaseService.commit();
        assertThat(databaseService.retrieve("a").value()).isEqualTo("2");

        assertThatThrownBy(databaseService::rollback).isInstanceOf(TransactionNotFoundException.class)
                .hasMessageContaining("Rollback operation cannot be executed as there are no transactions started.");
    }

    @Test
    void testCase5() {
        databaseService.put(new Entry("a", "1"));

        databaseService.begin();
        assertThat(databaseService.retrieve("a").value()).isEqualTo("1");

        databaseService.put(new Entry("a", "2"));
        databaseService.begin();
        databaseService.remove("a");
        assertThatThrownBy(() -> databaseService.retrieve("a")).isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("Couldn't find entry in database for provided key.");

        databaseService.rollback();
        assertThat(databaseService.retrieve("a").value()).isEqualTo("2");

        databaseService.commit();
        assertThat(databaseService.retrieve("a").value()).isEqualTo("2");
    }

    @Test
    void testCase6() {
        databaseService.put(new Entry("a", "1"));
        databaseService.begin();
        assertThat(databaseService.countEntries("1").occurrences()).isEqualTo(1);

        databaseService.begin();
        databaseService.remove("a");
        assertThat(databaseService.countEntries("1").occurrences()).isEqualTo(0);

        databaseService.rollback();
        assertThat(databaseService.countEntries("1").occurrences()).isEqualTo(1);
    }
}
