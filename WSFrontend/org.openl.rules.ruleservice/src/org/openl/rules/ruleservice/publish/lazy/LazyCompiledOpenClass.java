package org.openl.rules.ruleservice.publish.lazy;

import java.util.Collection;
import java.util.Objects;

import org.openl.CompiledOpenClass;
import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.message.OpenLMessage;
import org.openl.rules.ruleservice.core.RuleServiceDependencyManager;
import org.openl.syntax.code.IDependency;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;

public class LazyCompiledOpenClass extends CompiledOpenClass {

    private LazyRuleServiceDependencyLoader lazyRuleServiceDependencyLoader;
    private RuleServiceDependencyManager dependencyManager;
    private IDependency dependency;

    public LazyCompiledOpenClass(RuleServiceDependencyManager dependencyManager,
            LazyRuleServiceDependencyLoader lazyRuleServiceDependencyLoader,
            IDependency dependency) {
        super(NullOpenClass.the, null);
        this.dependencyManager = Objects.requireNonNull(dependencyManager, "dependencyManager cannot be null");
        this.lazyRuleServiceDependencyLoader = Objects.requireNonNull(lazyRuleServiceDependencyLoader,
            "lazyRuleServiceDependencyLoader cannot be null");
        this.dependency = Objects.requireNonNull(dependency, "dependency cannot be null");
    }

    protected CompiledOpenClass getCompiledOpenClass() {
        try {
            return lazyRuleServiceDependencyLoader.compile(dependency.getNode().getIdentifier(), dependencyManager);
        } catch (OpenLCompilationException e) {
            throw new OpenlNotCheckedException("It must not happen! Compilation validated before.");
        }

    }


    @Override
    public IOpenClass getOpenClass() {
        return getCompiledOpenClass().getOpenClass();
    }

    @Override
    public IOpenClass getOpenClassWithErrors() {
        return getCompiledOpenClass().getOpenClassWithErrors();
    }

    @Override
    public int hashCode() {
        return getCompiledOpenClass().hashCode();
    }

    @Override
    public boolean hasErrors() {
        return getCompiledOpenClass().hasErrors();
    }

    @Override
    public void throwErrorExceptionsIfAny() {
        getCompiledOpenClass().throwErrorExceptionsIfAny();
    }

    @Override
    public Collection<OpenLMessage> getMessages() {
        return getCompiledOpenClass().getMessages();
    }

    @Override
    public Collection<IOpenClass> getTypes() {
        return getCompiledOpenClass().getTypes();
    }

    @Override
    public ClassLoader getClassLoader() {
        return getCompiledOpenClass().getClassLoader();
    }

    @Override
    public boolean equals(Object obj) {
        return getCompiledOpenClass().equals(obj);
    }

    @Override
    public String toString() {
        return getCompiledOpenClass().toString();
    }
}
