package org.openl.rules.runtime;

import org.openl.runtime.EngineFactory;
import org.openl.runtime.EngineFactoryDefinition;
import org.openl.runtime.IEngineRequestHandler;
import org.openl.runtime.ParameterizedEngineFactory;

public class ParameterizedRuleEngineFactory<T, R> extends ParameterizedEngineFactory<T, R> {

    public ParameterizedRuleEngineFactory(IEngineRequestHandler<R> handler, Class<T> engineInterface) {
        super(RuleEngineFactory.RULE_OPENL_NAME, handler, engineInterface);
    }

    @Override
    protected EngineFactory<T> makeFactory(EngineFactoryDefinition factoryDef) {
        return new RuleEngineFactory<T>(factoryDef, engineInterface);
    }

}
