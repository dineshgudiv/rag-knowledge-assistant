package com.companyname.ragassistant.util;

public final class TextSanitizer {
    private TextSanitizer() {
    }

    public static String sanitizeForPgText(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\u0000') {
                continue;
            }
            if ((c >= 0x00 && c <= 0x08) || c == 0x0B || c == 0x0C || (c >= 0x0E && c <= 0x1F)) {
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }
}
