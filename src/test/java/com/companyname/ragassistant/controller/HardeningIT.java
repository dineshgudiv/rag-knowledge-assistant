package com.companyname.ragassistant.controller;

import com.companyname.ragassistant.repository.ChunkRepository;
import com.companyname.ragassistant.repository.DocumentRepository;
import com.companyname.ragassistant.util.TextChunker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.matchesRegex;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HardeningIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ChunkRepository chunkRepository;

    @BeforeEach
    void clean() {
        chunkRepository.deleteAll();
        documentRepository.deleteAll();
    }

    @Test
    void ask_withQuestionNoAllowedDocIds_returns200() throws Exception {
        mockMvc.perform(post("/v1/chat/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"sports\",\"topK\":3,\"minScore\":0.2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").isString())
                .andExpect(jsonPath("$.citations").isArray());
    }

    @Test
    void ask_invalidPayload_returns400() throws Exception {
        mockMvc.perform(post("/v1/chat/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void ask_withZeroDocuments_returnsIdk200() throws Exception {
        mockMvc.perform(post("/v1/chat/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"sports\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("I don't know."))
                .andExpect(jsonPath("$.citations").isEmpty());
    }

    @Test
    void legacyHealthEndpoints_return200() throws Exception {
        mockMvc.perform(get("/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").exists());
        mockMvc.perform(get("/liveness")).andExpect(status().isOk()).andExpect(jsonPath("$.status").exists());
        mockMvc.perform(get("/readiness")).andExpect(status().isOk()).andExpect(jsonPath("$.status").exists());
        mockMvc.perform(get("/favicon.ico")).andExpect(status().isNoContent());
    }

    @Test
    void upload_withControlBytes_succeeds() throws Exception {
        byte[] badBytes = "hello".getBytes(StandardCharsets.UTF_8);
        byte[] withControl = new byte[badBytes.length + 3];
        System.arraycopy(badBytes, 0, withControl, 0, badBytes.length);
        withControl[badBytes.length] = 0x00;
        withControl[badBytes.length + 1] = 0x01;
        withControl[badBytes.length + 2] = 0x02;

        MockMultipartFile file = new MockMultipartFile("file", "nul-test.txt", "text/plain", withControl);

        mockMvc.perform(multipart("/v1/documents/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").isNumber());
        mockMvc.perform(post("/v1/chat/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"hello\",\"topK\":3,\"minScore\":0.0,\"redactPii\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.citations").isArray());
    }

    @Test
    void upload_largeText_isBoundedAndNoCrash() throws Exception {
        String large = "sports ".repeat(1_400_000); // > MAX_DOC_CHARS after spacing
        MockMultipartFile file = new MockMultipartFile("file", "large.txt", "text/plain", large.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/v1/documents/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chunkCount").isNumber())
                .andExpect(jsonPath("$.chunkCount").value(org.hamcrest.Matchers.lessThanOrEqualTo(TextChunker.MAX_CHUNKS)));
    }

    @Test
    void snippet_respectsLengthWordBoundaryAndPiiRedaction() throws Exception {
        String text = "Technical Skills and sports interest include badminton and volleyball. " +
                "Contact ravi@example.com and phone 987-654-3210. Date of birth 16/05/2002. " +
                "Further details about sports achievements and practice schedule for tournaments.";
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", text.getBytes());

        mockMvc.perform(multipart("/v1/documents/upload").file(file))
                .andExpect(status().isOk());

        mockMvc.perform(post("/v1/chat/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"sports\",\"topK\":3,\"minScore\":0.0,\"redactPii\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.citations[0].support_span.length()").value(lessThanOrEqualTo(350)))
                .andExpect(jsonPath("$.citations[0].support_span").value(matchesRegex("^(\\.\\.\\.)?[A-Za-z0-9].*")))
                .andExpect(jsonPath("$.citations[0].support_span").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.matchesRegex("(?i).*[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}.*"))))
                .andExpect(jsonPath("$.citations[0].support_span").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.matchesRegex("(?i).*(?:dob|date\\s*of\\s*birth|\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b).*"))));
    }
}
