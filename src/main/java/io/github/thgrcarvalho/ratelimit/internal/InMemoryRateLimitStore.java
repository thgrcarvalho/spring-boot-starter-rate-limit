package io.github.thgrcarvalho.ratelimit.internal;

import io.github.thgrcarvalho.ratelimit.RateLimitStore;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class InMemoryRateLimitStore implements RateLimitStore {

    private record Bucket(AtomicInteger remaining, Instant windowStart, Duration window, int limit) {
        boolean isExpired() {
            return Instant.now().isAfter(windowStart.plus(window));
        }
    }

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public void clear() {
        buckets.clear();
    }

    @Override
    public boolean tryConsume(String key, int limit, Duration window) {
        Bucket bucket = buckets.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new Bucket(new AtomicInteger(limit), Instant.now(), window, limit);
            }
            return existing;
        });

        int current = bucket.remaining().get();
        while (current > 0) {
            if (bucket.remaining().compareAndSet(current, current - 1)) {
                return true;
            }
            current = bucket.remaining().get();
        }
        return false;
    }
}
