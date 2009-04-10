package org.openl.rules.runtime;

import java.io.File;
import java.net.URL;

import org.openl.runtime.EngineFactory;
import org.openl.runtime.EngineFactoryDefinition;

public class RuleEngineFactory<T> extends EngineFactory<T> {

    static public final String RULE_OPENL_NAME = "org.openl.xls";

    public RuleEngineFactory(EngineFactoryDefinition factoryDef, Class<T> engineInterface) {
        super(RULE_OPENL_NAME, factoryDef, engineInterface);
    }

    public RuleEngineFactory(File file, Class<T> engineInterface) {
        super(RULE_OPENL_NAME, file, engineInterface);
    }

    public RuleEngineFactory(String sourceFile, Class<T> engineInterface) {
        super(RULE_OPENL_NAME, sourceFile, engineInterface);
    }

    public RuleEngineFactory(String userHome, String sourceFile, Class<T> engineInterface) {
        super(RULE_OPENL_NAME, userHome, sourceFile, engineInterface);
    }

    public RuleEngineFactory(URL url, Class<T> engineInterface) {
        super(RULE_OPENL_NAME, url, engineInterface);
    }

}
