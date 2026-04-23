package io.github.thgrcarvalho.ratelimit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Spring MVC controller method as rate-limited using a token-bucket algorithm.
 *
 * <p>Each unique {@code key} (default: client IP) gets its own bucket. When the bucket
 * is empty, the request is rejected with HTTP 429 Too Many Requests.</p>
 *
 * <pre>{@code
 * @PostMapping("/payments")
 * @RateLimit(requests = 10, window = "1m")
 * public ResponseEntity<PaymentResponse> charge(@RequestBody ChargeRequest request) {
 *     return ResponseEntity.ok(paymentService.charge(request));
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * Maximum number of requests allowed within the {@link #window()}.
     *
     * @return the request limit, defaulting to {@code 60}
     */
    int requests() default 60;

    /**
     * Duration of the rate-limit window. Supported units: {@code s} (seconds),
     * {@code m} (minutes), {@code h} (hours). Examples: {@code "1m"}, {@code "1h"}.
     *
     * @return the window string, defaulting to {@code "1m"}
     */
    String window() default "1m";

    /**
     * Strategy for deriving the rate-limit key from the request.
     *
     * @return the key strategy, defaulting to {@link KeyStrategy#IP}
     */
    KeyStrategy keyStrategy() default KeyStrategy.IP;

    enum KeyStrategy {
        /** One bucket per client IP address. */
        IP,
        /** One bucket per IP + HTTP method + path — useful for per-endpoint limits. */
        IP_AND_PATH
    }
}
