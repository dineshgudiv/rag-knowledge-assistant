package com.companyname.ragassistant.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextSanitizerTest {

    @Test
    void sanitizeForPgText_removesNulAndControlChars() {
        String input = "A\u0000B\u0001C\tD\nE\rF\u000BF\u000CG\u001FH";
        String out = TextSanitizer.sanitizeForPgText(input);
        assertEquals("ABC\tD\nE\rFFGH", out);
    }

    @Test
    void sanitizeForPgText_nullBecomesEmpty() {
        assertEquals("", TextSanitizer.sanitizeForPgText(null));
    }
}
