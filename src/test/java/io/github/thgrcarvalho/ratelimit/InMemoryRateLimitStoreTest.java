package io.github.thgrcarvalho.ratelimit;

import io.github.thgrcarvalho.ratelimit.internal.InMemoryRateLimitStore;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRateLimitStoreTest {

    private final InMemoryRateLimitStore store = new InMemoryRateLimitStore();

    @Test
    void consumesUpToLimit_thenRejects() {
        String key = "test-" + System.nanoTime();
        assertTrue(store.tryConsume(key, 3, Duration.ofMinutes(1)));
        assertTrue(store.tryConsume(key, 3, Duration.ofMinutes(1)));
        assertTrue(store.tryConsume(key, 3, Duration.ofMinutes(1)));
        assertFalse(store.tryConsume(key, 3, Duration.ofMinutes(1)));
    }

    @Test
    void differentKeys_haveIndependentBuckets() {
        String key1 = "key1-" + System.nanoTime();
        String key2 = "key2-" + System.nanoTime();

        assertTrue(store.tryConsume(key1, 1, Duration.ofMinutes(1)));
        assertFalse(store.tryConsume(key1, 1, Duration.ofMinutes(1)));

        assertTrue(store.tryConsume(key2, 1, Duration.ofMinutes(1)));
    }

    @Test
    void expiredWindow_resetsTokens() throws InterruptedException {
        String key = "expired-" + System.nanoTime();
        assertTrue(store.tryConsume(key, 1, Duration.ofMillis(100)));
        assertFalse(store.tryConsume(key, 1, Duration.ofMillis(100)));

        Thread.sleep(150);

        assertTrue(store.tryConsume(key, 1, Duration.ofMillis(100)),
                "bucket should reset after window expires");
    }
}
