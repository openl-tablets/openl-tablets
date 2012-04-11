package org.openl.rules.runtime;

import java.io.File;
import java.net.URL;

import org.openl.runtime.EngineFactory;
import org.openl.runtime.EngineFactoryDefinition;

/**
 * 
 * Class RuleEngineFactory creates wrappers around OpenL
 * classes with openl name "org.openl.xls".
 * 
 */
public class RuleEngineFactory<T> extends EngineFactory<T> {

    static public final String RULE_OPENL_NAME = "org.openl.xls";
    
    /**
     *
     * @param factoryDef Engine factory definition {@link EngineFactoryDefinition}.
     * @param engineInterface User interface of rule.
     */
    public RuleEngineFactory(EngineFactoryDefinition factoryDef, Class<T> engineInterface) {
        super(RULE_OPENL_NAME, factoryDef, engineInterface);
    }
    
    /**
     *
     * @param file Rule file
     * @param engineInterface User interface of rule
     */
    public RuleEngineFactory(File file, Class<T> engineInterface) {
        super(RULE_OPENL_NAME, file, engineInterface);
    }
    
    /**
     *
     * @param sourceFile A path name of rule file string
     * @param engineInterface User interface of a rule
     */
    public RuleEngineFactory(String sourceFile, Class<T> engineInterface) {
        super(RULE_OPENL_NAME, sourceFile, engineInterface);
    }
    
    /**
     *
     * @param userHome Current path of Openl userHome
     * @param sourceFile A pathname of rule file string
     * @param engineInterface User interface of a rule
     */
    public RuleEngineFactory(String userHome, String sourceFile, Class<T> engineInterface) {
        super(RULE_OPENL_NAME, userHome, sourceFile, engineInterface);
    }
    
    /**
     *
     * @param url Url to rule file
     * @param engineInterface User interface of a rule
     */
    public RuleEngineFactory(URL url, Class<T> engineInterface) {
        super(RULE_OPENL_NAME, url, engineInterface);
    }

}
