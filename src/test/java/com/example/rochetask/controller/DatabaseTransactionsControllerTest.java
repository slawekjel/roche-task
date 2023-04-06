package com.example.rochetask.controller;

import com.example.rochetask.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DatabaseTransactionsController.class)
@ExtendWith(MockitoExtension.class)
class DatabaseTransactionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @Test
    void shouldReturnSuccessfulResponse_whenBeginsTransaction() throws Exception {
        mockMvc.perform(post("/api/v1/database/transactions/begin"))
                .andExpect(status().isOk());

        verify(transactionService, times(1)).begin();
    }

    @Test
    void shouldReturnSuccessfulResponse_whenRollbacksTransaction() throws Exception {
        mockMvc.perform(post("/api/v1/database/transactions/rollback"))
                .andExpect(status().isOk());

        verify(transactionService, times(1)).rollback();
    }

    @Test
    void shouldReturnSuccessfulResponse_whenCommitsTransaction() throws Exception {
        mockMvc.perform(post("/api/v1/database/transactions/commit"))
                .andExpect(status().isOk());

        verify(transactionService, times(1)).commit();
    }
}
