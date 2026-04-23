package io.github.thgrcarvalho.ratelimit.internal;

import io.github.thgrcarvalho.ratelimit.RateLimit;
import io.github.thgrcarvalho.ratelimit.RateLimitStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

public final class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitStore store;

    public RateLimitInterceptor(RateLimitStore store) {
        this.store = store;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                              HttpServletResponse response,
                              Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod method)) return true;

        RateLimit annotation = method.getMethodAnnotation(RateLimit.class);
        if (annotation == null) return true;

        String key = buildKey(request, annotation);
        Duration window = DurationParser.parse(annotation.window());

        if (store.tryConsume(key, annotation.requests(), window)) {
            return true;
        }

        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Too Many Requests\",\"status\":429}");
        return false;
    }

    private String buildKey(HttpServletRequest request, RateLimit annotation) {
        String ip = getClientIp(request);
        return switch (annotation.keyStrategy()) {
            case IP -> ip;
            case IP_AND_PATH -> ip + ":" + request.getMethod() + ":" + request.getRequestURI();
        };
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
