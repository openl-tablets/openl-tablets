package org.openl.rules.ruleservice.publish.lazy;

import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.rules.ruleservice.core.RuleServiceDependencyManager;
import org.openl.rules.ruleservice.core.RuleServiceOpenLCompilationException;
import org.openl.types.IOpenField;

/**
 * Lazy field that will compile module declaring it and will get real field to do operations with it.
 *
 * @author Marat Kamalov
 */
public abstract class LazyField extends LazyMember<IOpenField> {

    private final String fieldName;

    LazyField(String fieldName,
            RuleServiceDependencyManager dependencyManager,
            ClassLoader classLoader,
            Map<String, Object> externalParameters) {
        super(dependencyManager, classLoader, externalParameters);
        this.fieldName = fieldName;
    }

    protected IOpenField initMember() {
        try {
            CompiledOpenClass compiledOpenClass = getCompiledOpenClassWithThrowErrorExceptionsIfAny();
            return compiledOpenClass.getOpenClass().getField(fieldName);
        } catch (Exception e) {
            throw new RuleServiceOpenLCompilationException("Failed to load a lazy field.", e);
        }
    }
}
