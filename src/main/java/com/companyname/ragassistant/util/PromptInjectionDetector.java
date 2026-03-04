package com.companyname.ragassistant.util;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class PromptInjectionDetector {
    private static final List<Rule> RULES = List.of(
            new Rule("override_instructions", Pattern.compile("\\b(ignore|bypass|override)\\b.{0,40}\\b(instruction|policy|guardrail|rule|system)\\b", Pattern.CASE_INSENSITIVE)),
            new Rule("role_hijack", Pattern.compile("\\b(you are now|act as|pretend to be|roleplay as)\\b", Pattern.CASE_INSENSITIVE)),
            new Rule("data_exfiltration", Pattern.compile("\\b(reveal|print|show|dump|expose)\\b.{0,40}\\b(prompt|hidden|system|secret|key|password|token)\\b", Pattern.CASE_INSENSITIVE)),
            new Rule("jailbreak_marker", Pattern.compile("\\b(jailbreak|do anything now|dan)\\b", Pattern.CASE_INSENSITIVE))
    );

    public String detectReason(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        for (Rule rule : RULES) {
            if (rule.pattern().matcher(normalized).find()) {
                return rule.reason();
            }
        }
        return null;
    }

    private record Rule(String reason, Pattern pattern) {
    }
}
