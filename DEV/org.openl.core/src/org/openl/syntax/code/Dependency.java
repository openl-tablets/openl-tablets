package org.openl.syntax.code;

import java.util.Objects;

import org.openl.dependency.DependencyType;
import org.openl.syntax.impl.IdentifierNode;

public class Dependency implements IDependency {

    private final DependencyType type;

    private final IdentifierNode node;

    public Dependency(DependencyType dependencyType, IdentifierNode node) {
        this.node = Objects.requireNonNull(node, "node cannot be null");
        this.type = Objects.requireNonNull(dependencyType, "dependencyType cannot be null");
    }

    @Override
    public IdentifierNode getNode() {
        return node;
    }

    @Override
    public String toString() {
        return node.getIdentifier();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Dependency that = (Dependency) o;
        return Objects.equals(node.getIdentifier(), that.node.getIdentifier());
    }

    @Override
    public int hashCode() {
        return Objects.hash(node.getIdentifier());
    }

    public DependencyType getType() {
        return type;
    }
}
