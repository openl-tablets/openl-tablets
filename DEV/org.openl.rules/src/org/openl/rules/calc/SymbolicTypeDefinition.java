package org.openl.rules.calc;

import org.openl.syntax.impl.IdentifierNode;

public class SymbolicTypeDefinition {

    private IdentifierNode name;
    private IdentifierNode type;
    private boolean asterisk;
    private boolean tilde;

    public SymbolicTypeDefinition(IdentifierNode name, IdentifierNode type, boolean asterisk, boolean tilde) {
        this.name = name;
        this.type = type;
        this.asterisk = asterisk;
        this.tilde = tilde;
        if (asterisk && tilde) {
            throw new IllegalArgumentException("Only ~ or * can be used at the same time.");
        }
    }

    public IdentifierNode getName() {
        return name;
    }

    public IdentifierNode getType() {
        return type;
    }

    public boolean isAsteriskPresented() {
        return asterisk;
    }

    public boolean isTildePresented() {
        return tilde;
    }
}
