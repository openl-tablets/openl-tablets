package org.openl.binding.impl.cast;

import java.util.Objects;

public class AliasToAliasOpenCast implements IOpenCast, INestedCastOpenCast {

    private final IOpenCast openCast;
    private final boolean implicit;

    public AliasToAliasOpenCast(IOpenCast openCast) {
        this.openCast = Objects.requireNonNull(openCast, "openCast cannot be null");
        this.implicit = openCast.isImplicit();
    }

    public AliasToAliasOpenCast(IOpenCast openCast, boolean implicit) {
        this.openCast = Objects.requireNonNull(openCast, "openCast cannot be null");
        this.implicit = implicit;
    }

    @Override
    public IOpenCast getNestedOpenCast() {
        return openCast;
    }

    @Override
    public boolean hasNestedOpenCast() {
        return openCast != null;
    }

    @Override
    public int getDistance() {
        return openCast.getDistance();
    }

    @Override
    public Object convert(Object from) {
        return openCast.convert(from);
    }

    @Override
    public boolean isImplicit() {
        return implicit;
    }
}
