package org.openl.rules.calc;

import org.openl.syntax.impl.IdentifierNode;

public class SymbolicTypeDefinition {

    private IdentifierNode name;
    private IdentifierNode type;

    public SymbolicTypeDefinition(IdentifierNode name, IdentifierNode type) {
        this.name = name;
        this.type = type;
    }

    public IdentifierNode getName() {
        return name;
    }

    public IdentifierNode getType() {
        return type;
    }

}
