package io.github.thgrcarvalho.ratelimit.internal;

import java.time.Duration;

final class DurationParser {

    private DurationParser() {}

    static Duration parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Duration must not be blank");
        }
        String trimmed = value.trim();
        char unit = trimmed.charAt(trimmed.length() - 1);
        long amount = Long.parseLong(trimmed, 0, trimmed.length() - 1, 10);
        return switch (unit) {
            case 's' -> Duration.ofSeconds(amount);
            case 'm' -> Duration.ofMinutes(amount);
            case 'h' -> Duration.ofHours(amount);
            case 'd' -> Duration.ofDays(amount);
            default -> throw new IllegalArgumentException("Unknown duration unit '" + unit + "' in: " + value);
        };
    }
}
