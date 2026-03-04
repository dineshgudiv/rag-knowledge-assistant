package com.companyname.ragassistant.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void askEndpoint_returnsContract() throws Exception {
        mockMvc.perform(post("/v1/chat/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"unknown\",\"topK\":3,\"minScore\":0.9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").isString())
                .andExpect(jsonPath("$.citations").isArray())
                .andExpect(jsonPath("$.bestScore").isNumber())
                .andExpect(jsonPath("$.queryLogId").isNumber())
                .andExpect(jsonPath("$.latencyMs").isNumber());
    }
}
