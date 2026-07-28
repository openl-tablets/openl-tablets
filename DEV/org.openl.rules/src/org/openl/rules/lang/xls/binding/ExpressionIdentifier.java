package org.openl.rules.lang.xls.binding;

import java.util.Objects;

import lombok.Getter;

import org.openl.util.text.ILocation;

public class ExpressionIdentifier {
    @Getter
    private final String identifier;
    @Getter
    private final ILocation location;

    public ExpressionIdentifier(String identifier, ILocation location) {
        this.identifier = Objects.requireNonNull(identifier, "identifier cannot be null");
        this.location = Objects.requireNonNull(location, "location cannot be null");
    }
}
