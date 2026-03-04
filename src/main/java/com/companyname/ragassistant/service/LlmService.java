package com.companyname.ragassistant.service;

import org.springframework.stereotype.Service;

/**
 * Stub to keep scaffold compiling. MVP is retrieval-only (extractive answer from citations).
 * Plug a real provider later (OpenAI/Ollama/etc).
 */
@Service
public class LlmService {
    public String complete(String prompt) {
        throw new UnsupportedOperationException("LLM provider not configured yet.");
    }
}