package org.openl.rules.repository.git;

import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.jgit.lib.Constants;
import org.openl.util.StringUtils;

/**
 * @author Vladyslav Pikus
 * @since 5.25.0
 */
public interface WildcardBranchNameFilter extends Predicate<String> {

    WildcardBranchNameFilter NO_MATCH = (branch) -> false;
    WildcardBranchNameFilter MASTER = Constants.MASTER::equals;

    default boolean accept(String branch) {
        return test(branch);
    }

    static WildcardBranchNameFilter create(String... patterns) {
        if (patterns == null) {
            return WildcardBranchNameFilter.NO_MATCH;
        } else {
            String[] filtered = Stream.of(patterns)
                    .filter(StringUtils::isNotBlank)
                    .map(String::trim)
                    .toArray(String[]::new);
            if (filtered.length == 0) {
                return WildcardBranchNameFilter.NO_MATCH;
            } else {
                return new WildcardBranchNameFilterImpl(filtered);
            }
        }
    }

}
