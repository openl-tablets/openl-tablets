package org.openl.syntax.code;

import org.openl.syntax.impl.IdentifierNode;

public class Dependency implements IDependency {

    private DependencyType type;
    private IdentifierNode node;

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

}
