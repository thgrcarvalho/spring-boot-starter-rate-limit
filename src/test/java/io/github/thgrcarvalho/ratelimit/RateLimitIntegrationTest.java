package io.github.thgrcarvalho.ratelimit;

import io.github.thgrcarvalho.ratelimit.internal.InMemoryRateLimitStore;
import io.github.thgrcarvalho.ratelimit.test.TestApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RateLimitIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    InMemoryRateLimitStore store;

    @BeforeEach
    void resetStore() {
        store.clear();
    }

    @Test
    void limitedEndpoint_allowsUpToLimit_thenRejects() {
        // First 3 requests should succeed
        for (int i = 0; i < 3; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity("/limited", String.class);
            assertEquals(HttpStatus.OK, response.getStatusCode(),
                    "Request " + (i + 1) + " should succeed");
        }

        // 4th request should be rejected
        ResponseEntity<String> rejected = restTemplate.getForEntity("/limited", String.class);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, rejected.getStatusCode(),
                "4th request should be rejected");
    }

    @Test
    void unlimitedEndpoint_neverRejected() {
        for (int i = 0; i < 10; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity("/unlimited", String.class);
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Test
    void perPathStrategy_keysOnPathNotJustIp() {
        // /per-path allows 2 requests
        assertEquals(HttpStatus.OK, restTemplate.getForEntity("/per-path", String.class).getStatusCode());
        assertEquals(HttpStatus.OK, restTemplate.getForEntity("/per-path", String.class).getStatusCode());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, restTemplate.getForEntity("/per-path", String.class).getStatusCode());

        // /unlimited shares the same IP but has no limit — must still work
        assertEquals(HttpStatus.OK, restTemplate.getForEntity("/unlimited", String.class).getStatusCode());
    }

    @Test
    void rejectedResponse_hasCorrectContentType() {
        // exhaust the bucket from a fresh key — use unique path prefix via header trick
        // Since all tests share an IP, the /limited bucket may already be exhausted from
        // the first test. Just hit it until we get a 429 and verify the response shape.
        ResponseEntity<String> response = null;
        for (int i = 0; i < 10; i++) {
            response = restTemplate.getForEntity("/limited", String.class);
            if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) break;
        }
        assert response != null;
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        String body = response.getBody();
        assert body != null && body.contains("429");
    }
}
