package com.companyname.ragassistant.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(0)
public class RequestIdFilter implements Filter {
    private static final String HEADER = "X-Request-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String requestId = null;
        if (request instanceof HttpServletRequest req) {
            String incoming = req.getHeader(HEADER);
            if (incoming != null) {
                String trimmed = incoming.trim();
                if (!trimmed.isEmpty() && trimmed.length() <= 128) {
                    requestId = trimmed;
                }
            }
        }
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put("request_id", requestId);
        try {
            if (response instanceof HttpServletResponse res) {
                res.setHeader(HEADER, requestId);
            }
            chain.doFilter(request, response);
        } finally {
            MDC.remove("request_id");
        }
    }
}
