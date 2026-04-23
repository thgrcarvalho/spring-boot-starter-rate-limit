package io.github.thgrcarvalho.ratelimit;

import java.time.Duration;

/**
 * Storage backend for rate-limit token buckets.
 *
 * <p>The default implementation is {@link io.github.thgrcarvalho.ratelimit.internal.InMemoryRateLimitStore},
 * suitable for single-instance deployments. For multi-instance environments, provide a
 * custom implementation backed by Redis:</p>
 *
 * <pre>{@code
 * @Bean
 * RateLimitStore rateLimitStore(RedisConnectionFactory connectionFactory) {
 *     return new RedisRateLimitStore(connectionFactory);
 * }
 * }</pre>
 */
public interface RateLimitStore {

    /**
     * Attempts to consume one token from the bucket identified by {@code key}.
     *
     * <p>If the bucket does not exist it is created with {@code limit} tokens and
     * one is consumed immediately. Buckets reset after {@code window} elapses
     * since the first request in the current window.</p>
     *
     * @param key    the rate-limit key (e.g. client IP or IP + path)
     * @param limit  maximum tokens per window
     * @param window duration of the token window
     * @return {@code true} if a token was consumed (request allowed),
     *         {@code false} if the bucket is empty (request should be rejected)
     */
    boolean tryConsume(String key, int limit, Duration window);
}
