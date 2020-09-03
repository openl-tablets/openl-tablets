package org.openl.rules.project.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class RulesDeployHelper {
    private RulesDeployHelper() {
    }

    public static Set<String> splitRootClassNamesBindingClasses(String rootClassNamesBinding) {
        if (rootClassNamesBinding != null) {
            String[] rootClasses = rootClassNamesBinding.split(",");
            Set<String> rootClassNamesBindingClasses = new HashSet<>();
            for (String className : rootClasses) {
                if (className != null && className.trim().length() > 0) {
                    rootClassNamesBindingClasses.add(className.trim());
                }
            }
            return rootClassNamesBindingClasses;
        } else {
            return Collections.emptySet();
        }
    }

}
