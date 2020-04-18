package org.openl.rules.ruleservice.publish.lazy;

import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.lang.xls.prebind.XlsLazyModuleOpenClass;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.rules.ruleservice.core.RuleServiceOpenLCompilationException;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

/**
 * Lazy field that will compile module declaring it and will get real field to do operations with it.
 *
 * @author PUdalau, Marat Kamalov
 */
public abstract class LazyField extends LazyMember<IOpenField> implements IOpenField {
    private final String fieldName;

    private LazyField(String fieldName,
            IOpenField original,
            IDependencyManager dependencyManager,
            ClassLoader classLoader,
            boolean executionMode,
            Map<String, Object> externalParameters) {
        super(dependencyManager, executionMode, classLoader, original, externalParameters);
        this.fieldName = fieldName;
    }

    public static LazyField getLazyField(final XlsLazyModuleOpenClass xlsLazyModuleOpenClass,
            final DeploymentDescription deployment,
            final Module module,
            IOpenField original,
            IDependencyManager dependencyManager,
            ClassLoader classLoader,
            boolean executionMode,
            Map<String, Object> externalParameters) {
        LazyField lazyField = new LazyField(original
            .getName(), original, dependencyManager, classLoader, executionMode, externalParameters) {
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
                return xlsLazyModuleOpenClass;
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
            throw new RuleServiceOpenLCompilationException("Failed to load lazy field.", e);
        }
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        return getMember().get(target, env);
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        getMember().set(target, value, env);
    }

    @Override
    public boolean isConst() {
        return getOriginal().isConst();
    }

    @Override
    public boolean isReadable() {
        return getOriginal().isReadable();
    }

    @Override
    public boolean isWritable() {
        return getOriginal().isWritable();
    }

}
