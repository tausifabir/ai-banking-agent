package com.aibankingagent.ai_banking_agent.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Stamps every request with an {@code X-Request-Id} (generating one if the
 * client didn't supply it), puts the same value on the response header,
 * surfaces it on the {@link HttpServletRequest} as the {@code requestId}
 * attribute, and pushes it into SLF4J's MDC so logs from downstream
 * services carry the same correlation id.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String ATTR = "requestId";
    public static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String existing = request.getHeader(HEADER);
        String requestId = (existing == null || existing.isBlank())
                ? UUID.randomUUID().toString()
                : existing.trim();

        request.setAttribute(ATTR, requestId);
        response.setHeader(HEADER, requestId);
        MDC.put(MDC_KEY, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
