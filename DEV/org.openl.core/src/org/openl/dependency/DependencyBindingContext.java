package org.openl.dependency;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.openl.binding.IBindingContext;
import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.impl.BindingContextDelegator;
import org.openl.message.OpenLMessagesUtils;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenField;

public class DependencyBindingContext extends BindingContextDelegator {

    private final IDependencyManager dependencyManager;

    private final Set<String> loadedDependencies = new HashSet<>();

    public DependencyBindingContext(IBindingContext delegate, IDependencyManager dependencyManager) {
        super(delegate);
        this.dependencyManager = Objects.requireNonNull(dependencyManager, "dependencyManager cannot be null");
    }

    public IDependencyManager getDependencyManager() {
        return dependencyManager;
    }

    @Override
    public IOpenField findVar(String namespace, String name, boolean strictMatch) throws AmbiguousFieldException {
        IOpenField var = super.findVar(namespace, name, strictMatch);
        if (var != null) {
            return var;
        }
        Collection<ResolvedDependency> dependencies;
        try {
            dependencies = dependencyManager
                .resolveDependency(new Dependency(new IdentifierNode(null, null, name, null)));
        } catch (AmbiguousDependencyException e) {
            throw new AmbiguousFieldException(name, null);
        } catch (DependencyNotFoundException e) {
            return null;
        }
        try {
            CompiledDependency compiledDependency = dependencyManager.loadDependency(dependencies.iterator().next());
            if (!loadedDependencies.contains(name)) {
                loadedDependencies.add(name);
                addMessages(compiledDependency.getCompiledOpenClass().getMessages());
            }
            return new ModuleVar(name,
                new DependencyOpenClass(compiledDependency.getCompiledOpenClass().getOpenClassWithErrors()));
        } catch (Exception e) {
            if (!loadedDependencies.contains(name)) {
                addMessages(OpenLMessagesUtils.newErrorMessages(e));
                loadedDependencies.add(name);
            }
        }
        return null;
    }
}
