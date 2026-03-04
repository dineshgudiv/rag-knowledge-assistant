package com.companyname.ragassistant.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class TextChunker {

    private static final int CHUNK_SIZE = 1000;
    private static final int OVERLAP = 120;
    public static final int MAX_DOC_CHARS = 8_000_000;
    public static final int MAX_CHUNKS = 10_000;
    public static final int PREVIEW_NORM_MAX_CHARS = 200_000;
    public static final int TAIL_WINDOW_CHARS = 300_000;
    private static final Pattern EMAIL = Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(?:\\+?\\d{1,3}[\\s.-]?)?(?:\\(?\\d{3}\\)?[\\s.-]?)\\d{3}[\\s.-]?\\d{4}(?!\\d)");
    private static final Pattern DOB = Pattern.compile("(?i)\\b(?:dob|date\\s*of\\s*birth)\\b|\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b");

    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        for (Segment segment : segments(text, CHUNK_SIZE, OVERLAP)) {
            chunks.add(segment.text());
        }
        return chunks;
    }

    public List<Segment> segments(String text, int chunkSize, int overlap) {
        String cleaned = normalizeExtractedText(text);
        if (cleaned.length() > MAX_DOC_CHARS) {
            cleaned = cleaned.substring(0, MAX_DOC_CHARS);
        }

        List<Segment> segments = new ArrayList<>();
        if (cleaned.isEmpty()) {
            return segments;
        }

        int safeSize = Math.max(800, chunkSize);
        int safeOverlap = Math.max(0, Math.min(overlap, Math.max(1, safeSize / 2)));
        int length = cleaned.length();
        int start = 0;

        while (start < length && segments.size() < MAX_CHUNKS) {
            int end = Math.min(length, start + safeSize);
            if (end < length) {
                end = snapForwardBoundary(cleaned, end, Math.min(length, end + 120));
            }
            if (end <= start) {
                end = Math.min(length, start + safeSize);
                if (end <= start) {
                    break;
                }
            }

            String value = cleaned.substring(start, end).trim();
            if (!value.isEmpty()) {
                segments.add(new Segment(start, end, value));
            }

            if (end >= length) {
                break;
            }

            int nextStart = Math.max(0, end - safeOverlap);
            nextStart = snapBackwardBoundary(cleaned, nextStart, start);
            if (nextStart <= start) {
                nextStart = end;
            }
            start = nextStart;
        }

        return segments;
    }

    public String clean(String text) {
        return normalizeExtractedText(text);
    }

    public String normalizeExtractedText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        int cap = Math.min(text.length(), MAX_DOC_CHARS);
        StringBuilder out = new StringBuilder(Math.min(cap, 512_000));
        boolean lastWasSpace = false;

        for (int i = 0; i < cap; i++) {
            char c = text.charAt(i);
            if (c == '\u0000') {
                continue;
            }
            if (isDroppedControl(c)) {
                continue;
            }
            if (c == '\r') {
                continue;
            }
            if (Character.isWhitespace(c)) {
                if (!lastWasSpace && out.length() > 0) {
                    out.append(' ');
                    lastWasSpace = true;
                }
                continue;
            }
            out.append(c);
            lastWasSpace = false;
        }
        return out.toString().trim();
    }

    public String supportSpan(String chunkText, String question, int minLen, int maxLen, boolean redactPii) {
        String normalized = normalizeForPreview(chunkText);
        if (normalized.isBlank()) {
            return "";
        }
        int safeMin = Math.max(250, minLen);
        int safeMax = Math.max(safeMin, maxLen);

        int center = bestCenter(normalized, question);
        int target = Math.min(safeMax, Math.max(safeMin, 300));
        int start = Math.max(0, center - (target / 2));
        int end = Math.min(normalized.length(), start + target);

        if (end - start < safeMin) {
            start = Math.max(0, end - safeMin);
        }

        start = snapWordStart(normalized, start);
        end = snapWordEnd(normalized, end);
        if (end <= start) {
            start = 0;
            end = Math.min(normalized.length(), safeMin);
        }

        String span = normalized.substring(start, end).trim();
        if (start == 0) {
            span = trimLeadingPartialWord(span);
        }
        if (start > 0) {
            span = "..." + span;
        }
        if (end < normalized.length()) {
            span = span + "...";
        }
        return redactPii ? redactPreview(span) : span;
    }

    private int bestCenter(String content, String question) {
        if (question == null || question.isBlank()) {
            return content.length() / 2;
        }
        String lower = content.toLowerCase(Locale.ROOT);
        for (String token : tokenize(question)) {
            if (token.length() < 3) {
                continue;
            }
            int idx = lower.indexOf(token);
            if (idx >= 0) {
                return idx + (token.length() / 2);
            }
        }
        return content.length() / 2;
    }

    private List<String> tokenize(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String cleaned = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.isBlank()) {
            return List.of();
        }
        return List.of(cleaned.split(" "));
    }

    private int snapWordStart(String text, int idx) {
        int i = Math.max(0, Math.min(idx, text.length() - 1));
        while (i > 0 && Character.isLetterOrDigit(text.charAt(i)) && Character.isLetterOrDigit(text.charAt(i - 1))) {
            i--;
        }
        while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
            i++;
        }
        return i;
    }

    private int snapWordEnd(String text, int idx) {
        int i = Math.max(1, Math.min(idx, text.length()));
        while (i < text.length() && Character.isLetterOrDigit(text.charAt(i - 1)) && Character.isLetterOrDigit(text.charAt(i))) {
            i++;
        }
        while (i > 0 && Character.isWhitespace(text.charAt(i - 1))) {
            i--;
        }
        return i;
    }

    String tailByWords(String value, int maxChars) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int tailStart = Math.max(0, value.length() - TAIL_WINDOW_CHARS);
        String window = value.substring(tailStart);
        String normalized = normalizeForPreview(window);
        if (normalized.length() <= maxChars) {
            return normalized;
        }
        int start = normalized.length() - maxChars;
        start = snapWordStart(normalized, start);
        return normalized.substring(start).trim();
    }

    static String normalizeForPreview(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int start = 0;
        int end = value.length();
        if (end - start > PREVIEW_NORM_MAX_CHARS) {
            end = PREVIEW_NORM_MAX_CHARS;
        }

        StringBuilder sb = new StringBuilder(Math.min(end - start, PREVIEW_NORM_MAX_CHARS));
        boolean lastWasSpace = false;
        for (int i = start; i < end; i++) {
            char c = value.charAt(i);
            if (c == '\u0000') {
                continue;
            }
            if (isDroppedControl(c)) {
                continue;
            }
            if (Character.isWhitespace(c)) {
                if (!lastWasSpace && sb.length() > 0) {
                    sb.append(' ');
                    lastWasSpace = true;
                }
                continue;
            }
            sb.append(c);
            lastWasSpace = false;
        }
        return sb.toString().trim();
    }

    private static boolean isDroppedControl(char c) {
        return (c >= 0x00 && c <= 0x08) || c == 0x0B || c == 0x0C || (c >= 0x0E && c <= 0x1F);
    }

    private String trimLeadingPartialWord(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        if (!Character.isLetter(text.charAt(0)) || Character.isUpperCase(text.charAt(0))) {
            return text;
        }
        int firstSpace = text.indexOf(' ');
        if (firstSpace <= 0 || firstSpace >= text.length() - 1) {
            return text;
        }
        String trimmed = text.substring(firstSpace + 1).trim();
        return trimmed.isBlank() ? text : trimmed;
    }

    private String redactPreview(String text) {
        String out = EMAIL.matcher(text).replaceAll("[REDACTED_EMAIL]");
        out = PHONE.matcher(out).replaceAll("[REDACTED_PHONE]");
        out = DOB.matcher(out).replaceAll("[REDACTED_DOB]");
        return out;
    }

    private int snapForwardBoundary(String text, int idx, int max) {
        int i = Math.max(0, Math.min(idx, text.length()));
        int upper = Math.max(i, Math.min(max, text.length()));
        while (i < upper) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?' || Character.isWhitespace(c)) {
                return i + 1;
            }
            i++;
        }
        return Math.min(idx, text.length());
    }

    private int snapBackwardBoundary(String text, int idx, int min) {
        int i = Math.max(0, Math.min(idx, text.length()));
        int lower = Math.max(0, min);
        while (i > lower) {
            char c = text.charAt(i - 1);
            if (c == '.' || c == '!' || c == '?' || Character.isWhitespace(c)) {
                return i;
            }
            i--;
        }
        return Math.max(lower, Math.min(idx, text.length()));
    }

    public record Segment(int startOffset, int endOffset, String text) {
    }
}
