package org.openl.rules.lang.xls.binding;

import java.util.Objects;

import org.openl.util.text.ILocation;

public class ExpressionIdentifier {
    private final String identifier;
    private final ILocation location;

    public ExpressionIdentifier(String identifier, ILocation location) {
        this.identifier = Objects.requireNonNull(identifier, "identifier cannot be null");
        this.location = Objects.requireNonNull(location, "location cannot be null");
    }

    public String getIdentifier() {
        return identifier;
    }

    public ILocation getLocation() {
        return location;
    }
}
