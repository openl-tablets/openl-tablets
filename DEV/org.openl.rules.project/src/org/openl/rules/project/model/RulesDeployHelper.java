package org.openl.rules.project.model;

import java.util.HashSet;
import java.util.Set;

public final class RulesDeployHelper {
    private RulesDeployHelper() {
    }

    public static Set<String> splitRootClassNamesBindingClasses(String rootClassNamesBinding) {
        if (rootClassNamesBinding != null) {
            var rootClasses = rootClassNamesBinding.split(",", -1);
            var rootClassNamesBindingClasses = new HashSet<String>();
            for (String className : rootClasses) {
                if (className != null && className.trim().length() > 0) {
                    rootClassNamesBindingClasses.add(className.trim());
                }
            }
            return rootClassNamesBindingClasses;
        } else {
            return Set.of();
        }
    }

}
