package com.example.rochetask.controller;

import com.example.rochetask.service.DatabaseService;
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

@WebMvcTest(DatabaseController.class)
@ExtendWith(MockitoExtension.class)
class DatabaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DatabaseService databaseService;

    @Test
    void shouldReturnSuccessfulResponse_whenClearsDatabase() throws Exception {
        mockMvc.perform(post("/api/v1/database/clear"))
                .andExpect(status().isOk());

        verify(databaseService, times(1)).clearAll();
    }
}
