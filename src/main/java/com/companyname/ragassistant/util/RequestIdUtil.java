package com.companyname.ragassistant.util;

import org.slf4j.MDC;

public final class RequestIdUtil {
    private static final String KEY = "request_id";

    private RequestIdUtil() {
    }

    public static String current() {
        String value = MDC.get(KEY);
        return value == null ? "" : value;
    }
}
