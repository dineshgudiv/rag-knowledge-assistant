package com.companyname.ragassistant.service;

import com.companyname.ragassistant.dto.AskRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RagServiceTest {

    @Autowired
    private RagService ragService;

    @Test
    void unknownQuestion_returnsIDontKnow() {
        var res = ragService.ask(new AskRequest("this should not match anything", 3, 0.95, null, true));
        assertEquals("I don't know.", res.answer());
        assertNotNull(res.queryLogId());
    }
}
