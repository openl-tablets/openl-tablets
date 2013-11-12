package org.openl.rules.runtime;

import org.openl.OpenL;
import org.openl.dependency.loader.FileDependencyLoader;

/**
 * Dependency loader for excel based files.
 * 
 */
public class RulesFileDependencyLoader extends FileDependencyLoader {

    public static final String RULE_OPENL_NAME = OpenL.OPENL_JAVA_RULE_NAME;

    public RulesFileDependencyLoader() {
        super(RULE_OPENL_NAME);
    }

}
