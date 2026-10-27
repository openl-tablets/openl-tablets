package org.openl.rules.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.util.AntPathMatcher;

import org.openl.util.StringUtils;

/**
 * The paths a repository never shows and never changes: files that are not rules, such as the build files of a
 * project or the files of a version control system.
 *
 * <p>A path is an Ant-style pattern relative to the root of the repository, such as {@code **}{@code /pom.xml}.
 * The letter case is ignored, since a file system of Windows or macOS opens {@code .GIT} as {@code .git}. A pattern
 * that ends with {@code /**} matches the folder itself too.
 *
 * <p>The {@code .git} folder, at any depth, is always ignored, whatever the setting holds. It keeps the
 * configuration and the hooks Git runs, and the {@code .git} file of a Git worktree points Git to another folder.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class IgnoredPaths {

    /** The system setting that lists the ignored paths, separated by commas. */
    public static final String PROPERTY = "repository.ignored-paths";

    private static final String GIT_FOLDER = "**/.git/**";

    // The patterns and the paths are matched in lower case, so the matcher rejects most paths at their first folder.
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private final List<String> patterns;

    /**
     * The ignored paths a setting lists.
     *
     * @param setting the patterns separated by commas; blank or {@code null} ignores the {@code .git} folder only
     */
    public static IgnoredPaths of(@Nullable String setting) {
        var patterns = new ArrayList<String>();
        patterns.add(GIT_FOLDER);
        if (StringUtils.isNotBlank(setting)) {
            for (var pattern : StringUtils.split(setting, ',')) {
                patterns.add(pattern.toLowerCase(Locale.ROOT));
            }
        }
        return new IgnoredPaths(List.copyOf(patterns));
    }

    /**
     * Tells whether a path is ignored.
     *
     * @param path a path relative to the root of the repository, its folders separated by {@code /}; a folder may
     *             end with {@code /}
     */
    public boolean matches(String path) {
        var name = (path.endsWith("/") ? path.substring(0, path.length() - 1) : path).toLowerCase(Locale.ROOT);
        return patterns.stream().anyMatch(pattern -> MATCHER.match(pattern, name));
    }
}
