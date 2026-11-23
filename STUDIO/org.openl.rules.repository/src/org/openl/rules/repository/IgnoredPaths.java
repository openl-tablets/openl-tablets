package org.openl.rules.repository;

import java.util.Objects;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

/**
 * The paths a repository never shows and never changes: files that are not rules, such as the files of a version
 * control system, the output of a build or the temporary files of an operating system or an editor.
 *
 * <p>This is the one place such files are told apart. A path is an Ant-style pattern relative to the root of the
 * repository, such as {@code **}{@code /.DS_Store}: see {@link PathPatterns}.
 *
 * <p>The {@code .git} folder, at any depth, is always ignored, whatever the setting holds. It keeps the
 * configuration and the hooks Git runs, and the {@code .git} file of a Git worktree points Git to another folder.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class IgnoredPaths {

    /** The system setting that lists the ignored paths, separated by commas. */
    public static final String PROPERTY = "repository.ignored-paths";

    private static final String GIT_FOLDER = "**/.git/**";

    private final PathPatterns patterns;

    /**
     * The ignored paths a setting lists.
     *
     * @param setting the patterns separated by commas; blank or {@code null} ignores the {@code .git} folder only
     */
    public static IgnoredPaths of(@Nullable String setting) {
        return new IgnoredPaths(PathPatterns.of(GIT_FOLDER + "," + Objects.toString(setting, "")));
    }

    /**
     * Tells whether a path is ignored.
     *
     * @param path a path relative to the root of the repository, its folders separated by {@code /}; a folder may
     *             end with {@code /}
     */
    public boolean matches(String path) {
        return patterns.matches(path);
    }
}
