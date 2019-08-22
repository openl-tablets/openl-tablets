package org.openl.rules.calc;

import org.openl.syntax.impl.IdentifierNode;

public class SymbolicTypeDefinition {

    private IdentifierNode name;
    private IdentifierNode type;
    private boolean markedWithAsterisk;

    public SymbolicTypeDefinition(IdentifierNode name, IdentifierNode type, boolean markedWithAsterisk) {
        this.name = name;
        this.type = type;
        this.markedWithAsterisk = markedWithAsterisk;
    }

    public IdentifierNode getName() {
        return name;
    }

    public IdentifierNode getType() {
        return type;
    }

    public boolean isMarkedWithAsterisk() {
        return markedWithAsterisk;
    }

}
