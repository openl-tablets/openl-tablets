package org.openl.rules.repository;

import static org.apache.commons.lang3.StringUtils.strip;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.util.AntPathMatcher;

import org.openl.util.StringUtils;

/**
 * A list of Ant-style patterns a setting holds, separated by commas, such as {@code **}{@code /target/**, pom.xml}.
 *
 * <p>A pattern is matched against a relative path, its folders separated by {@code /}. The letter case is ignored,
 * since a file system of Windows or macOS opens {@code POM.XML} as {@code pom.xml}. A pattern that ends with
 * {@code /**} matches the folder itself too.
 *
 * <p>A slash at the start or at the end of a path is not part of it: {@code /pom.xml}, the name a ZIP archive opened
 * as a file system gives, and {@code pom.xml} are the same path.
 *
 * @author Yury Molchan
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class PathPatterns {

    /** The patterns of a blank setting: no path matches them. */
    public static final PathPatterns NONE = new PathPatterns(List.of());

    // The patterns and the paths are matched in lower case, so the matcher rejects most paths at their first folder.
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private final List<String> patterns;

    /**
     * The patterns a setting lists.
     *
     * @param setting the patterns separated by commas; blank or {@code null} lists none
     */
    public static PathPatterns of(@Nullable String setting) {
        if (StringUtils.isBlank(setting)) {
            return NONE;
        }
        var patterns = StringUtils.split(setting, ',');
        return new PathPatterns(Arrays.stream(patterns).map(PathPatterns::normalized).toList());
    }

    /**
     * Tells whether a path matches any of the patterns.
     *
     * @param path a relative path, its folders separated by {@code /}
     */
    public boolean matches(String path) {
        var name = normalized(path);
        for (var pattern : patterns) {
            if (MATCHER.match(pattern, name)) {
                return true;
            }
        }
        return false;
    }

    private static String normalized(String path) {
        return strip(path, "/").toLowerCase(Locale.ROOT);
    }
}
