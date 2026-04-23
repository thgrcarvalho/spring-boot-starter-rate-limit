package io.github.thgrcarvalho.ratelimit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RedisRateLimitStoreTest {

    private static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static RedisRateLimitStore store;

    @BeforeAll
    static void start() {
        redis.start();
        connectionFactory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        store = new RedisRateLimitStore(connectionFactory, "test-ratelimit:");
    }

    @AfterAll
    static void stop() {
        if (connectionFactory != null) connectionFactory.destroy();
        redis.stop();
    }

    @Test
    void consumesUpToLimit_thenRejects() {
        String key = "key-basic-" + System.nanoTime();
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
        String key = "key-ttl-" + System.nanoTime();
        assertTrue(store.tryConsume(key, 1, Duration.ofSeconds(1)));
        assertFalse(store.tryConsume(key, 1, Duration.ofSeconds(1)));

        Thread.sleep(1500);

        assertTrue(store.tryConsume(key, 1, Duration.ofSeconds(1)),
                "bucket should reset after TTL expires");
    }

    @Test
    void concurrentRequests_exactlyLimitAllowed() throws Exception {
        String key = "key-concurrent-" + System.nanoTime();
        int limit = 5;
        int threads = 20;

        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        AtomicInteger allowed = new AtomicInteger();

        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                go.await();
                if (store.tryConsume(key, limit, Duration.ofMinutes(1))) {
                    allowed.incrementAndGet();
                }
                return null;
            }));
        }

        ready.await();
        go.countDown();
        for (Future<Void> f : futures) f.get();
        executor.shutdown();

        assertEquals(limit, allowed.get(),
                "exactly " + limit + " concurrent requests should be allowed");
    }
}
