package org.openl.rules.calc;

import lombok.Getter;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;

public class SymbolicTypeDefinition {

    @Getter
    private final IdentifierNode name;
    @Getter
    private final IdentifierNode type;
    private final boolean asterisk;
    private final boolean tilde;
    @Getter
    private final IOpenSourceCodeModule source;

    public SymbolicTypeDefinition(IdentifierNode name,
                                  IdentifierNode type,
                                  boolean asterisk,
                                  boolean tilde,
                                  IOpenSourceCodeModule source) {
        this.name = name;
        this.type = type;
        this.asterisk = asterisk;
        this.tilde = tilde;
        if (asterisk && tilde) {
            throw new IllegalArgumentException("Only ~ or * can be used at the same time.");
        }
        this.source = source;
    }

    public boolean isAsteriskPresented() {
        return asterisk;
    }

    public boolean isTildePresented() {
        return tilde;
    }
}
