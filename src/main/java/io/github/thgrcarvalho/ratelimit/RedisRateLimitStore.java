package io.github.thgrcarvalho.ratelimit;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.ReturnType;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Redis-backed {@link RateLimitStore} using an atomic Lua script for true
 * token-bucket semantics across multiple instances.
 *
 * <p>The Lua script runs as a single atomic operation: it initialises the bucket
 * on first request and decrements it on each subsequent call, returning 1 (allowed)
 * or 0 (denied). Because Redis executes Lua scripts atomically, there are no
 * race conditions between check and decrement.</p>
 *
 * <p>Wire it up to activate it — the in-memory fallback backs off automatically:</p>
 *
 * <pre>{@code
 * @Bean
 * RateLimitStore rateLimitStore(RedisConnectionFactory connectionFactory) {
 *     return new RedisRateLimitStore(connectionFactory);
 * }
 * }</pre>
 */
public final class RedisRateLimitStore implements RateLimitStore {

    private static final String DEFAULT_KEY_PREFIX = "ratelimit:";

    /**
     * Atomic Lua script: initialises the bucket on first use (with TTL), then
     * decrements and returns 1 (allowed) or 0 (bucket empty / denied).
     */
    private static final byte[] SCRIPT = """
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local current = redis.call('GET', key)
            if current == false then
                redis.call('SET', key, limit - 1, 'EX', window)
                return 1
            end
            current = tonumber(current)
            if current > 0 then
                redis.call('DECR', key)
                return 1
            end
            return 0
            """.getBytes(StandardCharsets.UTF_8);

    private final RedisConnectionFactory connectionFactory;
    private final String keyPrefix;

    /**
     * Creates a store using the default key prefix {@code "ratelimit:"}.
     *
     * @param connectionFactory the Redis connection factory to use
     */
    public RedisRateLimitStore(RedisConnectionFactory connectionFactory) {
        this(connectionFactory, DEFAULT_KEY_PREFIX);
    }

    /**
     * Creates a store with a custom key prefix.
     *
     * @param connectionFactory the Redis connection factory to use
     * @param keyPrefix         prefix prepended to every Redis key
     */
    public RedisRateLimitStore(RedisConnectionFactory connectionFactory, String keyPrefix) {
        this.connectionFactory = connectionFactory;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public boolean tryConsume(String key, int limit, Duration window) {
        try (RedisConnection conn = connectionFactory.getConnection()) {
            Long result = conn.scriptingCommands().eval(
                    SCRIPT,
                    ReturnType.INTEGER,
                    1,
                    redisKey(key),
                    String.valueOf(limit).getBytes(StandardCharsets.UTF_8),
                    String.valueOf(window.getSeconds()).getBytes(StandardCharsets.UTF_8)
            );
            return result != null && result == 1L;
        }
    }

    private byte[] redisKey(String key) {
        return (keyPrefix + key).getBytes(StandardCharsets.UTF_8);
    }
}
