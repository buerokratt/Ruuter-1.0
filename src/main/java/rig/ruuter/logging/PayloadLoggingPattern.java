package rig.ruuter.logging;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

class PayloadLoggingPattern {
    private static final String WILDCARD = "*";

    private final String configuration;
    private final String destination;
    private final String type;

    public static PayloadLoggingPattern fromString(String pattern) {
        pattern = requireNonNull(pattern, "Null pattern").trim();
        if (pattern.isBlank()) {
            throw new IllegalArgumentException("Empty pattern");
        }
        String[] parts = Stream.of(pattern.split("\\.", 3))
                .map(String::trim)
                .toArray(String[]::new);
        parts = Arrays.copyOf(parts, 3);
        return new PayloadLoggingPattern(parts[0], parts[1], parts[2]);
    }

    private PayloadLoggingPattern(String configuration, String destination, String type) {
        this.configuration = requireNonNullElse(configuration, WILDCARD);
        this.destination = requireNonNullElse(destination, WILDCARD);
        this.type = requireNonNullElse(type, WILDCARD).toLowerCase();
    }

    boolean matches(String configurationCode, String destination, PayloadType type) {
        return isMatch(this.configuration, configurationCode)
                && isMatch(this.destination, destination)
                && isMatch(this.type, type.name().toLowerCase());
    }

    private boolean isWildcard(String string) {
        return WILDCARD.equals(string);
    }

    private boolean isMatch(String pattern, String test) {
        return isWildcard(pattern) || pattern.equals(test);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PayloadLoggingPattern that = (PayloadLoggingPattern) o;
        return configuration.equals(that.configuration) && destination.equals(that.destination) && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configuration, destination, type);
    }
}
