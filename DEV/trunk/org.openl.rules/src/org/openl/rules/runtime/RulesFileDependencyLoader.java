package org.openl.rules.runtime;

import org.openl.dependency.loader.FileDependencyLoader;

public class RulesFileDependencyLoader extends FileDependencyLoader {

    public static final String RULE_OPENL_NAME = "org.openl.xls";

    public RulesFileDependencyLoader() {
        super(RULE_OPENL_NAME);
    }
    
}
