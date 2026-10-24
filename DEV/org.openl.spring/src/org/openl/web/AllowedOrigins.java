package org.openl.web;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.openl.util.StringUtils;

/**
 * The origins the {@code cors.allowed.origins} setting names as trusted, read alike by OpenL Studio and
 * OpenL Rule Services.
 *
 * <p>An origin is matched case-insensitively. An asterisk trusts every origin. A blank or missing
 * setting trusts none. A {@code null} origin — a request that carries no {@code Origin} header — is
 * never trusted.
 *
 * @param origins the listed origins, in lower case
 * @param any     whether the setting is an asterisk
 * @author Yury Molchan
 */
public record AllowedOrigins(Set<String> origins, boolean any) {

    /** Reads the comma-separated setting. Spaces around an entry are ignored. */
    public static AllowedOrigins parse(@Nullable String value) {
        if (StringUtils.isBlank(value)) {
            return new AllowedOrigins(Set.of(), false);
        }
        if ("*".equals(value.trim())) {
            return new AllowedOrigins(Set.of(), true);
        }
        var origins = Stream.of(StringUtils.split(value, ','))
                .map(origin -> origin.trim().toLowerCase(Locale.ROOT))
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        return new AllowedOrigins(origins, false);
    }

    public boolean contains(@Nullable String origin) {
        return origin != null && (any || origins.contains(origin.toLowerCase(Locale.ROOT)));
    }

    /** Whether no origin is trusted at all, so that no response depends on the {@code Origin} header. */
    public boolean isEmpty() {
        return !any && origins.isEmpty();
    }
}
