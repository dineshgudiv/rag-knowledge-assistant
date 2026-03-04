package com.companyname.ragassistant.util;

import org.slf4j.MDC;

public final class ActorUtil {
    private static final String KEY = "actor";

    private ActorUtil() {
    }

    public static String current() {
        String value = MDC.get(KEY);
        return value == null || value.isBlank() ? "anonymous" : value;
    }
}
