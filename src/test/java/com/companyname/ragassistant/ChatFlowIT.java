package com.companyname.ragassistant;

import com.companyname.ragassistant.dto.AskRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ChatFlowIT {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper om;

  @Test
  void upload_then_ask_in_scope_returns_citations() throws Exception {
    var file = new MockMultipartFile("file", "note.txt", "text/plain",
        "RAG means retrieval augmented generation.\nThis system returns citations.".getBytes());

    mvc.perform(multipart("/v1/documents/upload").file(file))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.chunkCount").value(org.hamcrest.Matchers.greaterThan(0)));

    var req = new AskRequest("What does RAG mean?", 3, 0.2, null, true);
    mvc.perform(post("/v1/chat/ask")
            .contentType(MediaType.APPLICATION_JSON)
            .content(om.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.citations.length()").value(org.hamcrest.Matchers.greaterThan(0)));
  }

  @Test
  void ask_out_of_scope_returns_idk() throws Exception {
    var req = new AskRequest("What is the capital of Mars?", 3, 0.8, null, true);
    mvc.perform(post("/v1/chat/ask")
            .contentType(MediaType.APPLICATION_JSON)
            .content(om.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.answer").value("I don't know."));
  }
}
