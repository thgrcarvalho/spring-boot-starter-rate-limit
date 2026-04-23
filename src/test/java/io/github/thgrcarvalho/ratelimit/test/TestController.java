package io.github.thgrcarvalho.ratelimit.test;

import io.github.thgrcarvalho.ratelimit.RateLimit;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/limited")
    @RateLimit(requests = 3, window = "1m")
    public ResponseEntity<String> limited() {
        return ResponseEntity.ok("ok");
    }

    @GetMapping("/unlimited")
    public ResponseEntity<String> unlimited() {
        return ResponseEntity.ok("ok");
    }

    @GetMapping("/per-path")
    @RateLimit(requests = 2, window = "1m", keyStrategy = RateLimit.KeyStrategy.IP_AND_PATH)
    public ResponseEntity<String> perPath() {
        return ResponseEntity.ok("ok");
    }
}
