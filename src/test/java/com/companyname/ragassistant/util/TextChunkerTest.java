package com.companyname.ragassistant.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextChunkerTest {

    private final TextChunker chunker = new TextChunker();

    @Test
    void normalizeForPreview_largeInput_isBounded() {
        String huge = "A".repeat(5_000_000) + "\u0000\u0001   tail";
        String normalized = TextChunker.normalizeForPreview(huge);
        assertTrue(normalized.length() <= TextChunker.PREVIEW_NORM_MAX_CHARS);
        assertFalse(normalized.contains("\u0000"));
    }

    @Test
    void tailByWords_largeInput_isBounded() {
        String huge = "word ".repeat(1_200_000);
        String tail = chunker.tailByWords(huge, 600);
        assertTrue(tail.length() <= 650);
        assertTrue(tail.contains("word"));
    }
}
