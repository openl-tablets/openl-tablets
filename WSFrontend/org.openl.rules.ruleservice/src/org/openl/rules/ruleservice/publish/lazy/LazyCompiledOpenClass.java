package org.openl.rules.ruleservice.publish.lazy;

import java.util.Collection;

import org.openl.CompiledOpenClass;
import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.message.OpenLMessage;
import org.openl.rules.ruleservice.core.LazyRuleServiceDependencyLoader;
import org.openl.rules.ruleservice.core.RuleServiceDeploymentRelatedDependencyManager;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;

public class LazyCompiledOpenClass extends CompiledOpenClass {

    private LazyRuleServiceDependencyLoader lazyRuleServiceDependencyLoader;
    private RuleServiceDeploymentRelatedDependencyManager dependencyManager;
    private IDependency dependency;

    public LazyCompiledOpenClass(RuleServiceDeploymentRelatedDependencyManager dependencyManager,
            LazyRuleServiceDependencyLoader lazyRuleServiceDependencyLoader,
            IDependency dependency) {
        super(null, null, null, null);
        if (lazyRuleServiceDependencyLoader == null) {
            throw new IllegalArgumentException("lazyRuleServiceDependencyLoader must not be null!");
        }
        if (dependency == null) {
            throw new IllegalArgumentException("dependency must not be null!");
        }
        if (dependencyManager == null) {
            throw new IllegalArgumentException("dependencyManager must not be null!");
        }
        this.dependencyManager = dependencyManager;
        this.lazyRuleServiceDependencyLoader = lazyRuleServiceDependencyLoader;
        this.dependency = dependency;
    }

    protected CompiledOpenClass getCompiledOpenClass() {
        try {
            return lazyRuleServiceDependencyLoader.compile(dependency.getNode().getIdentifier(), dependencyManager);
        } catch (OpenLCompilationException e) {
            throw new OpenlNotCheckedException("It must not happen! Compilation validated before!");
        }

    }

    @Override
    public SyntaxNodeException[] getBindingErrors() {
        return getCompiledOpenClass().getBindingErrors();
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
    public SyntaxNodeException[] getParsingErrors() {
        return getCompiledOpenClass().getParsingErrors();
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
