package org.openl.syntax.code;

import org.openl.syntax.impl.IdentifierNode;

public class Dependency implements IDependency {

    private final DependencyType type;
    private final IdentifierNode node;

    public Dependency(DependencyType type, IdentifierNode node) {
        this.type = type;
        this.node = node;
    }

    @Override
    public DependencyType getType() {
        return type;
    }

    @Override
    public IdentifierNode getNode() {
        return node;
    }

    @Override
    public String toString() {
        return "Dependency{" + "type=" + type + ", node=" + node + '}';
    }
}
