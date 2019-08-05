package org.openl.rules.calc;

import org.openl.syntax.impl.IdentifierNode;

public class SymbolicTypeDefinition {

    private IdentifierNode name;
    private IdentifierNode type;
    private boolean markedWithStar;

    public SymbolicTypeDefinition(IdentifierNode name, IdentifierNode type, boolean markedWithStar) {
        this.name = name;
        this.type = type;
        this.markedWithStar = markedWithStar;
    }

    public IdentifierNode getName() {
        return name;
    }

    public IdentifierNode getType() {
        return type;
    }

    public boolean isMarkedWithStar() {
        return markedWithStar;
    }

}
