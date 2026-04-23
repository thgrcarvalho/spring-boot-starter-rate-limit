# Changelog

## [0.2.0] — 2026-04-23

### Added
- **`RedisRateLimitStore`** — distributed token-bucket backend for multi-instance deployments.
  Uses an atomic Lua script (`EVAL`) so check-and-decrement is a single Redis operation —
  no race conditions between instances. Wire it up with a single `@Bean` method.
- Constructor overload `RedisRateLimitStore(factory, keyPrefix)` for custom key namespacing.

## [0.1.0] — 2026-04-23

Initial release.

- `@RateLimit` annotation with configurable `requests`, `window`, and `keyStrategy`
- `IP` and `IP_AND_PATH` key strategies
- `InMemoryRateLimitStore` with CAS thread safety (single-instance)
- Pluggable `RateLimitStore` port — bring your own Redis or database backend
- Spring Boot autoconfiguration, no `@EnableXxx` annotation required
