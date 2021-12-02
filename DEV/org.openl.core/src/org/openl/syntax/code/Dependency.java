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

        if (type != that.type)
            return false;
        return node.getIdentifier().equals(that.node.getIdentifier());
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + node.getIdentifier().hashCode();
        return result;
    }

    public DependencyType getType() {
        return type;
    }
}
