package org.openl.rules.repository;

import java.util.Objects;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

/**
 * The files of a project that are kept apart from its rules, by Ant-style patterns relative to the root of the
 * project: see {@link PathPatterns}.
 *
 * <p>A <b>protected</b> file, such as {@code pom.xml}, is read, edited and merged like any other file, but it is not
 * moved with the project: an export, a deployment and an import of the project leave it out, and an import that
 * replaces the project keeps the protected files the project holds. A revision that changes only protected files is
 * a technical revision.
 *
 * <p>A <b>hidden</b> file, such as {@code rules.xml}, is a file a business user does not need to see. A protected
 * file is hidden too. Being hidden changes nothing but the screens of OpenL Studio, which leave such a file out unless
 * the user asks to see it.
 *
 * @author Yury Molchan
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProjectFiles {

    /** The application setting that lists the protected files of every project, separated by commas. */
    public static final String PROTECTED_PROPERTY = "project.protected-files";

    /** The application setting that lists the hidden files of every project, separated by commas. */
    public static final String HIDDEN_PROPERTY = "project.hidden-files";

    private final PathPatterns protectedFiles;
    /** The hidden and the protected files together. */
    private final PathPatterns hiddenFiles;

    /**
     * The files the settings list.
     *
     * @param protectedFiles the patterns of the protected files separated by commas, or {@code null} for none
     * @param hiddenFiles    the patterns of the hidden files separated by commas, or {@code null} for none
     */
    public static ProjectFiles of(@Nullable String protectedFiles, @Nullable String hiddenFiles) {
        return new ProjectFiles(PathPatterns.of(protectedFiles),
                PathPatterns.of(Objects.toString(protectedFiles, "") + "," + Objects.toString(hiddenFiles, "")));
    }

    /**
     * Tells whether a file is protected.
     *
     * @param path the path of the file relative to the root of its project
     */
    public boolean isProtected(String path) {
        return protectedFiles.matches(path);
    }

    /**
     * Tells whether a file is hidden: either listed as hidden or protected.
     *
     * @param path the path of the file relative to the root of its project
     */
    public boolean isHidden(String path) {
        return hiddenFiles.matches(path);
    }
}
