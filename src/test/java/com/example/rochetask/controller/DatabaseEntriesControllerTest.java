package com.example.rochetask.controller;

import com.example.rochetask.exception.DataNotFoundException;
import com.example.rochetask.model.Counter;
import com.example.rochetask.model.Entry;
import com.example.rochetask.service.DatabaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DatabaseEntriesController.class)
@ExtendWith(MockitoExtension.class)
class DatabaseEntriesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DatabaseService databaseService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReturnCreatedForPut_whenNewEntryWillBeCreated() throws Exception {
        Entry entry = new Entry("key", "value");
        String entryJson = objectMapper.writeValueAsString(entry);
        when(databaseService.isDuplicatedDatabaseKey("key")).thenReturn(false);
        mockMvc.perform(put(Fixtures.ENTRIES_ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(entryJson))
                .andExpect(status().isCreated());

        verify(databaseService, times(1)).put(entry);
    }

    @Test
    void shouldReturnOkForPut_whenNewEntryWillReplaceOld() throws Exception {
        Entry entry = new Entry("key", "value");
        String entryJson = objectMapper.writeValueAsString(entry);
        when(databaseService.isDuplicatedDatabaseKey("key")).thenReturn(true);
        mockMvc.perform(put(Fixtures.ENTRIES_ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(entryJson))
                .andExpect(status().isOk());

        verify(databaseService, times(1)).put(entry);
    }

    @Test
    void shouldReturnBadRequestForPut_whenRequestBodyValidationFails_forKey() throws Exception {
        mockMvc.perform(put(Fixtures.ENTRIES_ENDPOINT).contentType(MediaType.APPLICATION_JSON)
                        .content(Fixtures.ENTRY_WITH_WRONG_KEY)).andExpect(status().isBadRequest())
                .andExpect(content().json(Fixtures.WRONG_KEY_MESSAGE));

        verify(databaseService, times(0)).put(any());
    }

    @Test
    void shouldReturnBadRequestForPut_whenRequestBodyValidationFails_forValue() throws Exception {
        mockMvc.perform(put(Fixtures.ENTRIES_ENDPOINT).contentType(MediaType.APPLICATION_JSON)
                        .content(Fixtures.ENTRY_WITH_WRONG_VALUE)).andExpect(status().isBadRequest())
                .andExpect(content().json(Fixtures.WRONG_VALUE_MESSAGE));

        verify(databaseService, times(0)).put(any());
    }

    @Test
    void shouldReturnBadRequestForPut_whenRequestBodyValidationFails_forKeyAndValue() throws Exception {
        mockMvc.perform(put(Fixtures.ENTRIES_ENDPOINT).contentType(MediaType.APPLICATION_JSON)
                .content(Fixtures.ENTRY_WITH_WRONG_KEY_AND_VALUE)).andExpect(status().isBadRequest());

        verify(databaseService, times(0)).put(any());
    }

    @Test
    void shouldReturnOkWithBody_whenSuccessfullyRetrievesEntry() throws Exception {
        String key = "key";
        Entry entry = new Entry(key, "value");
        String entryJson = objectMapper.writeValueAsString(entry);
        when(databaseService.retrieve(key)).thenReturn(entry);
        mockMvc.perform(get(Fixtures.ENTRIES_ENDPOINT + "/" + key).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().json(entryJson));

        verify(databaseService, times(1)).retrieve(key);
    }

    @Test
    void shouldReturnNotFoundWithBody_whenCantRetrieveEntry() throws Exception {
        String key = "key";
        when(databaseService.retrieve(key)).thenThrow(new DataNotFoundException("cant find entry"));
        mockMvc.perform(get(Fixtures.ENTRIES_ENDPOINT + "/" + key).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()).andExpect(content().json(Fixtures.DATA_NOT_FOUND_MESSAGE));

        verify(databaseService, times(1)).retrieve(key);
    }

    @Test
    void shouldReturnNoContent_whenSuccessfullyRemovesEntry() throws Exception {
        String key = "key";
        mockMvc.perform(delete(Fixtures.ENTRIES_ENDPOINT + "/" + key).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(databaseService, times(1)).remove(key);
    }

    @Test
    void shouldReturnNotFoundWithBody_whenCantFindEntryToRemove() throws Exception {
        String key = "key";
        doThrow(new DataNotFoundException("cant find entry")).when(databaseService).remove(key);
        mockMvc.perform(delete(Fixtures.ENTRIES_ENDPOINT + "/" + key).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()).andExpect(content().json(Fixtures.DATA_NOT_FOUND_MESSAGE));

        verify(databaseService, times(1)).remove(key);
    }

    @Test
    void shouldReturnOkWithBody_whenSuccessfullyCountEntries() throws Exception {
        String value = "value";
        when(databaseService.countEntries(value)).thenReturn(new Counter(10));
        mockMvc.perform(get(Fixtures.ENTRIES_ENDPOINT + "/counters/" + value).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().json(Fixtures.OCCURRENCES_MESSAGE));

        verify(databaseService, times(1)).countEntries(value);
    }

    private static class Fixtures {
        private static final String ENTRY_WITH_WRONG_KEY = "{\"key\":\"wrong?key\",\"value\":\"value\"}";
        private static final String WRONG_KEY_MESSAGE =
                "{\"message\":\"[The 'key' must have between 1 and 10 characters and contain only " +
                        "alphanumeric characters.]\"}";
        private static final String ENTRY_WITH_WRONG_VALUE = "{\"key\":\"key\",\"value\":\"wrong?value\"}";
        private static final String WRONG_VALUE_MESSAGE =
                "{\"message\":\"[The 'value' must have between 1 and 10 characters and contain only " +
                        "alphanumeric characters.]\"}";
        private static final String ENTRY_WITH_WRONG_KEY_AND_VALUE =
                "{\"key\":\"wrong?key\",\"value\":\"wrong?value\"}";

        private static final String OCCURRENCES_MESSAGE = "{\"occurrences\":10}";
        private static final String DATA_NOT_FOUND_MESSAGE = "{\"message\":\"cant find entry\"}";

        private static final String ENTRIES_ENDPOINT = "/api/v1/database/entries";
    }
}
