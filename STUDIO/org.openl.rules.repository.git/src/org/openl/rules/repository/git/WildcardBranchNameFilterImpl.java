package org.openl.rules.repository.git;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Wildcard branch name filter.
 *
 * <table class="striped" style="text-align:left; margin-left:2em">
 * <caption style="display:none">Pattern Language</caption> <thead>
 * <tr>
 * <th scope="col">Example
 * <th scope="col">Description
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <th scope="row">{@code *}</th>
 * <td>Matches simple branch names like {@code master}, {@code release-21.10}. If a branch name has a path separator, it
 * will be skipped</td>
 * </tr>
 * <tr>
 * <th scope="row">{@code **}</th>
 * <td>Matches all branches</td>
 * </tr>
 * <tr>
 * <th scope="row">{@code *.*}</th>
 * <td>Matches simple branches containing a dot</td>
 * </tr>
 * <tr>
 * <th scope="row">{@code *.{10,11}}</th>
 * <td>Matches branch ending with {@code .10} or {@code .11}</td>
 * </tr>
 * <tr>
 * <th scope="row">{@code foo.?}</th>
 * <td>Matches branch name starting with {@code foo.} and a single character after dot</td>
 * </tr>
 * <tr>
 * <th scope="row"><code>&#47;releases&#47;*</code>
 * <td>Matches <code>&#47;releases&#47;21.10</code> on UNIX platforms</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Vladyslav Pikus
 * @since 5.25.0
 */
final class WildcardBranchNameFilterImpl implements WildcardBranchNameFilter {

    private final List<PathMatcher> matchers;

    /**
     * @param patterns branch name pattern list
     * @see FileSystem#getPathMatcher(String)
     */
    WildcardBranchNameFilterImpl(String... patterns) {
        Objects.requireNonNull(patterns, "Branch name pattern list cannot be null.");
        final FileSystem fs = FileSystems.getDefault();
        matchers = Stream.of(patterns)
            .map(pattern -> fs.getPathMatcher("glob:" + pattern))
            .collect(Collectors.toList());
        if (matchers.isEmpty()) {
            throw new IllegalArgumentException("Branch name pattern list cannot be empty.");
        }
    }

    @Override
    public boolean test(String branch) {
        Path p = Paths.get(branch);
        return matchers.stream().anyMatch(matcher -> matcher.matches(p));
    }

}
