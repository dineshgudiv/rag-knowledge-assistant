package com.companyname.ragassistant.util;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptBuilder {

    public String build(String question, List<String> contextChunks) {
        String context = String.join("\n---\n", contextChunks);
        return "Answer using only the context below. Include concise references.\n\nContext:\n" + context + "\n\nQuestion: " + question;
    }
}
