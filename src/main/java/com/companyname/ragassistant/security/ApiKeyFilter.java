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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.companyname.ragassistant.util.HashUtil;

import java.io.IOException;

/**
 * Optional API-key gate. If env var RAG_API_KEY is not set, this is a no-op.
 * If set, requires header: X-API-Key: <value>
 */
@Component
@Order(1)
public class ApiKeyFilter implements Filter {
    @Value("${app.security.api-key:}")
    private String required;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        MDC.put("actor", "anonymous");
        try {
            if (required == null || required.isBlank()) {
                chain.doFilter(request, response);
                return;
            }

            HttpServletRequest req = (HttpServletRequest) request;
            String path = req.getRequestURI();
            if (path.startsWith("/actuator/health")
                    || path.startsWith("/liveness")
                    || path.startsWith("/readiness")
                    || path.startsWith("/health")
                    || path.startsWith("/swagger-ui")
                    || path.startsWith("/v3/api-docs")) {
                chain.doFilter(request, response);
                return;
            }
            String got = req.getHeader(SecurityConstants.API_KEY_HEADER);

            if (required.equals(got)) {
                String hashPrefix = HashUtil.sha256(got).substring(0, 6);
                MDC.put("actor", "apiKey:" + hashPrefix);
                chain.doFilter(request, response);
                return;
            }

            HttpServletResponse res = (HttpServletResponse) response;
            res.setStatus(401);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"unauthorized\"}");
        } finally {
            MDC.remove("actor");
        }
    }
}
