package com.aichecker.security;

import com.aichecker.dto.ApiMessageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";

    private final ObjectMapper objectMapper;
    private final Map<String, CounterWindow> counters = new ConcurrentHashMap<>();

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.rate-limit.auth-requests-per-minute:60}")
    private int authRequestsPerMinute;

    public AuthRateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled || !isAuthRoute(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        long minute = System.currentTimeMillis() / 60000L;
        String key = request.getServletPath() + "|" + clientId(request);

        CounterWindow counter = counters.compute(key, (ignored, existing) -> {
            if (existing == null || existing.minute != minute) {
                return new CounterWindow(minute, 1);
            }
            existing.count += 1;
            return existing;
        });

        if (counter.count > authRequestsPerMinute) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(
                    new ApiMessageResponse("Too many authentication attempts. Try again in a minute.")
            ));
            return;
        }

        if (counters.size() > 5000) {
            cleanupOldEntries(minute);
        }

        filterChain.doFilter(request, response);
    }

    private void cleanupOldEntries(long currentMinute) {
        counters.entrySet().removeIf(entry -> entry.getValue().minute < currentMinute - 2);
    }

    private boolean isAuthRoute(HttpServletRequest request) {
        String path = request.getServletPath();
        return LOGIN_PATH.equals(path) || REGISTER_PATH.equals(path);
    }

    private String clientId(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class CounterWindow {
        long minute;
        int count;

        CounterWindow(long minute, int count) {
            this.minute = minute;
            this.count = count;
        }
    }
}
