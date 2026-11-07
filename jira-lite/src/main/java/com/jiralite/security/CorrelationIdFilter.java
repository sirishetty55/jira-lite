package com.jiralite.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Filter that establishes an end-to-end request correlation ID for distributed tracing and observability.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Extracts the incoming {@code X-Correlation-ID} header if provided.</li>
 *   <li>Validates client-supplied correlation IDs against a strict pattern to prevent log injection (CRLF),
 *       header manipulation, or buffer overflow from maliciously long strings.</li>
 *   <li>Generates a fresh cryptographically random UUID if the header is absent or invalid.</li>
 *   <li>Injects the correlation ID into SLF4J's {@link MDC} and request attributes for logging and error reporting.</li>
 *   <li>Sets the {@code X-Correlation-ID} header on the HTTP response.</li>
 *   <li>Guarantees MDC cleanup in a {@code finally} block to prevent thread-pool pollution.</li>
 * </ul>
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String CORRELATION_ID_REQUEST_ATTR = "correlationId";

    /**
     * Permitted characters: alphanumeric, hyphen, underscore. Length: 1 to 64 characters.
     * Prevents log forging (CRLF injection), path manipulation, or excessively large headers.
     */
    private static final Pattern VALID_CORRELATION_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = resolveCorrelationId(request.getHeader(CORRELATION_ID_HEADER));

        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        request.setAttribute(CORRELATION_ID_REQUEST_ATTR, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    /**
     * Validates client-supplied correlation ID. If missing or invalid, generates a new UUID.
     */
    public String resolveCorrelationId(String headerValue) {
        if (StringUtils.hasText(headerValue)) {
            String trimmed = headerValue.trim();
            if (VALID_CORRELATION_ID_PATTERN.matcher(trimmed).matches()) {
                return trimmed;
            }
        }
        return UUID.randomUUID().toString();
    }
}

