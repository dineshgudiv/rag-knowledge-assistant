package com.companyname.ragassistant.controller;

import com.companyname.ragassistant.dto.AskRequest;
import com.companyname.ragassistant.dto.AskResponse;
import com.companyname.ragassistant.service.RagService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/chat")
public class ChatController {
    private final RagService ragService;

    public ChatController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest req) {
        return ragService.ask(req);
    }
}