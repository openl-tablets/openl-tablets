package org.openl.rules.project.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class RulesDeployHelper {
    private RulesDeployHelper() {
    }

    public static Set<String> splitRootClassNamesBindingClasses(String rootClassNamesBinding) {
        if (rootClassNamesBinding != null) {
            var rootClasses = rootClassNamesBinding.split(",", -1);
            return Arrays.stream(rootClasses)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(className -> !className.isEmpty())
                    .collect(Collectors.toCollection(HashSet::new));
        } else {
            return Set.of();
        }
    }

}
