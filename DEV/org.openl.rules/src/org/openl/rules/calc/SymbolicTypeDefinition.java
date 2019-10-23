package org.openl.rules.calc;

import org.openl.syntax.impl.IdentifierNode;

public class SymbolicTypeDefinition {

    private IdentifierNode name;
    private IdentifierNode type;
    private boolean withAsterisk;
    private boolean withTilde;

    public SymbolicTypeDefinition(IdentifierNode name, IdentifierNode type, boolean withAsterisk, boolean withTilde) {
        this.name = name;
        this.type = type;
        this.withAsterisk = withAsterisk;
        this.withTilde = withTilde;
        if (withAsterisk && withTilde) {
            throw new IllegalArgumentException("Only ~ or * can be used at the same time.");
        }
    }

    public IdentifierNode getName() {
        return name;
    }

    public IdentifierNode getType() {
        return type;
    }

    public boolean isWithAsterisk() {
        return withAsterisk;
    }

    public boolean isWithTilde() {
        return withTilde;
    }
}
