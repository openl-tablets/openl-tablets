package org.openl.dependency;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.openl.binding.IBindingContext;
import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.exception.AmbiguousTypeException;
import org.openl.binding.impl.BindingContextDelegator;
import org.openl.message.OpenLMessagesUtils;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;

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
    public IOpenClass findType(String namespace, String typeName) throws AmbiguousTypeException {
        IOpenClass type = super.findType(namespace, typeName);
        if (type != null) {
            return type;
        }
        if (typeName.contains(".") && !typeName.endsWith(".")) {
            String dependencyName = typeName.substring(0, typeName.indexOf("."));
            ResolvedDependency resolvedDependency = resolveDependency(dependencyName);
            if (resolvedDependency == null) {
                return null;
            }
            try {
                CompiledDependency compiledDependency = dependencyManager.loadDependency(resolvedDependency);
                if (!loadedDependencies.contains(dependencyName)) {
                    loadedDependencies.add(dependencyName);
                    addMessages(compiledDependency.getCompiledOpenClass().getMessages());
                }
                String tName = typeName.substring(typeName.indexOf(".") + 1);
                IOpenClass t = compiledDependency.getCompiledOpenClass().getOpenClassWithErrors().findType(tName);
                if (t != null) {
                    return t;
                }
                try {
                    t = JavaOpenClass.getOpenClass(compiledDependency.getClassLoader().loadClass(tName));
                    IOpenClass x = compiledDependency.getCompiledOpenClass()
                        .getOpenClassWithErrors()
                        .findType(t.getInstanceClass().getSimpleName());
                    if (x != null && x.getInstanceClass() == t.getInstanceClass()) {
                        return x;
                    }
                    return t;
                } catch (ClassNotFoundException e) {
                    return null;
                }
            } catch (Exception e) {
                if (!loadedDependencies.contains(dependencyName)) {
                    addMessages(OpenLMessagesUtils.newErrorMessages(e));
                    loadedDependencies.add(dependencyName);
                }
            }
        }
        return null;
    }

    @Override
    public IOpenField findVar(String namespace, String name, boolean strictMatch) throws AmbiguousFieldException {
        IOpenField var = super.findVar(namespace, name, strictMatch);
        if (var != null) {
            return var;
        }
        ResolvedDependency resolvedDependency = resolveDependency(name);
        if (resolvedDependency == null) {
            return null;
        }
        try {
            CompiledDependency compiledDependency = dependencyManager.loadDependency(resolvedDependency);
            if (!loadedDependencies.contains(name)) {
                loadedDependencies.add(name);
                addMessages(compiledDependency.getCompiledOpenClass().getMessages());
            }
            return new DependencyVar(compiledDependency.getDependency().getNode().getIdentifier(),
                new DependencyOpenClass(compiledDependency.getCompiledOpenClass().getOpenClassWithErrors()),
                compiledDependency.getDependencyType());
        } catch (Exception e) {
            if (!loadedDependencies.contains(name)) {
                addMessages(OpenLMessagesUtils.newErrorMessages(e));
                loadedDependencies.add(name);
            }
        }
        return null;
    }

    private ResolvedDependency resolveDependency(String dependencyName) {
        try {
            Collection<ResolvedDependency> resolvedDependencies = dependencyManager.resolveDependency(
                new Dependency(DependencyType.PROJECT, new IdentifierNode(null, null, dependencyName, null)),
                false);
            return resolvedDependencies.isEmpty() ? null : resolvedDependencies.iterator().next();
        } catch (AmbiguousDependencyException e) {
            throw new AmbiguousFieldException(dependencyName, null);
        } catch (DependencyNotFoundException e) {
            try {
                Collection<ResolvedDependency> resolvedDependencies = dependencyManager.resolveDependency(
                    new Dependency(DependencyType.MODULE, new IdentifierNode(null, null, dependencyName, null)),
                    false);
                return resolvedDependencies.isEmpty() ? null : resolvedDependencies.iterator().next();
            } catch (AmbiguousDependencyException e1) {
                throw new AmbiguousFieldException(dependencyName, null);
            } catch (DependencyNotFoundException e1) {
                return null;
            }
        }
    }
}
