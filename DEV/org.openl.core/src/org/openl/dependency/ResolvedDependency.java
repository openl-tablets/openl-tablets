package org.openl.dependency;

import org.openl.syntax.code.Dependency;
import org.openl.syntax.impl.IdentifierNode;

public final class ResolvedDependency extends Dependency {
    public ResolvedDependency(DependencyType dependencyType, IdentifierNode node) {
        super(dependencyType, node);
    }
}
