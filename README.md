# spring-boot-starter-rate-limit

[![CI](https://github.com/thgrcarvalho/spring-boot-starter-rate-limit/actions/workflows/ci.yml/badge.svg)](https://github.com/thgrcarvalho/spring-boot-starter-rate-limit/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.thgrcarvalho/spring-boot-starter-rate-limit)](https://central.sonatype.com/artifact/io.github.thgrcarvalho/spring-boot-starter-rate-limit)
[![codecov](https://codecov.io/gh/thgrcarvalho/spring-boot-starter-rate-limit/branch/main/graph/badge.svg)](https://codecov.io/gh/thgrcarvalho/spring-boot-starter-rate-limit)

A Spring Boot starter that adds token-bucket rate limiting to any controller method with a single annotation.

```java
@PostMapping("/payments")
@RateLimit(requests = 10, window = "1m")
public ResponseEntity<PaymentResponse> charge(@RequestBody ChargeRequest request) {
    return ResponseEntity.ok(paymentService.charge(request));
}
```

Clients that exceed the limit receive HTTP 429 Too Many Requests. No code changes required beyond the annotation.

## Installation

**Gradle:**
```groovy
dependencies {
    implementation 'io.github.thgrcarvalho:spring-boot-starter-rate-limit:0.2.0'
}
```

**Maven:**
```xml
<dependency>
    <groupId>io.github.thgrcarvalho</groupId>
    <artifactId>spring-boot-starter-rate-limit</artifactId>
    <version>0.2.0</version>
</dependency>
```

The starter auto-configures on any `@SpringBootApplication` with Spring Web on the classpath. No `@EnableXxx` annotation needed.

## Annotation options

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `requests` | `int` | `60` | Maximum requests per window |
| `window` | `String` | `"1m"` | Window duration: `s`, `m`, `h`, `d` |
| `keyStrategy` | `KeyStrategy` | `IP` | How to key the bucket |

### Key strategies

- **`IP`** — one bucket per client IP. Best for general endpoint protection.
- **`IP_AND_PATH`** — one bucket per IP + method + path. Use when you need per-endpoint limits that don't interfere with each other.

## Storage backends

### In-memory (default)

A `ConcurrentHashMap` — zero configuration, suitable for single-instance deployments. Uses atomic compare-and-swap for thread safety.

### Redis (multi-instance)

A distributed token-bucket backed by Redis. Uses an atomic Lua script so check-and-decrement is a single Redis operation — no race conditions between pods.

```java
@Bean
RateLimitStore rateLimitStore(RedisConnectionFactory connectionFactory) {
    return new RedisRateLimitStore(connectionFactory);
}
```

The autoconfiguration backs off automatically once you declare a bean of type `RateLimitStore`.

### Custom backends

Implement `RateLimitStore` yourself and register it as a bean — same back-off behaviour applies.

## Running tests

```bash
./gradlew test
```

## Tech

Java 21 · Spring Boot 3 (autoconfigure, web) · Gradle · JUnit 5
