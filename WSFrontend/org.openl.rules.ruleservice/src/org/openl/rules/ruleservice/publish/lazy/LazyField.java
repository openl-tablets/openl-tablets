package org.openl.rules.ruleservice.publish.lazy;

import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.rules.lang.xls.prebind.XlsLazyModuleOpenClass;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.DeploymentDescription;
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

    private LazyField(String fieldName,
            RuleServiceDependencyManager dependencyManager,
            ClassLoader classLoader,
            Map<String, Object> externalParameters) {
        super(dependencyManager, classLoader, externalParameters);
        this.fieldName = fieldName;
    }

    static LazyField createLazyField(final IOpenField prebindedMethod,
                                     final RuleServiceDependencyManager dependencyManager,
                                     final DeploymentDescription deployment,
                                     final Module module,
                                     final ClassLoader classLoader,
                                     final Map<String, Object> externalParameters) {
        final LazyField lazyField = new LazyField(prebindedMethod.getName(),
            dependencyManager,
            classLoader,
            externalParameters) {
            @Override
            public DeploymentDescription getDeployment() {
                return deployment;
            }

            @Override
            public Module getModule() {
                return module;
            }

            @Override
            public XlsLazyModuleOpenClass getXlsLazyModuleOpenClass() {
                return (XlsLazyModuleOpenClass) prebindedMethod.getDeclaringClass();
            }
        };
        CompiledOpenClassCache.getInstance()
            .registerEvent(deployment, module.getName(), new LazyMemberEvent(lazyField));
        return lazyField;
    }

    @Override
    public IOpenField getMember() {
        IOpenField cachedMember = getCachedMember();
        if (cachedMember != null) {
            return cachedMember;
        }
        try {
            CompiledOpenClass compiledOpenClass = getCompiledOpenClassWithThrowErrorExceptionsIfAny();
            IOpenField openField = compiledOpenClass.getOpenClass().getField(fieldName);
            setCachedMember(openField);
            return openField;
        } catch (Exception e) {
            throw new RuleServiceOpenLCompilationException("Failed to load a lazy field.", e);
        }
    }
}
